package ru.justagod.vk.data;

import java.util.Objects;

public final class AuthRequest {
    private String login;
    private String password;

    public AuthRequest(
            String login,
            String password
    ) {
        this.login = login;
        this.password = password;
    }

    public String login() {
        return login;
    }

    public String password() {
        return password;
    }

    @Override
    public String toString() {
        return "AuthRequest[" +
                "login=" + login + ", " +
                "password=" + password + ']';
    }
}
