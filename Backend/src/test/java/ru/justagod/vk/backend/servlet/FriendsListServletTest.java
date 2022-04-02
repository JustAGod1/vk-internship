package ru.justagod.vk.backend.servlet;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import ru.justagod.vk.backend.EnhancedRandom;
import ru.justagod.vk.data.*;
import ru.justagod.vk.frontend.http.HttpClient;
import ru.justagod.vk.network.Endpoint;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class FriendsListServletTest extends ServletBaseTest {

    @Test
    void listFriends() throws IOException {
        Random random = new Random(777);
        User user = database.addUser("***", "ivan");
        int count = EnhancedRandom.randomInt(random, 30, 200);

        List<User> friends = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            User u = database.addUser("***", EnhancedRandom.randomString(random, 10));
            if (random.nextBoolean()) {
                friends.add(u);
                database.addFriend(user, u);
            }
        }

        Session session = sessions.updateUserSession(user);

        HttpClient client = connect();

        var response = client.sendRequest(
                Endpoint.FRIENDS_REQUEST_ENDPOINT,
                new AuthorizedRequest(session.value())
        );

        var friendsResponse = assertSuccess(response).users().stream().map(UserName::user).collect(Collectors.toList());

        assertEquals(friends, friendsResponse);

        for (User friend : friends) {
            response = client.sendRequest(
                    Endpoint.FRIENDS_REQUEST_ENDPOINT,
                    new AuthorizedRequest(sessions.updateUserSession(friend).value())
            );
            if (!response.success() && response.response().error().kind() == BackendError.CHALLENGE_REQUIRED) {
                protection.solveChallenge("127.0.0.1");
                response = client.sendRequest(
                        Endpoint.FRIENDS_REQUEST_ENDPOINT,
                        new AuthorizedRequest(sessions.updateUserSession(friend).value())
                );
            }
            assertEquals(assertSuccess(response).users(), Collections.emptyList());
        }
    }

    @Test
    void badRequest() throws Exception {
        mockDatabaseReadOnly();
        HttpClient client = connect();

        var response = client.sendRequest(
                Endpoint.FRIENDS_REQUEST_ENDPOINT,
                new AuthorizedRequest(null)
        );

        assertBadRequest(response);
    }

    @Test
    void notAuthorized() throws IOException {
        HttpClient client = connect();

        var response = client.sendRequest(
                Endpoint.FRIENDS_REQUEST_ENDPOINT,
                new AuthorizedRequest(UUID.randomUUID().toString())
        );

        assertUnauthorized(response);
    }
}