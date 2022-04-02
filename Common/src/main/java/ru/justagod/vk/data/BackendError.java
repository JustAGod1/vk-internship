package ru.justagod.vk.data;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class BackendError {

    public static final int CONNECTED_FROM_ANOTHER_LOCATION = -9;
    public static final int PROTOCOL_ERROR = -8;
    public static final int BAD_COOKIE = -7;
    public static final int QUEUE_OVERFLOW = -6;
    public static final int BUFFER_OVERRUN = -5;
    public static final int WRONG_CHALLENGE_ANSWER = -4;
    public static final int CHALLENGE_IS_NOT_REQUIRED = -3;
    public static final int TOO_MANY_REQUESTS = -3;
    public static final int GENERIC_ERROR = -1;
    public static final int BAD_REQUEST = 0;
    public static final int CHALLENGE_REQUIRED = 1;
    public static final int USERNAME_ALREADY_EXISTS = 2;
    public static final int WRONG_USERNAME_OR_PASSWORD = 3;
    public static final int FORBIDDEN = 4;
    public static final int FRIEND_ALREADY_ADDED = 5;
    public static final int FRIEND_ALREADY_REMOVED = 6;


    private static final Map<Integer, String> codeNames = new HashMap<>();
    private int kind;
    private String payload;

    public BackendError(int kind, String payload) {
        this.kind = kind;
        this.payload = payload;
    }

    public static String codeName(int code) {
        String name = codeNames.get(code);
        if (name == null) {
            name = String.format("UNKNOWN_ERROR(%d)", code);
        }
        return name;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(codeName(this.kind));
        if (payload != null) {
            result.append(" ");
            result.append(payload);
        }
        return result.toString();
    }

    public int kind() {
        return kind;
    }

    public String payload() {
        return payload;
    }

    static {
        for (Field field : BackendError.class.getFields()) {
            try {
                codeNames.put(field.getInt(null), field.getName());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BackendError that = (BackendError) o;

        if (kind != that.kind) return false;
        return payload != null ? payload.equals(that.payload) : that.payload == null;
    }

    @Override
    public int hashCode() {
        int result = kind;
        result = 31 * result + (payload != null ? payload.hashCode() : 0);
        return result;
    }
}
