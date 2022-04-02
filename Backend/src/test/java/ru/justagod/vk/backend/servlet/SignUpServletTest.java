package ru.justagod.vk.backend.servlet;

import org.junit.jupiter.api.Test;
import ru.justagod.vk.backend.db.PasswordsManager;
import ru.justagod.vk.data.BackendError;
import ru.justagod.vk.data.SessionResponse;
import ru.justagod.vk.data.User;
import ru.justagod.vk.data.UserPasswordRequest;
import ru.justagod.vk.frontend.http.HttpClient;
import ru.justagod.vk.network.Endpoint;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class SignUpServletTest extends ServletBaseTest {

    private void validateResponse(SessionResponse session) {
        assertNotNull(session);

        assertNotNull(session.session());
        assertNotNull(session.session().value());
        assertNotNull(session.session().validUntil());

        assertNotNull(session.user());

    }

    @Test
    void newUser() throws Exception {
        String username = "ivan";
        String password = "***";

        HttpClient client = connect();
        var response = client.sendRequest(
                Endpoint.SIGN_UP_REQUEST_ENDPOINT,
                new UserPasswordRequest(username, password)
        );
        SessionResponse session = assertSuccess(response.response());
        validateResponse(session);

        User user = database.findUser(username, PasswordsManager.hashed(password));
        assertNotNull(user);

        assertEquals(user, sessions.getSessionOwner(session.session().value()));
    }

    @Test
    void usernameCollision() throws Exception {
        String username = "ivan";
        String password = "***";

        HttpClient client = connect();
        var response = client.sendRequest(Endpoint.SIGN_UP_REQUEST_ENDPOINT, new UserPasswordRequest(username, password));
        validateResponse(assertSuccess(response.response()));

        response = client.sendRequest(Endpoint.SIGN_UP_REQUEST_ENDPOINT, new UserPasswordRequest(username, password));
        assertEquals(assertError(response.response()).kind(), BackendError.USERNAME_ALREADY_EXISTS);
    }

    @Test
    void badRequest() throws Exception {
        mockDatabaseReadOnly();
        String username = "ivan";
        String password = "***";

        HttpClient client = connect();
        var response = client.sendRequest(Endpoint.SIGN_UP_REQUEST_ENDPOINT, new UserPasswordRequest(username, null));
        assertBadRequest(response);

        response = client.sendRequest(Endpoint.SIGN_UP_REQUEST_ENDPOINT, new UserPasswordRequest(null, null));
        assertBadRequest(response);

        response = client.sendRequest(Endpoint.SIGN_UP_REQUEST_ENDPOINT, new UserPasswordRequest(null, password));
        assertBadRequest(response);
    }
}