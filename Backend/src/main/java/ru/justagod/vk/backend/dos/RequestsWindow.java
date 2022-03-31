package ru.justagod.vk.backend.dos;

import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;

public class RequestsWindow {
    private final Duration trackingDuration;
    private final LinkedList<Instant> requests = new LinkedList<>();

    public RequestsWindow(Duration trackingDuration) {
        this.trackingDuration = trackingDuration;
    }

    public boolean update() {
        Instant now = Instant.now();
        while (!requests.isEmpty() && Duration.between(requests.getFirst(), now).compareTo(trackingDuration) >= 0) {
            requests.removeFirst();
        }

        return requests.isEmpty();
    }

    public int getRequestsCount() {
        update();
        return requests.size();
    }

    public void addRequest() {
        requests.add(Instant.now());
    }


}
