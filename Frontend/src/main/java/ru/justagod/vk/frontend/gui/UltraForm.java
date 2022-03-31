package ru.justagod.vk.frontend.gui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.justagod.vk.data.Session;
import ru.justagod.vk.data.UserPasswordRequest;
import ru.justagod.vk.frontend.Main;
import ru.justagod.vk.frontend.http.ServerResponse;
import ru.justagod.vk.network.Endpoint;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

public class UltraForm extends JFrame {
    private JButton signUpButton;
    private JPanel root;
    private JTextField username;
    private JTextField password;
    private JLabel errorLabel;
    private JButton signInButton;

    @Nullable
    private Session session = null;

    public UltraForm() throws HeadlessException {
        setTitle("Sign Up");
        setContentPane(root);
        pack();

        signUpButton.addActionListener(this::onSignUp);
        signInButton.addActionListener(this::onSignIn);
    }

    private void clearError() {
        errorLabel.setText("");
    }

    private void setError(String error) {
        errorLabel.setText(error);
    }

    private void onSignIn(ActionEvent actionEvent) {
        requestSession(Endpoint.SIGN_IN_REQUEST_ENDPOINT);
    }

    private void onSignUp(ActionEvent actionEvent) {
        requestSession(Endpoint.SIGN_UP_REQUEST_ENDPOINT);
    }

    private void requestSession(Endpoint<UserPasswordRequest, Session> endpoint) {
        clearError();
        try {
            ServerResponse<Session> response = Main.client.sendRequest(endpoint,
                    new UserPasswordRequest(
                           username.getText(),
                            password.getText()
                    )
            );

            if (!response.success()) {
                session = null;
                setError(response.error().toString());
            } else {
                session = response.response();
            }
            switchAuthorizedView();
        } catch (IOException e) {
            e.printStackTrace();
            errorLabel.setText(e.toString());
        }
    }

    private void switchAuthorizedView() {

    }

}
