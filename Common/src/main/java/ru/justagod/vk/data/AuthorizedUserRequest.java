package ru.justagod.vk.data;

public class AuthorizedUserRequest extends AuthorizedRequest{

    private User user;

    public AuthorizedUserRequest(String session, User user) {
        super(session);
        this.user = user;
    }

    public User user() {
        return user;
    }
}
