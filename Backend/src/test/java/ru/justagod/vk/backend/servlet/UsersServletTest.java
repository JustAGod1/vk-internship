package ru.justagod.vk.backend.servlet;

import org.junit.jupiter.api.Test;
import ru.justagod.vk.backend.EnhancedRandom;
import ru.justagod.vk.data.*;
import ru.justagod.vk.frontend.http.HttpClient;
import ru.justagod.vk.network.Endpoint;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class UsersServletTest extends ServletBaseTest {


    @Test
    void requestUsers() throws IOException {
        User user = new User(UUID.randomUUID());
        Session session = sessions.updateUserSession(user);
        Random random = new Random(777);
        var users = IntStream.range(0, 5)
                .mapToObj((a) -> EnhancedRandom.randomString(random, random.nextInt(10)))
                .map((name) -> new UserName(database.addUser("***", name), name))
                .sorted(Comparator.comparing(UserName::username))
                .toList();

        HttpClient client = connect();

        BackendResponse<UsersListResponse> response = client.sendRequest(
                        Endpoint.USERS_REQUEST_ENDPOINT,
                        new AuthorizedRequest(session.value())
                ).response();
        List<UserName> usersResponse = assertSuccess(response).users();

        usersResponse.sort(Comparator.comparing(UserName::username));

        assertEquals(users, usersResponse);
    }

    @Test
    void badRequest() throws Exception {
        mockDatabaseReadOnly();
        HttpClient client = connect();

        var response = client.sendRequest(
                Endpoint.USERS_REQUEST_ENDPOINT,
                null
        );
        assertBadRequest(response);

        response = client.sendRequest(
                Endpoint.USERS_REQUEST_ENDPOINT,
                new AuthorizedRequest(null)
        );
        assertBadRequest(response);
    }

    @Test
    void unauthorized() throws Exception {
        mockDatabaseReadOnly();
        HttpClient client = connect();

        var response = client.sendRequest(
                Endpoint.USERS_REQUEST_ENDPOINT,
                new AuthorizedRequest(UUID.randomUUID().toString())
        );
        assertUnauthorized(response);
    }

}