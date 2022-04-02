package ru.justagod.vk.backend.servlet;

import org.junit.jupiter.api.Test;
import ru.justagod.vk.backend.db.PasswordsManager;
import ru.justagod.vk.data.BackendError;
import ru.justagod.vk.data.User;
import ru.justagod.vk.data.UserPasswordRequest;
import ru.justagod.vk.frontend.http.HttpClient;
import ru.justagod.vk.network.Endpoint;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class SignInServletTest extends ServletBaseTest {

    @Test
    void signIn() throws Exception {
        String username = "Ivan";
        String password = "***";

        User user = database.addUser(PasswordsManager.hashed(password), username);

        HttpClient client = connect();

        var response = client.sendRequest(Endpoint.SIGN_IN_REQUEST_ENDPOINT,
                new UserPasswordRequest(
                        username, password
                )
        );

        assertEquals(user, assertSuccess(response).user());
    }

    @Test
    void badRequest() throws Exception {
        mockDatabaseReadOnly();
        String username = "Ivan";
        String password = "***";

        HttpClient client = connect();

        var response = client.sendRequest(Endpoint.SIGN_IN_REQUEST_ENDPOINT,
                new UserPasswordRequest(
                        username, null
                )
        );
        assertBadRequest(response);

        response = client.sendRequest(Endpoint.SIGN_IN_REQUEST_ENDPOINT,
                new UserPasswordRequest(
                        null, null
                )
        );
        assertBadRequest(response);

        response = client.sendRequest(Endpoint.SIGN_IN_REQUEST_ENDPOINT,
                new UserPasswordRequest(
                        null, password
                )
        );
        assertBadRequest(response);
    }

    @Test
    void noMatch() throws Exception {
        mockDatabaseReadOnly();
        HttpClient client = connect();

        var response = client.sendRequest(
                Endpoint.SIGN_IN_REQUEST_ENDPOINT,
                new UserPasswordRequest(
                        "Ivan", "***"
                )
        );

        assertErrorKind(BackendError.WRONG_USERNAME_OR_PASSWORD, response);
    }

}