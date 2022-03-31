package ru.justagod.vk.backend.servlet;

import com.google.gson.JsonSyntaxException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ru.justagod.vk.backend.Main;
import ru.justagod.vk.backend.db.DatabaseManager;
import ru.justagod.vk.backend.dos.RequestsWindow;
import ru.justagod.vk.data.BackendError;
import ru.justagod.vk.data.BackendResponse;
import ru.justagod.vk.network.Endpoint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public abstract class ServletBase<Request, Response> extends HttpServlet {

    private final ConcurrentHashMap<String, RequestsWindow> requestsTracker = new ConcurrentHashMap<>();
    private static final int PAYLOAD_LIMIT = 10 * 1024;

    private final Endpoint<Request, Response> endpoint;
    protected final DatabaseManager database;

    public ServletBase(Endpoint<Request, Response> endpoint, DatabaseManager database) {
        this.endpoint = endpoint;
        this.database = database;
        cleanUpLoop();
    }

    private synchronized void cleanUpLoop() {
        requestsTracker.values().removeIf(RequestsWindow::update);
        Main.executor.schedule(this::cleanUpLoop, 1, TimeUnit.MINUTES);
    }

    @Override
    protected synchronized final void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String ip = req.getRemoteAddr();
            if (Main.protection.onRequest(ip)) {
                resp.setStatus(429); // Too many requests
                return;
            }

            byte[] content = req.getInputStream().readNBytes(PAYLOAD_LIMIT);
            if (!req.getInputStream().isFinished()) {
                resp.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
                return;
            }

            String contentString = new String(content, StandardCharsets.UTF_8);
            Request request;
            try {
                request = endpoint.parseRequest(Main.gson, contentString);
            } catch (Endpoint.ParsingException e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            resp.setStatus(HttpServletResponse.SC_OK);
            BackendResponse<Response> response = handle(req, request, resp);

            resp.getOutputStream().write(endpoint.writeResponse(Main.gson, response).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(500);
            resp.getOutputStream().write(
                    endpoint.writeResponse(
                                    Main.gson,
                                    BackendResponse.error(new BackendError(BackendError.GENERIC_ERROR, e.toString()))
                            ).getBytes(StandardCharsets.UTF_8)
            );

        }
    }

    protected abstract BackendResponse<Response> handle(HttpServletRequest req, Request request, HttpServletResponse resp) throws IOException;

}
