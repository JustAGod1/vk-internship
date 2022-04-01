package ru.justagod.vk.data;

public class AuthorizedUserRequest extends AuthorizedRequest{

    private User friend;

    public AuthorizedUserRequest(String session, User friend) {
        super(session);
        this.friend = friend;
    }

    public User friend() {
        return friend;
    }
}
