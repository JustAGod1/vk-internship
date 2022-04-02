package ru.justagod.vk.backend.servlet;

import org.junit.jupiter.api.Test;
import ru.justagod.vk.data.AuthorizedUserRequest;
import ru.justagod.vk.data.BackendError;
import ru.justagod.vk.data.Session;
import ru.justagod.vk.data.User;
import ru.justagod.vk.frontend.http.HttpClient;
import ru.justagod.vk.network.Endpoint;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RemoveFriendServletTest extends ServletBaseTest {

    @Test
    void removeFriend() throws IOException {
        User ivan = database.addUser("***", "Ivan");
        User sergey = database.addUser("***", "Sergey");

        database.addFriend(ivan, sergey);

        Session session = sessions.updateUserSession(ivan);

        HttpClient client = connect();
        var response = client.sendRequest(
                Endpoint.REMOVE_FRIEND_REQUEST_ENDPOINT,
                new AuthorizedUserRequest(session.value(), sergey)
        );

        assertSuccess(response);
    }

    @Test
    void notAuthorized() throws Exception {
        User ivan = database.addUser("***", "Ivan");
        User sergey = database.addUser("***", "Sergey");

        database.addFriend(ivan, sergey);
        mockDatabaseReadOnly();

        HttpClient client = connect();
        var response = client.sendRequest(
                Endpoint.REMOVE_FRIEND_REQUEST_ENDPOINT,
                new AuthorizedUserRequest(UUID.randomUUID().toString(), sergey)
        );

        assertUnauthorized(response);
    }


    @Test
    void noFriend() throws Exception {
        User ivan = database.addUser("***", "Ivan");
        User sergey = database.addUser("***", "Sergey");

        Session session = sessions.updateUserSession(ivan);

        HttpClient client = connect();
        var response = client.sendRequest(
                Endpoint.REMOVE_FRIEND_REQUEST_ENDPOINT,
                new AuthorizedUserRequest(session.value(), sergey)
        );

        assertErrorKind(BackendError.FRIEND_ALREADY_REMOVED, response);
    }

    @Test
    void badRequest() throws Exception {
        User ivan = database.addUser("***", "Ivan");
        User sergey = database.addUser("***", "Sergey");

        mockDatabaseReadOnly();
        Session session = sessions.updateUserSession(ivan);

        HttpClient client = connect();
        var response = client.sendRequest(
                Endpoint.REMOVE_FRIEND_REQUEST_ENDPOINT,
                new AuthorizedUserRequest(null, sergey)
        );
        assertBadRequest(response);

        response = client.sendRequest(
                Endpoint.REMOVE_FRIEND_REQUEST_ENDPOINT,
                new AuthorizedUserRequest(session.value(), null)
        );
        assertBadRequest(response);

        response = client.sendRequest(
                Endpoint.REMOVE_FRIEND_REQUEST_ENDPOINT,
                new AuthorizedUserRequest(null, null)
        );
        assertBadRequest(response);
    }
}