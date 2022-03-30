package ru.justagod.vk.backend.model;

import java.time.Instant;

public record Message(
        User sender,
        User receiver,
        Instant sentAt,
        String content
) {}
