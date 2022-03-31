package ru.justagod.vk.backend.servlet;

import com.google.gson.JsonSyntaxException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ru.justagod.vk.backend.Main;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public abstract class ServletBase<T> extends HttpServlet {

    private final ConcurrentHashMap<String, RequestsWindow> requestsTracker = new ConcurrentHashMap<>();
    private static final int PAYLOAD_LIMIT = 10 * 1024;
    private static final Duration TRACKING_DURATION = Duration.of(1, ChronoUnit.MINUTES);
    private static final int MAX_REQUESTS = 10;
    private static final Duration BAN_DURATION = Duration.of(3, ChronoUnit.MINUTES);

    private final Class<T> requestClass;

    public ServletBase(Class<T> requestClass) {
        this.requestClass = requestClass;
        cleanUpLoop();
    }

    private synchronized void cleanUpLoop() {
        requestsTracker.values().removeIf(RequestsWindow::update);
        Main.executor.schedule(this::cleanUpLoop, 1, TimeUnit.MINUTES);
    }

    @Override
    protected synchronized final void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String ip = req.getRemoteAddr();
        RequestsWindow window = requestsTracker.computeIfAbsent(ip, (a) -> new RequestsWindow(TRACKING_DURATION, MAX_REQUESTS, BAN_DURATION));
        window.addRequest();
        if (window.isBanned()) {
            resp.setStatus(429); // Too many requests
            return;
        }

        byte[] content = req.getInputStream().readNBytes(PAYLOAD_LIMIT);
        if (!req.getInputStream().isFinished()) {
            resp.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            return;
        }

        String contentString = new String(content, StandardCharsets.UTF_8);
        T request;
        try {
            request = Main.gson.fromJson(contentString, requestClass);
        } catch (JsonSyntaxException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        handle(req, request, resp);

    }

    protected abstract void handle(HttpServletRequest req, T request, HttpServletResponse resp);

}
