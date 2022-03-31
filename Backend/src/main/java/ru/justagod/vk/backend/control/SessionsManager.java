package ru.justagod.vk.backend.control;

import org.jetbrains.annotations.Nullable;
import ru.justagod.vk.backend.model.User;
import ru.justagod.vk.data.Session;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SessionsManager {

    // Every user will be forced to resend his login/password every hour. Sounds perfect as for me.
    private static final Duration SESSION_LIFETIME = Duration.of(60, ChronoUnit.MINUTES);

    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private static final Map<String, User> session2user = new ConcurrentHashMap<>();
    private static final Map<User, String> user2session = new ConcurrentHashMap<>();

    private static final Map<String, Instant> session2spoil = new ConcurrentHashMap<>();

    public static Session updateUserSession(User user) {
        ReentrantReadWriteLock.WriteLock l = lock.writeLock();
        try {
            Session s = Session.random(Instant.now().plus(SESSION_LIFETIME));

            String before = user2session.remove(user);
            if (before != null) {
                session2user.remove(before);
                session2spoil.remove(before);
            }

            session2user.put(s.value(), user);
            user2session.put(user, s.value());
            session2spoil.put(s.value(), s.validUntil());

            return s;
        } finally {
            l.unlock();
        }
    }


    @Nullable
    public static User getSessionOwner(String session) {
        ReentrantReadWriteLock.ReadLock l = lock.readLock();
        try {

        } finally {
            l.unlock();
        }
    }

}
