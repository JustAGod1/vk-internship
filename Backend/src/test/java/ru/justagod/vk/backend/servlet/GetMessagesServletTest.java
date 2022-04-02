package ru.justagod.vk.backend.servlet;

import org.junit.jupiter.api.Test;
import ru.justagod.vk.backend.EnhancedRandom;
import ru.justagod.vk.data.*;
import ru.justagod.vk.frontend.http.HttpClient;
import ru.justagod.vk.frontend.http.ServerResponse;
import ru.justagod.vk.network.Endpoint;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class GetMessagesServletTest extends ServletBaseTest {


    @Test
    void batchMessages() throws Exception {
        User ivan = database.addUser("***", "ip");
        User sergey = database.addUser("***", "sp");

        Session session = sessions.updateUserSession(ivan);


        Random random = new Random(777);

        Stream<Message> messagesGenerator = IntStream
                .generate(new AtomicInteger()::incrementAndGet)
                .mapToObj((v) -> EnhancedRandom.randomMessageAndDirection(ivan, sergey, random, 30, Instant.ofEpochSecond(v)));

        int count = random.nextInt(2000);

        List<Message> messages = messagesGenerator.limit(count).collect(Collectors.toList());
        Collections.reverse(messages);

        TreeMap<Instant, Message> treeMap = new TreeMap<>();

        for (Message message : messages) {
            treeMap.put(message.sentAt(), message);
            database.addMessage(message);
        }

        int iterations = random.nextInt(300);

        HttpClient client = connect();
        for (int i = 0; i < iterations; i++) {
            Message pivot = messages.get(random.nextInt(messages.size()));

            ServerResponse<Messages> response = client.sendRequest(
                    Endpoint.GET_MESSAGES_REQUEST_ENDPOINT,
                    new MessagesRequest(
                            session.value(),
                            sergey,
                            pivot.sentAt()
                    )
            );
            if (!response.success() && response.response().error().kind() == BackendError.CHALLENGE_REQUIRED) {
                protection.solveChallenge("127.0.0.1");
                response = client.sendRequest(
                        Endpoint.GET_MESSAGES_REQUEST_ENDPOINT,
                        new MessagesRequest(
                                session.value(),
                                sergey,
                                pivot.sentAt()
                        )
                );
            }
            List<Message> actual = assertSuccess(response).messages();
            List<Message> expected = new ArrayList<>();

            while (pivot != null && expected.size() < 100) {
                expected.add(pivot);
                var entry = treeMap.lowerEntry(pivot.sentAt());
                if (entry != null) {
                    pivot = entry.getValue();
                } else {
                    pivot = null;
                }
            }

            assertEquals(expected, actual);

        }
    }

    @Test
    void badRequest() throws Exception {
        String session = "fkdjfkslj";
        User user = database.addUser("***", "ivan");


        mockDatabaseReadOnly();
        HttpClient client = connect();

        var response = client.sendRequest(
                Endpoint.GET_MESSAGES_REQUEST_ENDPOINT,
                new MessagesRequest(
                        null, user, Instant.now()
                )
        );
        assertBadRequest(response);

        response = client.sendRequest(
                Endpoint.GET_MESSAGES_REQUEST_ENDPOINT,
                new MessagesRequest(
                        session, null, Instant.now()
                )
        );
        assertBadRequest(response);

        response = client.sendRequest(
                Endpoint.GET_MESSAGES_REQUEST_ENDPOINT,
                new MessagesRequest(
                        session, user, null
                )
        );
        assertBadRequest(response);

        response = client.sendRequest(
                Endpoint.GET_MESSAGES_REQUEST_ENDPOINT,
                new MessagesRequest(
                        null, user, null
                )
        );
        assertBadRequest(response);

        response = client.sendRequest(
                Endpoint.GET_MESSAGES_REQUEST_ENDPOINT,
                new MessagesRequest(
                        null, null, Instant.now()
                )
        );
        assertBadRequest(response);

        response = client.sendRequest(
                Endpoint.GET_MESSAGES_REQUEST_ENDPOINT,
                new MessagesRequest(
                        session, null, null
                )
        );
        assertBadRequest(response);

        response = client.sendRequest(
                Endpoint.GET_MESSAGES_REQUEST_ENDPOINT,
                new MessagesRequest(
                        null, null, null
                )
        );
        assertBadRequest(response);
    }

    @Test
    void notAuthorized() throws IOException {
        HttpClient client = connect();

        var response = client.sendRequest(
                Endpoint.GET_MESSAGES_REQUEST_ENDPOINT,
                new MessagesRequest(
                        "kfjsdklfjs", new User(UUID.randomUUID()), Instant.now()
                )
        );

        assertUnauthorized(response);
    }
}