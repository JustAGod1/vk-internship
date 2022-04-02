package ru.justagod.vk.backend.servlet;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import ru.justagod.vk.backend.EnhancedRandom;
import ru.justagod.vk.data.Message;
import ru.justagod.vk.data.SendMessageRequest;
import ru.justagod.vk.data.Session;
import ru.justagod.vk.data.User;
import ru.justagod.vk.frontend.http.HttpClient;
import ru.justagod.vk.network.Endpoint;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class SendMessageServletTest extends ServletBaseTest {


    @Test
    void sendMessage() throws IOException, InterruptedException {
        Random random = new Random(777);
        HttpClient client = connect();

        User ivan = database.addUser("***", "Ivan");
        User sergey = database.addUser("***", "Sergey");

        Session session = sessions.updateUserSession(ivan);

        Instant before = Instant.now();
        Thread.sleep(1);
        String content = EnhancedRandom.randomString(random, 20);
        var response = client.sendRequest(
                Endpoint.SEND_MESSAGE_REQUEST_ENDPOINT,
                new SendMessageRequest(
                        session.value(),
                        content,
                        sergey
                )
        );

        assertSuccess(response);
        Thread.sleep(1);

        List<Message> actual = database.readMessages(Instant.now(), ivan, sergey);

        assertEquals(actual.size(), 1);

        assertEquals(actual.get(0).receiver(), sergey);
        assertEquals(actual.get(0).sender(), ivan);
        assertEquals(actual.get(0).content(), content);

        Instant sentAt = actual.get(0).sentAt();
        assertTrue(sentAt.isBefore(Instant.now()));
        assertTrue(sentAt.isAfter(before));
    }

    @Test
    void badRequest() throws Exception {

        User ivan = database.addUser("***", "Ivan");
        User sergey = database.addUser("***", "Sergey");
        mockDatabaseReadOnly();
        HttpClient client = connect();

        Session session = sessions.updateUserSession(ivan);

        String content = "kdfkjdskjf";

        var response = client.sendRequest(
                Endpoint.SEND_MESSAGE_REQUEST_ENDPOINT,
                new SendMessageRequest(
                        session.value(),
                        content,
                        null
                )
        );
        assertBadRequest(response);

        response = client.sendRequest(
                Endpoint.SEND_MESSAGE_REQUEST_ENDPOINT,
                new SendMessageRequest(
                        session.value(),
                        null,
                        sergey
                )
        );
        assertBadRequest(response);

        response = client.sendRequest(
                Endpoint.SEND_MESSAGE_REQUEST_ENDPOINT,
                new SendMessageRequest(
                        null,
                        content,
                        sergey
                )
        );
        assertBadRequest(response);

        response = client.sendRequest(
                Endpoint.SEND_MESSAGE_REQUEST_ENDPOINT,
                new SendMessageRequest(
                        null,
                        content,
                        null
                )
        );
        assertBadRequest(response);

        response = client.sendRequest(
                Endpoint.SEND_MESSAGE_REQUEST_ENDPOINT,
                new SendMessageRequest(
                        null,
                        null,
                        sergey
                )
        );
        assertBadRequest(response);

        response = client.sendRequest(
                Endpoint.SEND_MESSAGE_REQUEST_ENDPOINT,
                new SendMessageRequest(
                        session.value(),
                        null,
                        null
                )
        );
        assertBadRequest(response);

        response = client.sendRequest(
                Endpoint.SEND_MESSAGE_REQUEST_ENDPOINT,
                new SendMessageRequest(
                        null,
                        null,
                        null
                )
        );
        assertBadRequest(response);
    }
    @Test
    void notAuthorized() throws IOException, InterruptedException {
        Random random = new Random(777);
        HttpClient client = connect();

        User sergey = database.addUser("***", "Sergey");

        String content = EnhancedRandom.randomString(random, 20);
        var response = client.sendRequest(
                Endpoint.SEND_MESSAGE_REQUEST_ENDPOINT,
                new SendMessageRequest(
                        "kerjkejrflk",
                        content,
                        sergey
                )
        );

        assertUnauthorized(response);
    }

}