package ru.justagod.vk.backend.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ru.justagod.vk.backend.Main;
import ru.justagod.vk.backend.db.DatabaseManager;
import ru.justagod.vk.backend.dos.ClientChallenge;
import ru.justagod.vk.data.BackendError;
import ru.justagod.vk.data.BackendResponse;
import ru.justagod.vk.network.Endpoint;

import java.io.IOException;

public class ChallengeServlet extends ServletBase<Integer, Void> {


    public ChallengeServlet(DatabaseManager database) {
        super(Endpoint.SOLVE_CHALLENGE_REQUEST_ENDPOINT, database, true);
    }

    @Override
    protected BackendResponse<Void> handle(HttpServletRequest req, Integer answer, HttpServletResponse resp) throws IOException {
        if (answer == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return BackendResponse.badRequest();
        }

        ClientChallenge challenge = Main.protection.onRequest(req.getRemoteAddr());
        if (challenge == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return BackendResponse.error(BackendError.CHALLENGE_IS_NOT_REQUIRED);
        }

        if (challenge.getAnswer() != answer) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return BackendResponse.error(BackendError.WRONG_CHALLENGE_ANSWER);
        }

        Main.protection.solveChallenge(req.getRemoteAddr());

        return BackendResponse.success(null);

    }
}