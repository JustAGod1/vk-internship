package ru.justagod.vk.backend.servlet;

import org.junit.jupiter.api.Test;
import ru.justagod.vk.backend.EnhancedRandom;
import ru.justagod.vk.data.*;
import ru.justagod.vk.frontend.http.HttpClient;
import ru.justagod.vk.network.Endpoint;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class AddFriendServletTest extends ServletBaseTest {

    @Test
    void addFriend() throws IOException {
        Random random = new Random(777);
        User user = database.addUser("***", "ivan");
        int count = EnhancedRandom.randomInt(random, 30, 200);

        List<User> friends = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            User u = database.addUser("***", EnhancedRandom.randomString(random, 10));
            if (random.nextBoolean()) {
                friends.add(u);
            }
        }

        HttpClient client = connect();

        Session session = sessions.updateUserSession(user);

        List<User> addedFriends = new ArrayList<>();

        for (User friend : friends) {
            var response = client.sendRequest(
                    Endpoint.ADD_FRIEND_REQUEST_ENDPOINT,
                    new AuthorizedUserRequest(session.value(), friend)
            );
            if (!response.success() && response.response().error().kind() == BackendError.CHALLENGE_REQUIRED) {
                protection.solveChallenge("127.0.0.1");
                response = client.sendRequest(
                        Endpoint.ADD_FRIEND_REQUEST_ENDPOINT,
                        new AuthorizedUserRequest(session.value(), friend)
                );
            }
            assertSuccess(response);
            addedFriends.add(friend);
            var actualFriends = database.requestFriends(user).stream().map(UserName::user).collect(Collectors.toList());

            assertEquals(addedFriends, actualFriends);
        }
    }

    @Test
    void alreadyAddedDirect() throws IOException {
        User ivan = database.addUser("***", "ivan");
        User sergey = database.addUser("***", "sergey");
        Session session = sessions.updateUserSession(ivan);

        database.addFriend(ivan, sergey);

        HttpClient client = connect();

        var response = client.sendRequest(
                Endpoint.ADD_FRIEND_REQUEST_ENDPOINT,
                new AuthorizedUserRequest(session.value(), sergey)
        );

        assertErrorKind(BackendError.FRIEND_ALREADY_ADDED, response);
    }

    @Test
    void alreadyAddedIndirect() throws IOException {
        User ivan = database.addUser("***", "ivan");
        User sergey = database.addUser("***", "sergey");
        Session session = sessions.updateUserSession(ivan);

        HttpClient client = connect();

        var response = client.sendRequest(
                Endpoint.ADD_FRIEND_REQUEST_ENDPOINT,
                new AuthorizedUserRequest(session.value(), sergey)
        );
        assertSuccess(response);

        response = client.sendRequest(
                Endpoint.ADD_FRIEND_REQUEST_ENDPOINT,
                new AuthorizedUserRequest(session.value(), sergey)
        );
        assertErrorKind(BackendError.FRIEND_ALREADY_ADDED, response);
    }

    @Test
    void badRequest() throws Exception {
        User user = database.addUser("***", "ivan");
        mockDatabaseReadOnly();

        HttpClient client = connect();

        var response = client.sendRequest(
                Endpoint.ADD_FRIEND_REQUEST_ENDPOINT,
                new AuthorizedUserRequest(UUID.randomUUID().toString(), null)
        );
        assertBadRequest(response);

        response = client.sendRequest(
                Endpoint.ADD_FRIEND_REQUEST_ENDPOINT,
                new AuthorizedUserRequest(null, user)
        );
        assertBadRequest(response);

        response = client.sendRequest(
                Endpoint.ADD_FRIEND_REQUEST_ENDPOINT,
                new AuthorizedUserRequest(null, null)
        );
        assertBadRequest(response);
    }

    @Test
    void notAuthorized() throws Exception {
        mockDatabaseReadOnly();
        HttpClient client = connect();

        var response = client.sendRequest(
                Endpoint.ADD_FRIEND_REQUEST_ENDPOINT,
                new AuthorizedUserRequest(UUID.randomUUID().toString(), new User(UUID.randomUUID()))
        );
        assertUnauthorized(response);

    }

}