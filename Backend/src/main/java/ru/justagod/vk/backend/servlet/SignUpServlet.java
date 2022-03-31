package ru.justagod.vk.backend.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ru.justagod.vk.backend.Main;
import ru.justagod.vk.backend.db.PasswordsManager;
import ru.justagod.vk.backend.model.User;
import ru.justagod.vk.data.SignUpRequest;

public class SignUpServlet extends ServletBase<SignUpRequest> {

    public SignUpServlet() {
        super(SignUpRequest.class);
    }


    @Override
    protected void handle(HttpServletRequest req, SignUpRequest request, HttpServletResponse resp) {
        if (request.firstname() == null || request.surname() == null ||
                request.username() == null || request.password() == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        if (Main.database.findUser(request.username()) != null) {
            resp.setStatus(409); // Conflict
            return;
        }

        Main.database.addUser(
                request.firstname(), request.surname(),
                PasswordsManager.hashed(request.password()),
                request.username()
        );
    }
}
