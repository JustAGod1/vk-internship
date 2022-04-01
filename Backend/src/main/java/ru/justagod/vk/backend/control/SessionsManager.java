package ru.justagod.vk.backend.control;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.justagod.vk.data.User;
import ru.justagod.vk.data.Session;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SessionsManager {

    // Every user will be forced to resend their login/password every hour. Sounds perfect for me.
    public final static Duration SESSION_LIFETIME = Duration.of(60, ChronoUnit.MINUTES);

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final Map<String, User> session2user = new ConcurrentHashMap<>();
    private final Map<User, String> user2session = new ConcurrentHashMap<>();

    private final Map<String, Instant> session2spoil = new ConcurrentHashMap<>();

    public static SessionsManager create(ScheduledExecutorService executor) {
        return new SessionsManager(executor);
    }

    private SessionsManager(ScheduledExecutorService executor) {
        cleanUpLoop(executor);
    }

    private void cleanUpLoop(ScheduledExecutorService executor) {
        ReentrantReadWriteLock.WriteLock l = lock.writeLock();
        l.lock();
        try {
            Instant now = Instant.now();
            Iterator<Map.Entry<String, Instant>> iterator = session2spoil.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Instant> s2i = iterator.next();
                if (s2i.getValue().compareTo(now) <= 0) {
                    iterator.remove();
                    User user = session2user.remove(s2i.getKey());
                    if (user != null) {
                        user2session.remove(user);
                    }
                }
            }
        } finally {
            l.unlock();
        }
        executor.schedule(() -> cleanUpLoop(executor), 5, TimeUnit.MINUTES);
    }

    @NotNull
    public Session updateUserSession(@NotNull User user) {
        ReentrantReadWriteLock.WriteLock l = lock.writeLock();
        l.lock();
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
    public User getSessionOwner(String session) {
        ReentrantReadWriteLock.ReadLock l = lock.readLock();
        l.lock();
        try {
            Instant until = session2spoil.get(session);
            if (until == null || until.compareTo(Instant.now()) <= 0)
                return null; // It will be cleaned up in the future cleanUpLoop
            return session2user.get(session);
        } finally {
            l.unlock();
        }
    }


}
