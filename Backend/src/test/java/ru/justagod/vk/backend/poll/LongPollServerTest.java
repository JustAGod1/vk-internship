package ru.justagod.vk.backend.poll;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import ru.justagod.vk.backend.EnhancedRandom;
import ru.justagod.vk.backend.control.SessionsManager;
import ru.justagod.vk.data.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LongPollServerTest {

    private Random random = new Random(777);
    private final Gson gson = GsonHolder.gson;

    private SessionsManager sessions;
    private ScheduledExecutorService executor;
    private LongPollServer server;

    private static MockedStatic<Clock> clockMock;

    private void mockInstant(Instant instant) {
        if (clockMock != null) {
            clockMock.close();
        }
        Clock spyClock = spy(Clock.class);
        clockMock = mockStatic(Clock.class);
        clockMock.when(Clock::systemUTC).thenReturn(spyClock);
        when(spyClock.instant()).thenReturn(instant);
    }

    @BeforeEach
    void prepare() throws IOException {
        random = new Random(777);
        executor = Executors.newScheduledThreadPool(5);
        sessions = SessionsManager.create(executor);
        server = LongPollServer.start(executor, gson, sessions, 0);
    }

    @AfterEach
    void shutdown() throws IOException {
        executor.shutdownNow();
        server.stop();
        if (clockMock != null) {
            clockMock.close();
            clockMock = null;
        }
    }

    <T> BackendError assertError(BackendResponse<T> response) {
        assertFalse(response.isSuccess(),response.toString());
        return response.error();
    }

    <T> T assertSuccess(BackendResponse<T> response) {
        assertTrue(response.isSuccess(), response.toString());
        return response.payload();
    }

    @Test
    void wrongSession() throws IOException {
        LongPollTestConnection connection = makeConnection();

        String cookie = assertSuccess(connection.read(String.class));
        connection.write(cookie);
        assertNull(assertSuccess(connection.read(String.class)));
        connection.write("kkjdkajf");
        assertEquals(assertError(connection.read(String.class)), new BackendError(BackendError.FORBIDDEN, null));

    }

    @Test
    void rightSession() throws IOException {
        LongPollTestConnection connection = makeConnection();
        User user = new User(UUID.randomUUID());

        Session session = sessions.updateUserSession(user);

        String cookie = assertSuccess(connection.read(String.class));
        connection.write(cookie);
        assertNull(assertSuccess(connection.read(String.class)));
        connection.write(session.value());
        assertNull(assertSuccess(connection.read(String.class)));
    }

    private LongPollTestConnection authorize(User user) throws IOException {
        LongPollTestConnection connection = makeConnection();
        Session session = sessions.updateUserSession(user);

        authorize(session, connection);

        return connection;
    }

    @NotNull
    private LongPollTestConnection makeConnection() throws IOException {
        return LongPollTestConnection.connect(new InetSocketAddress(server.getPort()));
    }

    private void authorize(Session session, LongPollTestConnection connection) throws IOException {
        String cookie = assertSuccess(connection.read(String.class));
        connection.write(cookie);
        assertNull(assertSuccess(connection.read(String.class)));
        connection.write(session.value());
        assertNull(assertSuccess(connection.read(String.class)));
    }

    @Test
    void tooManyRequestsBan() throws Exception {
        User user = new User(UUID.randomUUID());

        LongPollTestConnection connection = authorize(user);

        mockInstant(Instant.EPOCH);
        for (int i = 0; i < LongPollServerConnection.MAX_REQUESTS*2; i++) {
            connection.write(null);
            BackendResponse<Messages> response = connection.read(Messages.class);
            if (!response.isSuccess()) {
                assertEquals(response.error().kind(), BackendError.TOO_MANY_REQUESTS);
                return;
            }
        }
        assertFalse(false, "Server was expected to ban us");
    }

    @Test
    void tooBigPayload() throws IOException {
        LongPollTestConnection connection = makeConnection();
        connection.write("k".repeat(LongPollServerConnection.MAX_MESSAGE_SIZE + 1));
        connection.read(JsonElement.class); // Here we get fancy new cookie
        assertError(connection.read(JsonElement.class));
    }

    @Test
    void messagesAcquiring() throws Exception {
        User ivan = new User(UUID.randomUUID());
        User sergey = new User(UUID.randomUUID());

        LongPollTestConnection ivanConnection = authorize(ivan);
        LongPollTestConnection sergeyConnection = authorize(sergey);

        checkMessagesReceiving(ivan, ivanConnection, sergey, sergeyConnection);
    }

    @Test
    void messagesAcquiringReenter() throws Exception {
        User ivan = new User(UUID.randomUUID());
        User sergey = new User(UUID.randomUUID());

        LongPollTestConnection ivanConnection = authorize(ivan);
        LongPollTestConnection sergeyConnection = authorize(sergey);
        authorize(sergey);

        assertThrows(SocketException.class, () -> checkMessagesReceiving(ivan, ivanConnection, sergey, sergeyConnection));
    }

    private void checkMessagesReceiving(User ivan,
                                        LongPollTestConnection ivanConnection,
                                        User sergey,
                                        LongPollTestConnection sergeyConnection) throws Exception {
        List<Message> messages = IntStream.range(0, random.nextInt(100))
                .mapToObj(i -> EnhancedRandom.randomMessageAndDirection(ivan, sergey, random, 300))
                .sorted(Comparator.comparing(Message::sentAt)).collect(Collectors.toList());

        ExecutorService executor = Executors.newFixedThreadPool(5);
        try {
            var chucks = Lists.partition(messages, 30);

            for (List<Message> chuck : chucks) {
                List<Future<Exception>> futures = new ArrayList<>();
                for (Message message : chuck) {
                    Future<Exception> future = executor
                            .submit(() -> {
                                try {
                                    LongPollCommunicationHandler.getConnection(ivan).addEvent(message);
                                    LongPollCommunicationHandler.getConnection(sergey).addEvent(message);
                                } catch (Exception e) {
                                    return e;
                                }
                                return null;
                            });
                    futures.add(future);
                }

                for (Future<Exception> future : futures) {
                    Exception e = future.get();
                    if (e != null) throw e;
                }
            }
        } finally {
            executor.shutdownNow();
        }

        List<Message> actual = new ArrayList<>();
        while (actual.size() < messages.size()) {
            ivanConnection.write(null);
            actual.addAll(assertSuccess(ivanConnection.read(Messages.class)).messages());
        }
        actual.sort(Comparator.comparing(Message::sentAt));
        assertThrows(SocketTimeoutException.class, () -> ivanConnection.read(Messages.class));
        assertEquals(messages, actual);

        actual.clear();
        while (actual.size() < messages.size()) {
            sergeyConnection.write(null);
            actual.addAll(assertSuccess(sergeyConnection.read(Messages.class)).messages());
        }
        actual.sort(Comparator.comparing(Message::sentAt));
        assertThrows(SocketTimeoutException.class, () -> ivanConnection.read(Messages.class));
        assertEquals(messages, actual);
    }


}