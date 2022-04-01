package ru.justagod.vk.frontend.gui;

import org.jetbrains.annotations.Nullable;
import ru.justagod.vk.data.*;
import ru.justagod.vk.frontend.http.HttpClient;
import ru.justagod.vk.frontend.http.ServerResponse;
import ru.justagod.vk.network.Endpoint;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

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
        if (response == null) return;

        if (!response.success()) {
            session = null;
            setError(response.response().error().toString());
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
        if (checkError(response) && response.response().error().kind() != BackendError.CHALLENGE_IS_NOT_REQUIRED) return;

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
