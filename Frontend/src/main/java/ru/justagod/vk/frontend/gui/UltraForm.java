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

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        root = new JPanel();
        root.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        errorLabel = new JLabel();
        errorLabel.setText("Error goes here");
        root.add(errorLabel, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(5, 1, new Insets(0, 0, 0, 0), -1, -1));
        root.add(panel1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, 1, 1, null, null, null, 0, false));
        signUpButton = new JButton();
        signUpButton.setText("Sign Up");
        panel1.add(signUpButton, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Sign Up");
        panel2.add(label1, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer1 = new com.intellij.uiDesigner.core.Spacer();
        panel2.add(spacer1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer2 = new com.intellij.uiDesigner.core.Spacer();
        panel2.add(spacer2, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel3, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Username");
        panel3.add(label2, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        username = new JTextField();
        panel3.add(username, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel4, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Password");
        panel4.add(label3, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        password = new JTextField();
        panel4.add(password, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        signInButton = new JButton();
        signInButton.setText("Sign In");
        panel1.add(signInButton, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        authorizedView = new JPanel();
        authorizedView.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        root.add(authorizedView, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JSplitPane splitPane1 = new JSplitPane();
        authorizedView.add(splitPane1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(5, 2, new Insets(0, 0, 0, 0), -1, -1));
        splitPane1.setLeftComponent(panel5);
        usersUpdate = new JButton();
        usersUpdate.setText("Update");
        panel5.add(usersUpdate, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Users");
        panel5.add(label4, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        usersList = new JList();
        usersList.setSelectionMode(0);
        panel5.add(usersList, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        addFriendButton = new JButton();
        addFriendButton.setText("Add friend");
        panel5.add(addFriendButton, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        chatButton = new JButton();
        chatButton.setText("Chat");
        panel5.add(chatButton, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSplitPane splitPane2 = new JSplitPane();
        splitPane1.setRightComponent(splitPane2);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
        splitPane2.setLeftComponent(panel6);
        friendsUpdate = new JButton();
        friendsUpdate.setText("Update");
        panel6.add(friendsUpdate, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Friends");
        panel6.add(label5, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        friendsList = new JList();
        panel6.add(friendsList, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        removeFriend = new JButton();
        removeFriend.setText("Remove firend");
        panel6.add(removeFriend, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(6, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane2.setRightComponent(panel7);
        chatLabel = new JLabel();
        chatLabel.setText("Chat");
        panel7.add(chatLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel7.add(panel8, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        chatMessageField = new JTextField();
        panel8.add(chatMessageField, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        sendButton = new JButton();
        sendButton.setText("Send");
        panel8.add(sendButton, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        loadMoreButton = new JButton();
        loadMoreButton.setText("Load more");
        panel7.add(loadMoreButton, new com.intellij.uiDesigner.core.GridConstraints(5, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        refreshButton = new JButton();
        refreshButton.setText("Refresh");
        panel7.add(refreshButton, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        longPollStatus = new JLabel();
        longPollStatus.setText("Long poll");
        panel7.add(longPollStatus, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        messagesList = new JList();
        panel7.add(messagesList, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        challengePane = new JPanel();
        challengePane.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        challengePane.setVisible(false);
        root.add(challengePane, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        challengeLabel = new JLabel();
        challengeLabel.setText("Label");
        challengePane.add(challengeLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        challengeField = new JTextField();
        challengePane.add(challengeField, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        sendChallenge = new JButton();
        sendChallenge.setText("Send");
        challengePane.add(sendChallenge, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return root;
    }


    private interface Failable<T> {
        T run() throws Exception;
    }
}
