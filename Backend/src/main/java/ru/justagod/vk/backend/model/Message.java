package ru.justagod.vk.backend.model;

import java.time.Instant;
import java.util.UUID;

public record Message(
        UUID id,
        User sender,
        User receiver,
        Instant sentAt,
        String content
) {
    public static Message create(User sender, User receiver, Instant sentAt, String content) {
        return new Message(UUID.randomUUID(), sender, receiver, sentAt, content);
    }
}
