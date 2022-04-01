package ru.justagod.vk.backend.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ru.justagod.vk.backend.Main;
import ru.justagod.vk.backend.db.DatabaseManager;
import ru.justagod.vk.data.AuthorizedUserRequest;
import ru.justagod.vk.data.BackendError;
import ru.justagod.vk.data.BackendResponse;
import ru.justagod.vk.data.User;
import ru.justagod.vk.network.Endpoint;

import java.io.IOException;

public class RemoveFriendServlet extends ServletBase<AuthorizedUserRequest, Void> {

    private static final BackendResponse<Void> response = BackendResponse.success(null);

    public RemoveFriendServlet(DatabaseManager database) {
        super(Endpoint.REMOVE_FRIEND_REQUEST_ENDPOINT, database);
    }

    @Override
    protected BackendResponse<Void> handle(HttpServletRequest req, AuthorizedUserRequest request, HttpServletResponse resp) throws IOException {
        if (request.friend() == null || request.session() == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return BackendResponse.badRequest();
        }

        User user = Main.sessions.getSessionOwner(request.session());
        if (user == null) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return BackendResponse.forbidden();
        }

        if (database.removeFriend(user, request.friend())) return response;
        else {
            resp.setStatus(409); // Conflict
            return BackendResponse.error(BackendError.FRIEND_ALREADY_REMOVED);
        }
    }
}