package ru.justagod.vk.backend.servlet.integration;

import org.checkerframework.common.value.qual.IntRange;
import org.junit.jupiter.api.Test;
import ru.justagod.vk.backend.EnhancedRandom;
import ru.justagod.vk.backend.servlet.ServletBaseTest;
import ru.justagod.vk.data.*;
import ru.justagod.vk.frontend.http.HttpClient;
import ru.justagod.vk.frontend.http.ServerResponse;
import ru.justagod.vk.network.Endpoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SimpleIntegrationTests extends ServletBaseTest {

    @Test
    void signInSignUp() throws IOException {
        HttpClient client = connect();

        String username = "ivan";
        String password = "***";

        var response = client.sendRequest(
                Endpoint.SIGN_IN_REQUEST_ENDPOINT,
                new UserPasswordRequest(username, password)
        );
        assertErrorKind(BackendError.WRONG_USERNAME_OR_PASSWORD, response);

        response = client.sendRequest(
                Endpoint.SIGN_UP_REQUEST_ENDPOINT,
                new UserPasswordRequest(username, password)
        );
        SessionResponse session = assertSuccess(response);

        response = client.sendRequest(
                Endpoint.SIGN_IN_REQUEST_ENDPOINT,
                new UserPasswordRequest(username, password)
        );
        session = assertSuccess(response);
    }

    @Test
    void addRemoveSomeFriends() throws IOException {
        HttpClient client = connect();
        Random random = new Random(777);

        int cnt = 20;

        List<SessionResponse> sessions = new ArrayList<>(cnt);

        for (int i = 0; i < cnt; i++) {
            String username = String.valueOf(i);
            var response = challengeByPass(
                    client,
                    Endpoint.SIGN_UP_REQUEST_ENDPOINT,
                    new UserPasswordRequest(
                            username, "***"
                    )
            );

            sessions.add(assertSuccess(response));
        }

        boolean[] graphMatrix = new boolean[sessions.size() * sessions.size()];

        int iterations = 10;
        for (int iteration = 0; iteration < iterations; iteration++) {
            for (int from = 0; from < cnt; from++) {
                for (int to = 0; to < cnt; to++) {
                    if (to == from) continue;
                    int idx = from * cnt + to;

                    boolean newValue = random.nextBoolean();
                    boolean before = graphMatrix[idx];

                    if (newValue != before) {
                        ServerResponse<Void> response;
                        if (before) {
                            response = challengeByPass(
                                    client,
                                    Endpoint.REMOVE_FRIEND_REQUEST_ENDPOINT,
                                    new AuthorizedUserRequest(
                                            sessions.get(from).session().value(),
                                            sessions.get(to).user()
                                    )
                            );
                        } else {
                            response = challengeByPass(
                                    client,
                                    Endpoint.ADD_FRIEND_REQUEST_ENDPOINT,
                                    new AuthorizedUserRequest(
                                            sessions.get(from).session().value(),
                                            sessions.get(to).user()
                                    )
                            );
                        }

                        assertSuccess(response);
                        graphMatrix[idx] = newValue;
                    }

                }
            }

        }

        for (int from = 0; from < cnt; from++) {
            SessionResponse session = sessions.get(from);
            List<User> expected = new ArrayList<>();

            for (int to = 0; to < cnt; to++) {
                int idx = from * cnt + to;

                if (graphMatrix[idx]) {
                    expected.add(sessions.get(to).user());
                }
            }

            var response = challengeByPass(
                    client,
                    Endpoint.FRIENDS_REQUEST_ENDPOINT,
                    new AuthorizedRequest(session.session().value())
            );

            List<User> actual = assertSuccess(response).users().stream().map(UserName::user).toList();
            assertEquals(expected, actual);
        }



    }

}
