package ru.justagod.vk.backend.dos;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

abstract class UserState {

    public static final Duration TRACKING_DURATION = Duration.of(1, ChronoUnit.MINUTES);
    public static final int MAX_REQUESTS = 10;
    public static final Duration BAN_DURATION = Duration.of(3, ChronoUnit.MINUTES);

    static UserState initial() {
        return new Empty();
    }

    private UserState() {}

    @Nullable
    abstract UserState update();

    boolean isBanned() {
        return false;
    }

    UserState requestReceived() {
        return this;
    }

    private static class Empty extends UserState {

        private final Instant lifetimeLimit;
        private final int bansCount;

        Empty() {
            this(Instant.now().plus(TRACKING_DURATION), 0);
        }
        Empty(Instant lifetimeLimit, int bansCount) {
            this.lifetimeLimit = lifetimeLimit;
            this.bansCount = bansCount;
        }

        @Override
        UserState requestReceived() {
            UserState result = new TrackingRequests(bansCount);
            return result.requestReceived();
        }

        @Override
        @Nullable UserState update() {
            if (Instant.now().compareTo(lifetimeLimit) <= 0) return null;
            return this;
        }
    }

    private static class TrackingRequests extends UserState {

        private final int bansCount;
        private final RequestsWindow window = new RequestsWindow(TRACKING_DURATION);

        private Instant lifetimeLimit;

        TrackingRequests() {
            this(0);
        }

        TrackingRequests(int bansCount) {
            this(bansCount, Instant.now().plus(TRACKING_DURATION));
        }
        TrackingRequests(int bansCount, Instant lifetimeLimit) {
            this.bansCount = bansCount;
            this.lifetimeLimit = lifetimeLimit;
        }

        @Override
        @Nullable UserState update() {
            window.update();
            if (Instant.now().compareTo(lifetimeLimit) >= 0)
                return new Empty(lifetimeLimit.plus(TRACKING_DURATION), bansCount).update();
            return this;
        }

        @Override
        UserState requestReceived() {
            this.lifetimeLimit = Instant.now().plus(TRACKING_DURATION);
            window.addRequest();
            window.update();
            if (window.getRequestsCount() > MAX_REQUESTS) {
                return new Banned(bansCount).update();
            }
            return this;
        }
    }

    private static class Banned extends UserState {
        private final Instant until;
        private final int bansCount;

        Banned(int bansCount) {
            until = Instant.now().plus(BAN_DURATION.multipliedBy(bansCount + 1));
            this.bansCount = bansCount;
        }

        @Override
        @Nullable UserState update() {
            if (until.compareTo(Instant.now()) <= 0)
                return new Empty(until.plus(TRACKING_DURATION), bansCount+1).update();
            return this;
        }

        @Override
        boolean isBanned() {
            return true;
        }
    }

}
