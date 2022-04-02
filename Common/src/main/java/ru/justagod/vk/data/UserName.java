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


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserName userName = (UserName) o;

        if (user != null ? !user.equals(userName.user) : userName.user != null) return false;
        return username != null ? username.equals(userName.username) : userName.username == null;
    }

    @Override
    public int hashCode() {
        int result = user != null ? user.hashCode() : 0;
        result = 31 * result + (username != null ? username.hashCode() : 0);
        return result;
    }
}
