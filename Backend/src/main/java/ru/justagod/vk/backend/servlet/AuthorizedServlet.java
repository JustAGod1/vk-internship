package ru.justagod.vk.backend.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ru.justagod.vk.backend.control.SessionsManager;
import ru.justagod.vk.backend.db.DatabaseManager;
import ru.justagod.vk.backend.dos.DosProtection;
import ru.justagod.vk.data.AuthorizedRequest;
import ru.justagod.vk.data.BackendResponse;
import ru.justagod.vk.data.User;
import ru.justagod.vk.network.Endpoint;

import java.io.IOException;

public abstract class AuthorizedServlet<Request extends AuthorizedRequest, Response> extends ServletBase<Request, Response> {

    public AuthorizedServlet(Endpoint<Request, Response> endpoint, DatabaseManager database, DosProtection protection, SessionsManager sessions) {
        super(endpoint, database, protection, sessions);
    }

    public AuthorizedServlet(Endpoint<Request, Response> endpoint, DatabaseManager database, DosProtection protection, SessionsManager sessions, boolean skipProtection) {
        super(endpoint, database, protection, sessions, skipProtection);
    }

    @Override
    protected final BackendResponse<Response> handle(HttpServletRequest req, Request request, HttpServletResponse resp) throws IOException {
        if (request.session() == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return BackendResponse.badRequest();
        }
        User user = sessions.getSessionOwner(request.session());
        if (user == null) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return BackendResponse.forbidden();
        }
        return handle(req, user, request, resp);
    }

    protected abstract BackendResponse<Response> handle(HttpServletRequest req,
                                                        User user,
                                                        Request request,
                                                        HttpServletResponse resp);
}
