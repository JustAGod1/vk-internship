package ru.justagod.vk.data;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Message {
    private final UUID id;
    private final User sender;
    private final User receiver;
    private final Instant sentAt;
    private final String content;

    public Message(
            UUID id,
            User sender,
            User receiver,
            Instant sentAt,
            String content
    ) {
        this.id = id;
        this.sender = sender;
        this.receiver = receiver;
        this.sentAt = sentAt;
        this.content = content;
    }

    public static Message create(User sender, User receiver, Instant sentAt, String content) {
        return new Message(UUID.randomUUID(), sender, receiver, sentAt, content);
    }

    public UUID id() {
        return id;
    }

    public User sender() {
        return sender;
    }

    public User receiver() {
        return receiver;
    }

    public Instant sentAt() {
        return sentAt;
    }

    public String content() {
        return content;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Message) obj;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.sender, that.sender) &&
                Objects.equals(this.receiver, that.receiver) &&
                Objects.equals(this.sentAt, that.sentAt) &&
                Objects.equals(this.content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sender, receiver, sentAt, content);
    }

    @Override
    public String toString() {
        return "Message[" +
                "id=" + id + ", " +
                "sender=" + sender + ", " +
                "receiver=" + receiver + ", " +
                "sentAt=" + sentAt + ", " +
                "content=" + content + ']';
    }

}
