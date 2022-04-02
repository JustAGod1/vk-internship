package ru.justagod.vk.backend.dos;

import org.jetbrains.annotations.Nullable;
import ru.justagod.vk.data.User;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public abstract class UserState {

    public static final Duration TRACKING_DURATION = Duration.of(1, ChronoUnit.MINUTES);
    public static final int MAX_REQUESTS = 10;

    static UserState initial() {
        return new Empty();
    }

    private UserState() {}

    @Nullable
    abstract UserState update();

    @Nullable
    ClientChallenge challenge() {
        return null;
    }

    UserState requestReceived() {
        return this;
    }

    public UserState solveChallenge() {
        return this;
    }

    private static class Empty extends UserState {

        private final Instant lifetimeLimit;

        Empty() {
            this(Instant.now().plus(TRACKING_DURATION));
        }
        Empty(Instant lifetimeLimit) {
            this.lifetimeLimit = lifetimeLimit;
        }

        @Override
        UserState requestReceived() {
            UserState result = new TrackingRequests();
            return result.requestReceived();
        }

        @Override
        @Nullable UserState update() {
            if (Instant.now().compareTo(lifetimeLimit) <= 0) return null;
            return this;
        }
    }

    private static class TrackingRequests extends UserState {

        private final RequestsWindow window = new RequestsWindow(TRACKING_DURATION);

        private Instant lifetimeLimit;

        TrackingRequests() {
            this(Instant.now().plus(TRACKING_DURATION));
        }
        TrackingRequests(Instant lifetimeLimit) {
            this.lifetimeLimit = lifetimeLimit;
        }

        @Override
        @Nullable UserState update() {
            window.update();
            if (Instant.now().compareTo(lifetimeLimit) >= 0)
                return new Empty(lifetimeLimit.plus(TRACKING_DURATION)).update();
            return this;
        }

        @Override
        UserState requestReceived() {
            this.lifetimeLimit = Instant.now().plus(TRACKING_DURATION);
            window.addRequest();
            window.update();
            if (window.getRequestsCount() > MAX_REQUESTS) {
                return new ChallengeRequired().update();
            }
            return this;
        }
    }

    private static class ChallengeRequired extends UserState {
        private final Instant until;

        private final ClientChallenge challenge;

        ChallengeRequired() {
            challenge = ClientChallenge.generate();
            until = Instant.now().plus(TRACKING_DURATION);
        }

        @Override
        @Nullable UserState update() {
            if (until.compareTo(Instant.now()) <= 0)
                return new Empty(until.plus(TRACKING_DURATION)).update();
            return this;
        }

        @Override
        @Nullable ClientChallenge challenge() {
            return challenge;
        }

        @Override
        public UserState solveChallenge() {
            return new Empty(until.plus(TRACKING_DURATION)).update();
        }
    }

}
