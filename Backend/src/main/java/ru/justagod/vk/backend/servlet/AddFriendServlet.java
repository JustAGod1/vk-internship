package ru.justagod.vk.backend.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ru.justagod.vk.backend.Main;
import ru.justagod.vk.backend.control.SessionsManager;
import ru.justagod.vk.backend.db.DatabaseManager;
import ru.justagod.vk.backend.dos.DosProtection;
import ru.justagod.vk.data.AuthorizedUserRequest;
import ru.justagod.vk.data.BackendError;
import ru.justagod.vk.data.BackendResponse;
import ru.justagod.vk.data.User;
import ru.justagod.vk.network.Endpoint;

import java.io.IOException;

public class AddFriendServlet extends ServletBase<AuthorizedUserRequest, Void> {

    private static final BackendResponse<Void> response = BackendResponse.success(null);

    public AddFriendServlet(DatabaseManager database, DosProtection protection, SessionsManager sessions) {
        super(Endpoint.ADD_FRIEND_REQUEST_ENDPOINT, database, protection, sessions);
    }

    @Override
    protected BackendResponse<Void> handle(HttpServletRequest req, AuthorizedUserRequest request, HttpServletResponse resp) throws IOException {
        if (request.friend() == null || request.session() == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return BackendResponse.badRequest();
        }

        User user = sessions.getSessionOwner(request.session());
        if (user == null) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return BackendResponse.forbidden();
        }

        if (database.addFriend(user, request.friend())) return response;
        else {
            resp.setStatus(409); // Conflict
            return BackendResponse.error(BackendError.FRIEND_ALREADY_ADDED);
        }
    }
}
