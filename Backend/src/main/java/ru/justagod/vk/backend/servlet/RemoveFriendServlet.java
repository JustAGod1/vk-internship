package ru.justagod.vk.backend.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ru.justagod.vk.backend.control.SessionsManager;
import ru.justagod.vk.backend.db.DatabaseManager;
import ru.justagod.vk.backend.dos.DosProtection;
import ru.justagod.vk.data.AuthorizedUserRequest;
import ru.justagod.vk.data.BackendError;
import ru.justagod.vk.data.BackendResponse;
import ru.justagod.vk.data.User;
import ru.justagod.vk.network.Endpoint;

import java.io.IOException;

public class RemoveFriendServlet extends AuthorizedServlet<AuthorizedUserRequest, Void> {

    private static final BackendResponse<Void> response = BackendResponse.success(null);

    public RemoveFriendServlet(DatabaseManager database, DosProtection protection, SessionsManager sessions) {
        super(Endpoint.REMOVE_FRIEND_REQUEST_ENDPOINT, database, protection, sessions);
    }

    @Override
    protected BackendResponse<Void> handle(HttpServletRequest req, User user, AuthorizedUserRequest request, HttpServletResponse resp) {
        if (request.user() == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return BackendResponse.badRequest();
        }

        if (database.removeFriend(user, request.user())) return response;
        else {
            resp.setStatus(409); // Conflict
            return BackendResponse.error(BackendError.FRIEND_ALREADY_REMOVED);
        }
    }
}
