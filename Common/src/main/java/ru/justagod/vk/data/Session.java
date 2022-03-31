package ru.justagod.vk.data;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Session {
    private String value;
    private Instant validUntil;

    public Session(
            String value,
            Instant validUntil
    ) {
        this.value = value;
        this.validUntil = validUntil;
    }

    public static Session random(Instant validUntil) {
        return new Session(UUID.randomUUID().toString(), validUntil);
    }

    public String value() {
        return value;
    }

    public Instant validUntil() {
        return validUntil;
    }

    @Override
    public String toString() {
        return "Session[" +
                "value=" + value + ", " +
                "validUntil=" + validUntil + ']';
    }


}
