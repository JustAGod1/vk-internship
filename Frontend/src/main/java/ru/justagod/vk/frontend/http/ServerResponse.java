package ru.justagod.vk.frontend.http;

import ru.justagod.vk.data.BackendError;

public record ServerResponse<Response>(
        // Is not null only if request was successful
        Response response,
        // Is not null only if request was not successful
        BackendError error,
        // HTTP status code returned by server
        int status
) {

    public boolean success() {
        return status >= 200 && status < 300;
    }

    public static <T> ServerResponse<T> err(int code, BackendError error) {
        return new ServerResponse<T>(null, error, code);
    }

    public static <T> ServerResponse<T> success(int code, T response) {
        return new ServerResponse<>(response, null, code);
    }

}
