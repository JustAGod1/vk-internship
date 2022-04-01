package ru.justagod.vk.data;

import java.util.Objects;

public final class SessionResponse {
    private final Session session;
    private final User user;

    public SessionResponse(Session session, User user) {
        this.session = session;
        this.user = user;
    }

    public Session session() {
        return session;
    }

    public User user() {
        return user;
    }

    @Override
    public String toString() {
        return "SessionResponse[" +
                "session=" + session + ", " +
                "user=" + user + ']';
    }

}
