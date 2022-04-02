package ru.justagod.vk.backend.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ru.justagod.vk.backend.control.SessionsManager;
import ru.justagod.vk.backend.db.DatabaseManager;
import ru.justagod.vk.backend.dos.DosProtection;
import ru.justagod.vk.backend.poll.LongPollCommunicationHandler;
import ru.justagod.vk.data.*;
import ru.justagod.vk.network.Endpoint;

import java.time.Instant;

public class SendMessageServlet extends AuthorizedServlet<SendMessageRequest, Void> {
    public SendMessageServlet(DatabaseManager database, DosProtection protection, SessionsManager sessions) {
        super(Endpoint.SEND_MESSAGE_REQUEST_ENDPOINT, database, protection, sessions);
    }

    @Override
    protected BackendResponse<Void> handle(HttpServletRequest req, User user, SendMessageRequest request, HttpServletResponse resp) {
        if (request.payload() == null || request.recipient() == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return BackendResponse.badRequest();
        }

        Message message = Message.create(
                user, request.recipient(), Instant.now(), request.payload()
        );
        database.addMessage(message);

        dispatch(LongPollCommunicationHandler.getConnection(user),message);
        dispatch(LongPollCommunicationHandler.getConnection(request.recipient()),message);

        return null;
    }

    private void dispatch(LongPollCommunicationHandler connection, Message message) {
        if (connection == null) return;

        connection.addEvent(message);
    }
}
