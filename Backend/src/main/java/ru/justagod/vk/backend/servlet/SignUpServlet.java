package ru.justagod.vk.backend.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ru.justagod.vk.backend.Main;
import ru.justagod.vk.backend.db.DatabaseManager;
import ru.justagod.vk.backend.db.PasswordsManager;
import ru.justagod.vk.backend.model.User;
import ru.justagod.vk.data.*;
import ru.justagod.vk.network.Endpoint;

import java.io.IOException;

public class SignUpServlet extends ServletBase<SignUpRequest, Session> {

    private static final BackendResponse<Session> ALREADY_EXISTS
            = BackendResponse.error(BackendError.USERNAME_ALREADY_EXISTS);

    public SignUpServlet(DatabaseManager database) {
        super(Endpoint.SIGN_UP_REQUEST_ENDPOINT, database);
    }


    @Override
    protected BackendResponse<Session> handle(HttpServletRequest req, SignUpRequest request, HttpServletResponse resp) throws IOException {
        if (request.username() == null || request.password() == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return BackendResponse.badRequest();
        }
        if (Main.database.findUser(request.username()) != null) {
            resp.setStatus(409); // Conflict
            return ALREADY_EXISTS;
        }

        User user = Main.database.addUser(
                PasswordsManager.hashed(request.password()),
                request.username()
        );

        Session session = Main.sessions.updateUserSession(user);

        return BackendResponse.success(session);
    }
}
