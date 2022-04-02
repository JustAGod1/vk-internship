package ru.justagod.vk.backend.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ru.justagod.vk.backend.control.SessionsManager;
import ru.justagod.vk.backend.db.DatabaseManager;
import ru.justagod.vk.backend.dos.DosProtection;
import ru.justagod.vk.data.*;
import ru.justagod.vk.network.Endpoint;

import java.io.IOException;
import java.util.List;

public class GetMessagesServlet extends AuthorizedServlet<MessagesRequest, Messages>{

    public GetMessagesServlet(DatabaseManager database, DosProtection protection, SessionsManager sessions) {
        super(Endpoint.GET_MESSAGES_REQUEST_ENDPOINT, database, protection, sessions);
    }

    @Override
    protected BackendResponse<Messages> handle(HttpServletRequest req, User user, MessagesRequest request, HttpServletResponse resp) {
        if (request.user() == null || request.before() == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return BackendResponse.badRequest();
        }

        List<Message> result = database.readMessages(request.before(), request.user(), user);

        return BackendResponse.success(new Messages(result));
    }
}
