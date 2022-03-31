package ru.justagod.vk.data;

import com.google.gson.Gson;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class BackendResponse<T> {
    private @Nullable BackendError error;
    private @Nullable T payload;

    public BackendResponse(
            @Nullable
                    BackendError error,
            @Nullable
                    T payload
    ) {
        this.error = error;
        this.payload = payload;
    }

    public static <T> BackendResponse<T> badRequest() {
        return BackendResponse.error(new BackendError(BackendError.BAD_REQUEST, null));
    }

    public static <T> BackendResponse<T> success(T payload) {
        return new BackendResponse<>(null, payload);
    }

    public static <T> BackendResponse<T> error(int kind) {
        return error(new BackendError(kind, null));
    }

    public static <T> BackendResponse<T> error(BackendError error) {
        return new BackendResponse<T>(error, null);
    }

    public @Nullable BackendError error() {
        return error;
    }

    public @Nullable T payload() {
        return payload;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BackendResponse) obj;
        return Objects.equals(this.error, that.error) &&
                Objects.equals(this.payload, that.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(error, payload);
    }

    @Override
    public String toString() {
        return "BackendResponse[" +
                "error=" + error + ", " +
                "payload=" + payload + ']';
    }

}
