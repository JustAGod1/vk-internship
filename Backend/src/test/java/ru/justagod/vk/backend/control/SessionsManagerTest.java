package ru.justagod.vk.backend.control;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import ru.justagod.vk.backend.db.DatabaseManager;
import ru.justagod.vk.data.User;
import ru.justagod.vk.data.Session;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionsManagerTest {
    private static MockedStatic<Clock> clockMock;
    private ScheduledExecutorService executor;
    private DatabaseManager manager;


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
    public void prepare() throws IOException {
        manager = DatabaseManager.create(Files.createTempFile(null, null).toAbsolutePath().toString());
        mockInstant(Instant.EPOCH);
        executor = Executors.newScheduledThreadPool(5);
    }

    @AfterEach
    public void destroy() {
        executor.shutdownNow();
        if (clockMock != null) {
            clockMock.close();
            clockMock = null;
        }
    }


    @Test
    void testSessionCreation() {
        User user = manager.addUser("***", "ip");
        SessionsManager manager = SessionsManager.create(executor);

        Session session = manager.updateUserSession(user);

        assertEquals(manager.getSessionOwner(session.value()), user);
    }

    @Test
    void testSessionTimeout() {
        User user = manager.addUser("***", "ip");
        SessionsManager manager = SessionsManager.create(executor);

        Session session = manager.updateUserSession(user);
        assertEquals(manager.getSessionOwner(session.value()), user);

        mockInstant(Instant.now().plus(SessionsManager.SESSION_LIFETIME));
        assertNull(manager.getSessionOwner(session.value()));
    }

    @Test
    void testSessionInvalidation() {
        User user = manager.addUser("***", "ip");
        SessionsManager manager = SessionsManager.create(executor);

        Session firstSession = manager.updateUserSession(user);
        assertEquals(manager.getSessionOwner(firstSession.value()), user);

        Session secondSession = manager.updateUserSession(user);
        assertEquals(manager.getSessionOwner(secondSession.value()), user);

        assertNull(manager.getSessionOwner(firstSession.value()));
    }
}