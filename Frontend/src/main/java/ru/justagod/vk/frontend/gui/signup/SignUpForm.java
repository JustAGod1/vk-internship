package ru.justagod.vk.frontend.gui.signup;

import ru.justagod.vk.data.Session;
import ru.justagod.vk.data.SignUpRequest;
import ru.justagod.vk.frontend.Main;
import ru.justagod.vk.frontend.http.ServerResponse;
import ru.justagod.vk.network.Endpoint;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

public class SignUpForm extends JFrame {
    private JButton enterButton;
    private JPanel root;
    private JTextField username;
    private JTextField password;
    private JLabel errorLabel;

    public SignUpForm() throws HeadlessException {
        setTitle("Sign Up");
        setContentPane(root);
        pack();

        enterButton.addActionListener(this::onEnter);
    }

    private void onEnter(ActionEvent actionEvent) {
        try {
            ServerResponse<Session> response = Main.client.sendRequest(Endpoint.SIGN_UP_REQUEST_ENDPOINT,
                    new SignUpRequest(
                           username.getText(),
                            password.getText()
                    )
            );

            if (!response.success()) {
                errorLabel.setText(response.error().toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
            errorLabel.setText(e.toString());
        }
    }

}
