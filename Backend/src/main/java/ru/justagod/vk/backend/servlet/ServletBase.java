package ru.justagod.vk.backend.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ru.justagod.vk.backend.Main;
import ru.justagod.vk.backend.control.SessionsManager;
import ru.justagod.vk.backend.db.DatabaseManager;
import ru.justagod.vk.backend.dos.ClientChallenge;
import ru.justagod.vk.backend.dos.DosProtection;
import ru.justagod.vk.data.BackendError;
import ru.justagod.vk.data.BackendResponse;
import ru.justagod.vk.network.Endpoint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public abstract class ServletBase<Request, Response> extends HttpServlet {

    private static final int PAYLOAD_LIMIT = 10 * 1024;

    private final Endpoint<Request, Response> endpoint;
    protected final DatabaseManager database;
    protected final DosProtection protection;
    protected final SessionsManager sessions;
    private final boolean skipProtection;

    public ServletBase(Endpoint<Request, Response> endpoint, DatabaseManager database, DosProtection protection, SessionsManager sessions) {
        this(endpoint, database, protection, sessions, false);
    }

    public ServletBase(Endpoint<Request, Response> endpoint, DatabaseManager database, DosProtection protection, SessionsManager sessions, boolean skipProtection) {
        this.endpoint = endpoint;
        this.database = database;
        this.protection = protection;
        this.sessions = sessions;
        this.skipProtection = skipProtection;
    }

    public Endpoint<Request, Response> getEndpoint() {
        return endpoint;
    }

    @Override
    protected synchronized final void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            if (!skipProtection) {
                String ip = req.getRemoteAddr();
                ClientChallenge challenge = protection.onRequest(ip);
                if (challenge != null) {
                    resp.setStatus(429); // Too many requests
                    BackendResponse<Response> response = BackendResponse.error(
                            new BackendError(BackendError.CHALLENGE_REQUIRED, challenge.getChallenge())
                    );
                    resp.getOutputStream().write(endpoint.writeResponse(Main.gson, response).getBytes(StandardCharsets.UTF_8));
                    return;
                }
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
                sendBadRequest(resp);
                return;
            }
            if (!verifyRequest(request)) {
                sendBadRequest(resp);
                return;
            }
            resp.setStatus(HttpServletResponse.SC_OK);
            BackendResponse<Response> response = handle(req, request, resp);

            resp.getOutputStream().write(endpoint.writeResponse(Main.gson, response).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(500);
            resp.getOutputStream().write(endpoint.writeResponse(
                    Main.gson,
                    BackendResponse.error(new BackendError(BackendError.GENERIC_ERROR, e.toString()))
            ).getBytes(StandardCharsets.UTF_8));

        }
    }

    private void sendBadRequest(HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        resp.getOutputStream().write(endpoint.writeResponse(
                Main.gson,
                BackendResponse.badRequest()
        ).getBytes(StandardCharsets.UTF_8));
    }

    protected boolean verifyRequest(Request request) {
        return true;
    }

    protected abstract BackendResponse<Response> handle(HttpServletRequest req, Request request, HttpServletResponse resp) throws IOException;

}
