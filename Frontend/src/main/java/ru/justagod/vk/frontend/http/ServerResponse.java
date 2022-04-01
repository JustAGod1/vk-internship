package ru.justagod.vk.frontend.http;

import ru.justagod.vk.data.BackendError;
import ru.justagod.vk.data.BackendResponse;

public record ServerResponse<Response>(
        BackendResponse<Response> response,
        // HTTP status code returned by server
        int status
) {

    public boolean success() {
        return status >= 200 && status < 300;
    }

}
