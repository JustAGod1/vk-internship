package ru.justagod.vk.data;

public class AuthorizedRequest {
    private String session;

    public AuthorizedRequest(String session) {
        this.session = session;
    }

    public String session() {
        return session;
    }
}
