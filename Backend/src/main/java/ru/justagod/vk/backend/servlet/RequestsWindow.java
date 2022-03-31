package ru.justagod.vk.backend.servlet;

import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;

public class RequestsWindow {
    private final Duration trackingDuration;
    private final LinkedList<Instant> requests = new LinkedList<>();
    private final int maxRequests;
    private final Duration banTime;
    @Nullable
    private Instant bannedUntil = null;

    public RequestsWindow(Duration trackingDuration, int maxRequests, Duration banTime) {
        this.trackingDuration = trackingDuration;
        this.maxRequests = maxRequests;
        this.banTime = banTime;
    }

    public boolean update() {
        if (!isBanned()) bannedUntil = null;
        Instant now = Instant.now();
        while (!requests.isEmpty() && Duration.between(requests.getFirst(), now).compareTo(trackingDuration) <= 0) {
            requests.removeFirst();
        }

        return requests.isEmpty() && !isBanned();
    }

    public boolean isBanned() {
        return bannedUntil != null && bannedUntil.compareTo(Instant.now()) > 0;
    }

    public int getRequestsCount() {
        update();
        return requests.size();
    }

    public void addRequest() {
        update();
        if (isBanned()) return;
        requests.add(Instant.now());
        if (requests.size() > maxRequests) {
            bannedUntil = Instant.now().plus(banTime);
        }
    }


}
