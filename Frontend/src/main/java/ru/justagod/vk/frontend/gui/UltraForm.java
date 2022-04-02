package ru.justagod.vk.frontend.gui;

import org.jetbrains.annotations.Nullable;
import ru.justagod.vk.data.*;
import ru.justagod.vk.frontend.Main;
import ru.justagod.vk.frontend.http.HttpClient;
import ru.justagod.vk.frontend.http.ServerResponse;
import ru.justagod.vk.frontend.poll.LongPollClientConnection;
import ru.justagod.vk.network.Endpoint;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.*;
import java.util.List;

public class UltraForm extends JFrame {
    private final HttpClient client;
    private JButton signUpButton;
    private JPanel root;
    private JTextField username;
    private JTextField password;
    private JLabel errorLabel;
    private JButton signInButton;
    private JButton usersUpdate;
    private JList<UserName> usersList;
    private JButton friendsUpdate;
    private JPanel authorizedView;
    private JButton addFriendButton;
    private JList<UserName> friendsList;
    private JButton removeFriend;
    private JLabel challengeLabel;
    private JTextField challengeField;
    private JButton sendChallenge;
    private JPanel challengePane;
    private JTextField chatMessageField;
    private JButton sendButton;
    private JButton loadMoreButton;
    private JButton refreshButton;
    private JLabel chatLabel;
    private JLabel longPollStatus;
    private JButton chatButton;
    private JList<String> messagesList;

    private UserName currentChat;


    private final Map<User, List<Message>> messagesBuffer = new HashMap<>();

    @Nullable
    private SessionResponse session = null;

    public UltraForm(HttpClient client) throws HeadlessException {
        this.client = client;
        setTitle("Sign Up");
        setContentPane(root);
        pack();

        authorizedView.setVisible(false);

        signUpButton.addActionListener(this::onSignUp);
        signInButton.addActionListener(this::onSignIn);

        usersUpdate.addActionListener(a -> requestUsers());
        friendsUpdate.addActionListener(a -> requestFriends());

        addFriendButton.addActionListener(this::addFriend);
        removeFriend.addActionListener(this::removeFriend);

        sendChallenge.addActionListener(e -> sendChallenge());

        chatButton.addActionListener(this::openChat);
        sendButton.addActionListener(this::sendMessage);

        Thread thread = new Thread(this::runLongPoll);
        thread.setDaemon(true);
        thread.start();
    }

    private void setLongPollStatus(String status) {
        SwingUtilities.invokeLater(() -> longPollStatus.setText("Long poll status: " + status));
    }

    private <T> boolean checkLongPollError(BackendResponse<T> response) {
        if (!response.isSuccess()) {
            setLongPollStatus(response.error().toString());
            return true;
        }
        return false;
    }

    private void runLongPoll() {
        while (true) {
            setLongPollStatus("Disconnected");
            @Nullable SessionResponse session = this.session;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                return;
            }
            if (session == null) continue;
            try {
                LongPollClientConnection connection = LongPollClientConnection.connect(
                        new InetSocketAddress(Main.pollAddress(), Main.pollPort())
                );

                var response = connection.read(String.class);
                if (checkLongPollError(response)) continue;
                connection.write(response.payload());
                response = connection.read(String.class);
                if (checkLongPollError(response)) continue;
                connection.write(session.session().value());
                response = connection.read(String.class);
                if (checkLongPollError(response)) continue;

                setLongPollStatus("Polling");
                while (true) {
                    connection.write(null);
                    var messagesResponse = connection.read(Messages.class);
                    if (checkLongPollError(messagesResponse)) break;
                    var messages = messagesResponse.payload().messages();
                    if (!messages.isEmpty()) {
                        SwingUtilities.invokeLater(() -> {
                            messages.forEach(this::addMessage);
                            updateChat();
                        });
                    }

                    Thread.sleep(50);
                }


            } catch (Exception e) {
                e.printStackTrace();
                setLongPollStatus(e.toString());
            }
        }
    }

    private void sendMessage(ActionEvent actionEvent) {
        if (session == null) return;
        if (currentChat == null) return;

        var response = failable(() ->
                client.sendRequest(Endpoint.SEND_MESSAGE_REQUEST_ENDPOINT, new SendMessageRequest(
                                session.session().value(),
                                chatMessageField.getText(),
                                currentChat.user()
                        )
                )
        );
        checkError(response);

    }

    private void openChat(ActionEvent actionEvent) {
        UserName user = usersList.getSelectedValue();
        if (user == null) return;
        currentChat = user;

        requestMessages();
    }

    private void requestMessages() {
        if (session == null) return;
        if (currentChat == null) return;

        var response = failable(() ->
                client.sendRequest(Endpoint.GET_MESSAGES_REQUEST_ENDPOINT, new MessagesRequest(
                        session.session().value(),
                        currentChat.user(),
                        Instant.now())
                )
        );
        if (checkError(response)) return;

        for (Message message : response.response().payload().messages()) {
            addMessage(message);
        }

        updateChat();
    }

    private void updateChat() {
        messagesList.setListData(new String[0]);
        if (currentChat == null) {
            chatLabel.setText("Chat");
            return;
        }
        chatLabel.setText("Chat with " + currentChat);
        List<Message> messages = messagesBuffer.get(currentChat.user());
        if (messages == null) return;


        String[] data = messages.stream()
                .sorted(Comparator.comparing(Message::sentAt).reversed())
                .map((m) -> m.sender() + ": " + m.content())
                .toArray(String[]::new);

        messagesList.setListData(data);
    }

    private void addTo(List<Message> list, Message message) {
        list.add(message);
        Set<UUID> set = new HashSet<>();
        var iter = list.iterator();

        while (iter.hasNext()) {
            Message msg = iter.next();

            if (set.contains(msg.id())) iter.remove();
            else set.add(msg.id());
        }

        list.sort(Comparator.comparing(Message::sentAt).reversed());
    }

    private void addMessage(Message message) {
        if (session == null) return;
        if (message.receiver() != session.user()) {
            List<Message> l = messagesBuffer.computeIfAbsent(message.receiver(), a -> new ArrayList<>());
            addTo(l, message);
        }
        if (message.sender() != session.user()) {
            List<Message> r = messagesBuffer.computeIfAbsent(message.sender(), a -> new ArrayList<>());
            addTo(r, message);
        }
    }

    private void removeFriend(ActionEvent actionEvent) {
        UserName user = friendsList.getSelectedValue();
        if (user == null) return;
        SessionResponse session = this.session;
        if (session == null) return;

        ServerResponse<Void> result = failable(() ->
                client.sendRequest(Endpoint.REMOVE_FRIEND_REQUEST_ENDPOINT,
                        new AuthorizedUserRequest(
                                session.session().value(),
                                user.user()
                        )
                )
        );
        if (checkError(result)) return;
        requestFriends();
    }

    private void addFriend(ActionEvent actionEvent) {
        UserName user = usersList.getSelectedValue();
        if (user == null) return;
        SessionResponse session = this.session;
        if (session == null) return;

        ServerResponse<Void> result = failable(() ->
                client.sendRequest(Endpoint.ADD_FRIEND_REQUEST_ENDPOINT,
                        new AuthorizedUserRequest(
                                session.session().value(),
                                user.user()
                        )
                )
        );
        if (checkError(result)) return;
        requestFriends();
    }

    private void clearError() {
        errorLabel.setText("");
    }

    private void setError(String error) {
        errorLabel.setText(error);
    }

    @Nullable
    private <T> T failable(Failable<T> block) {
        try {
            setError(null);
            return block.run();
        } catch (Exception e) {
            e.printStackTrace();
            setError(e.toString());
            return null;
        }
    }

    private void onSignIn(ActionEvent actionEvent) {
        requestSession(Endpoint.SIGN_IN_REQUEST_ENDPOINT);
    }

    private void onSignUp(ActionEvent actionEvent) {
        requestSession(Endpoint.SIGN_UP_REQUEST_ENDPOINT);
    }

    private void requestSession(Endpoint<UserPasswordRequest, SessionResponse> endpoint) {
        clearError();
        ServerResponse<SessionResponse> response = failable(() ->
                client.sendRequest(endpoint,
                        new UserPasswordRequest(
                                username.getText(),
                                password.getText()
                        )
                )
        );
        if (checkError(response)) return;

        messagesBuffer.clear();
        currentChat = null;
        updateChat();
        if (!response.success()) {
            session = null;
        } else {
            session = response.response().payload();
        }
        switchAuthorizedView();
    }

    private void sendChallenge() {
        int answer;
        try {
            answer = Integer.parseInt(challengeField.getText());
        } catch (Exception e) {
            setError(e.toString());
            return;
        }
        ServerResponse<Void> response =
                failable(() -> client.sendRequest(Endpoint.SOLVE_CHALLENGE_REQUEST_ENDPOINT, answer));
        if (response == null) return;
        if (checkError(response) && response.response().error().kind() != BackendError.CHALLENGE_IS_NOT_REQUIRED)
            return;

        challengePane.setVisible(false);
    }

    private void setUpChallengeRequest(String challenge) {
        challengePane.setVisible(true);
        challengeLabel.setText(challenge);
    }

    private boolean checkError(ServerResponse<?> response) {
        if (response == null) return true;
        if (!response.success()) {
            if (response.response().error().kind() == BackendError.FORBIDDEN) {
                session = null;
                switchAuthorizedView();
            } else if (response.response().error().kind() == BackendError.CHALLENGE_REQUIRED) {
                setUpChallengeRequest(response.response().error().payload());
            } else {
                setError(response.response().error().toString());
            }
            return true;
        }
        return false;
    }

    private void requestFriends() {
        friendsList.removeAll();
        if (session == null) return;
        ServerResponse<UsersListResponse> response = failable(() -> client
                .sendRequest(
                        Endpoint.FRIENDS_REQUEST_ENDPOINT,
                        new AuthorizedRequest(session.session().value())
                )
        );
        if (checkError(response)) return;

        UserName[] data = response.response().payload().users().toArray(UserName[]::new);
        friendsList.setListData(data);


        friendsList.revalidate();
        friendsList.repaint();
    }

    private void requestUsers() {
        usersList.removeAll();
        if (session == null) return;
        ServerResponse<UsersListResponse> response = failable(() -> client
                .sendRequest(
                        Endpoint.USERS_REQUEST_ENDPOINT,
                        new AuthorizedRequest(session.session().value())
                )
        );
        if (response == null) return;
        if (checkError(response)) return;

        UserName[] data = response.response().payload().users().toArray(UserName[]::new);
        usersList.setListData(data);


        usersList.revalidate();
        usersList.repaint();
    }

    private void switchAuthorizedView() {
        if (session != null) {
            authorizedView.setVisible(true);
            requestUsers();
            requestFriends();
        } else {
            authorizedView.setVisible(false);
        }
    }


    private interface Failable<T> {
        T run() throws Exception;
    }
}
