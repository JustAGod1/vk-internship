package ru.justagod.vk.backend.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ru.justagod.vk.backend.Main;
import ru.justagod.vk.backend.db.DatabaseManager;
import ru.justagod.vk.data.AuthorizedRequest;
import ru.justagod.vk.data.BackendResponse;
import ru.justagod.vk.data.User;
import ru.justagod.vk.data.UsersListResponse;
import ru.justagod.vk.network.Endpoint;

import java.io.IOException;

public class UsersServlet extends ServletBase<AuthorizedRequest, UsersListResponse> {
    public UsersServlet(DatabaseManager database) {
        super(Endpoint.USERS_REQUEST_ENDPOINT, database);
    }

    @Override
    protected BackendResponse<UsersListResponse> handle(HttpServletRequest req, AuthorizedRequest request, HttpServletResponse resp) throws IOException {
        User user = Main.sessions.getSessionOwner(request.session());
        if (user == null) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return BackendResponse.forbidden();
        }

        return BackendResponse.success(new UsersListResponse(database.getUsers()));
    }
}
