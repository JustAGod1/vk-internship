package ru.justagod.vk.data;

import com.google.gson.*;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class BackendResponse<T> {
    private @Nullable BackendError error;
    private @Nullable T payload;

    private BackendResponse(
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
    public static <T> BackendResponse<T> forbidden() {
        return BackendResponse.error(new BackendError(BackendError.FORBIDDEN, null));
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

    public String toJson(Gson gson) {
        return gson.toJson(this);
    }

    public static <T> BackendResponse<T> fromJson(Gson gson, String json, Class<T> responseClass) throws JsonParseException {
        JsonElement element = JsonParser.parseString(json);
        if (!element.isJsonObject()) throw new JsonSyntaxException("Given json is not an object");
        JsonObject object = element.getAsJsonObject();

        BackendResponse<T> result = new BackendResponse<>(
                parsePayload(gson, object.get("error"), BackendError.class),
                parsePayload(gson, object.get("payload"), responseClass)
        );

        if (result.error == null && result.payload == null) {
            throw new JsonSyntaxException("Either result or payload must not be null");
        }
        if (result.error != null && result.payload != null) {
            throw new JsonSyntaxException("Either result or payload must not be null");
        }

        return result;
    }

    private static <T> T parsePayload(Gson gson, JsonElement element, Class<T> payloadClass) {
        if (element == null) return null;
        if (element.isJsonNull()) return null;
        if (!element.isJsonObject()) throw new JsonSyntaxException("payload is not an object");
        return gson.fromJson(element.getAsJsonObject(), payloadClass);
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
