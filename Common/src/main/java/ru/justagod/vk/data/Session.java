package ru.justagod.vk.data;

import java.time.Instant;
import java.util.UUID;

public record Session(
        String value,
        Instant validUntil
) {

    public static Session random(Instant validUntil) {
        return new Session(UUID.randomUUID().toString(), validUntil);
    }

}
