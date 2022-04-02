package ru.justagod.vk.data;

import java.time.Instant;

public class MessagesRequest extends AuthorizedUserRequest{

    private Instant before;

    public MessagesRequest(String session, User user, Instant before) {
        super(session, user);
        this.before = before;
    }

    public Instant before() {
        return before;
    }
}
