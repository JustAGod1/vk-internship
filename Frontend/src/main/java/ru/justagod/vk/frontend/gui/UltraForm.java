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
    private JPanel usersList;
    private JPanel friendsList;
    private JButton friendsUpdate;

    @Nullable
    private SessionResponse session = null;

    public UltraForm(HttpClient client) throws HeadlessException {
        this.client = client;
        setTitle("Sign Up");
        setContentPane(root);
        pack();


        signUpButton.addActionListener(this::onSignUp);
        signInButton.addActionListener(this::onSignIn);

        usersUpdate.addActionListener(a -> requestUsers());
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
            setError(response.error().toString());
        } else {
            session = response.response();
        }
        switchAuthorizedView();
    }

    private boolean checkError(ServerResponse<?> response) {
        if (!response.success()) {
            if (response.error().kind() == BackendError.FORBIDDEN) {
                session = null;
                switchAuthorizedView();
            } else {
                setError(response.error().toString());
            }
            return true;
        }
        return false;
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

        usersList.setLayout(null);
        int y = 0;
        for (UserName user : response.response().users()) {
            JLabel label = new JLabel(user.username());
            label.setLocation(0, y * 30);

            label.setSize(  100, 40);
            usersList.add(label);

            JButton addFriend = new JButton("+");
            addFriend.setLocation(100, y * 30 + 10);
            addFriend.setSize(20,20);
            usersList.add(addFriend);

            y++;
        }
        usersList.revalidate();
        usersList.repaint();
    }

    private void switchAuthorizedView() {
        if (session != null) {
            requestUsers();
        }
    }


    private interface Failable<T> {
        T run() throws Exception;
    }
}
