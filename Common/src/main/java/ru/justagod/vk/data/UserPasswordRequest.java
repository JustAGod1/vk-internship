package ru.justagod.vk.data;

public final class UserPasswordRequest {
    private String username;
    private String password;

    public UserPasswordRequest(
            String username,
            String password // Not hash
    ) {
        this.username = username;
        this.password = password;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

}
