package ru.justagod.vk.backend.dos;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DosProtectionTest {

    private static MockedStatic<Clock> clockMock;
    private ScheduledExecutorService executor;

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
    public void prepare() {
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
    void testBanProgress() {
        Instant currentTime = Instant.EPOCH;
        mockInstant(currentTime);

        String ip = "ip";
        DosProtection protection = DosProtection.create(executor);

        for (int banTimes = 0; banTimes < 10; banTimes++) {
            for (int i = 0; i < UserState.MAX_REQUESTS; i++) {
                assertFalse(protection.onRequest(ip));
            }
            for (int i = 0; i <= banTimes; i++) {
                assertTrue(protection.onRequest(ip));
                currentTime = currentTime.plus(UserState.BAN_DURATION);
                mockInstant(currentTime);
            }

        }
    }

    @Test
    void testTrackingPeriod() {
        Instant currentTime = Instant.EPOCH;
        mockInstant(currentTime);

        String ip = "ip";
        DosProtection protection = DosProtection.create(executor);

        for (int i = 0; i < UserState.MAX_REQUESTS; i++) {
            assertFalse(protection.onRequest(ip));
        }

        assertTrue(protection.onRequest(ip));

        currentTime = currentTime.plus(UserState.BAN_DURATION);
        mockInstant(currentTime);

        currentTime = currentTime.plus(UserState.TRACKING_DURATION);
        mockInstant(currentTime);

        for (int i = 0; i < UserState.MAX_REQUESTS; i++) {
            assertFalse(protection.onRequest(ip));
        }

        assertTrue(protection.onRequest(ip));

        currentTime = currentTime.plus(UserState.BAN_DURATION);
        mockInstant(currentTime);
        assertFalse(protection.onRequest(ip));


    }

}