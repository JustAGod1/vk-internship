package ru.justagod.vk.backend.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ru.justagod.vk.backend.Main;
import ru.justagod.vk.backend.control.SessionsManager;
import ru.justagod.vk.backend.db.DatabaseManager;
import ru.justagod.vk.backend.dos.DosProtection;
import ru.justagod.vk.data.AuthorizedRequest;
import ru.justagod.vk.data.BackendResponse;
import ru.justagod.vk.data.User;
import ru.justagod.vk.data.UsersListResponse;
import ru.justagod.vk.network.Endpoint;

import java.io.IOException;

public class UsersServlet extends ServletBase<AuthorizedRequest, UsersListResponse> {
    public UsersServlet(DatabaseManager database, DosProtection protection, SessionsManager sessions) {
        super(Endpoint.USERS_REQUEST_ENDPOINT, database, protection, sessions);
    }

    @Override
    protected BackendResponse<UsersListResponse> handle(HttpServletRequest req, AuthorizedRequest request, HttpServletResponse resp) throws IOException {
        if (request.session() == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return BackendResponse.badRequest();
        }
        User user = sessions.getSessionOwner(request.session());
        if (user == null) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return BackendResponse.forbidden();
        }

        return BackendResponse.success(new UsersListResponse(database.getUsers()));
    }
}
