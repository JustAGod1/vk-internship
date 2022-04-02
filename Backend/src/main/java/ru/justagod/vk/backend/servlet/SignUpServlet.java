package ru.justagod.vk.backend.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ru.justagod.vk.backend.Main;
import ru.justagod.vk.backend.control.SessionsManager;
import ru.justagod.vk.backend.db.DatabaseManager;
import ru.justagod.vk.backend.db.PasswordsManager;
import ru.justagod.vk.backend.dos.DosProtection;
import ru.justagod.vk.data.User;
import ru.justagod.vk.data.*;
import ru.justagod.vk.network.Endpoint;

import java.io.IOException;

public class SignUpServlet extends ServletBase<UserPasswordRequest, SessionResponse> {

    private static final BackendResponse<SessionResponse> ALREADY_EXISTS
            = BackendResponse.error(BackendError.USERNAME_ALREADY_EXISTS);

    public SignUpServlet(DatabaseManager database, DosProtection protection, SessionsManager sessions) {
        super(Endpoint.SIGN_UP_REQUEST_ENDPOINT, database, protection, sessions);
    }


    @Override
    protected BackendResponse<SessionResponse> handle(HttpServletRequest req, UserPasswordRequest request, HttpServletResponse resp) throws IOException {
        if (request.username() == null || request.password() == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return BackendResponse.badRequest();
        }
        if (database.findUser(request.username()) != null) {
            resp.setStatus(409); // Conflict
            return ALREADY_EXISTS;
        }

        User user = database.addUser(
                PasswordsManager.hashed(request.password()),
                request.username()
        );

        Session session = sessions.updateUserSession(user);

        return BackendResponse.success(new SessionResponse(session, user));
    }
}
