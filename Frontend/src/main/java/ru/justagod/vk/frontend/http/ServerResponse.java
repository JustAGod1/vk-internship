package ru.justagod.vk.frontend.http;

import ru.justagod.vk.data.BackendError;
import ru.justagod.vk.data.BackendResponse;

public final class ServerResponse<Response> {
    private BackendResponse<Response> response;
    private int status;

    public ServerResponse(BackendResponse<Response> response, int status) {
        this.response = response;
        this.status = status;
    }

    public BackendResponse<Response> response() {
        return response;
    }

    public int status() {
        return status;
    }

    public boolean success() {
        return status >= 200 && status < 300;
    }

}
