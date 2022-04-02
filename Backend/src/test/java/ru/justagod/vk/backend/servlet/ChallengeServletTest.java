package ru.justagod.vk.backend.servlet;

import org.junit.jupiter.api.Test;
import ru.justagod.vk.backend.dos.ClientChallenge;
import ru.justagod.vk.backend.dos.UserState;
import ru.justagod.vk.backend.dos.UserStateController;
import ru.justagod.vk.data.BackendError;
import ru.justagod.vk.frontend.http.HttpClient;
import ru.justagod.vk.network.Endpoint;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ChallengeServletTest extends ServletBaseTest {
    private static final String ip = "127.0.0.1";

    @Test
    void solveChallenge() throws Exception {
        mockDatabaseReadOnly();
        HttpClient client = connect();
        for (int i = 0; i < UserState.MAX_REQUESTS; i++) {
            assertNull(protection.onRequest(ip));
        }
        ClientChallenge challenge = protection.onRequest(ip);
        assertNotNull(challenge);

        assertSuccess(client.sendRequest(Endpoint.SOLVE_CHALLENGE_REQUEST_ENDPOINT, challenge.getAnswer()));

        assertNull(protection.onRequest(ip));
    }

    @Test
    void noChallenge() throws Exception {
        mockDatabaseReadOnly();
        HttpClient client = connect();
        ClientChallenge challenge = protection.onRequest(ip);
        assertNull(challenge);

        assertErrorKind(
                BackendError.CHALLENGE_IS_NOT_REQUIRED,
                client.sendRequest(Endpoint.SOLVE_CHALLENGE_REQUEST_ENDPOINT, 0)
        );
    }

    @Test
    void wrongAnswer() throws Exception {
        mockDatabaseReadOnly();
        HttpClient client = connect();
        for (int i = 0; i < UserState.MAX_REQUESTS; i++) {
            assertNull(protection.onRequest(ip));
        }
        ClientChallenge challenge = protection.onRequest(ip);
        assertNotNull(challenge);

        assertErrorKind(BackendError.WRONG_CHALLENGE_ANSWER, client.sendRequest(
                Endpoint.SOLVE_CHALLENGE_REQUEST_ENDPOINT,
                challenge.getAnswer() + 1)
        );

        assertNotNull(protection.onRequest(ip));
    }
}