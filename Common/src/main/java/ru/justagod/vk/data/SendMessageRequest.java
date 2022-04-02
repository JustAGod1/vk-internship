package ru.justagod.vk.data;

public class SendMessageRequest extends AuthorizedRequest{

    private String payload;
    private User recipient;

    public SendMessageRequest(String session, String payload, User recipient) {
        super(session);
        this.payload = payload;
        this.recipient = recipient;
    }


    public String payload() {
        return payload;
    }

    public User recipient() {
        return recipient;
    }
}
