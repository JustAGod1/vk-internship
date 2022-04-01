package ru.justagod.vk.backend.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ru.justagod.vk.backend.Main;
import ru.justagod.vk.backend.db.DatabaseManager;
import ru.justagod.vk.backend.db.PasswordsManager;
import ru.justagod.vk.data.*;
import ru.justagod.vk.network.Endpoint;

import java.io.IOException;

public class SignInServlet extends ServletBase<UserPasswordRequest, SessionResponse> {
    public SignInServlet(DatabaseManager database) {
        super(Endpoint.SIGN_IN_REQUEST_ENDPOINT, database);
    }

    @Override
    protected BackendResponse<SessionResponse> handle(HttpServletRequest req, UserPasswordRequest request, HttpServletResponse resp) throws IOException {
        if (request.username() == null || request.password() == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return BackendResponse.badRequest();
        }
        User user = database.findUser(request.username(), PasswordsManager.hashed(request.password()));
        if (user == null) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return BackendResponse.error(BackendError.WRONG_USERNAME_OR_PASSWORD);
        }

        return BackendResponse.success(new SessionResponse(Main.sessions.updateUserSession(user), user));
    }
}
