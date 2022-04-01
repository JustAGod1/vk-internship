package ru.justagod.vk.data;

import java.util.Objects;

public final class UserName {
    private final User user;
    private final String username;

    public UserName(User user, String username) {
        this.user = user;
        this.username = username;
    }

    public User user() {
        return user;
    }

    public String username() {
        return username;
    }

    @Override
    public String toString() {
        return "UserName[" +
                "user=" + user + ", " +
                "username=" + username + ']';
    }

}
