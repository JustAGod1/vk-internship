package ru.justagod.vk.backend.poll;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import ru.justagod.vk.backend.control.SessionsManager;
import ru.justagod.vk.data.BackendError;
import ru.justagod.vk.data.BackendResponse;
import ru.justagod.vk.data.User;

import java.io.IOException;
import java.security.SecureRandom;

public class LongPollAuthorizationHandler extends LongPollConnectionHandlerBase<String, String>{

    private static final ThreadLocal<SecureRandom> random = ThreadLocal.withInitial(SecureRandom::new);

    private enum State {
        INITIAL, COOKIE_SENT, COOKIE_RECEIVED
    }

    private State state = State.INITIAL;
    private final SessionsManager sessions;
    private final String cookie = createCookie();

    protected LongPollAuthorizationHandler(Gson gson, SessionsManager sessions) {
        super(gson, String.class);
        this.sessions = sessions;
    }

    private String createCookie() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 10; i++) {
            sb.append((char) ('a' + random.get().nextInt('z' - 'a')));
        }

        return sb.toString();
    }

    @Override
    public void connection(@NotNull LongPollServerConnection connection) throws IOException {
        super.connection(connection);
        state = State.COOKIE_SENT;
        connection.sendMessage(BackendResponse.success(cookie));
    }


    private void handleCookie(String s) throws IOException {
        if (!s.equals(cookie)) {
            connection.closeChannel(BackendError.BAD_COOKIE);
            return;
        }
        state = State.COOKIE_RECEIVED;
    }

    private void handleSession(String s) throws IOException {
        User user = sessions.getSessionOwner(s);
        if (user == null) {
            connection.closeChannel(BackendResponse.forbidden().error());
            return;
        }

        connection.setHandler(new LongPollCommunicationHandler(gson, user));

    }

    @Override
    protected String handleRequest(String s) throws IOException {
        switch (state) {
            case COOKIE_SENT -> handleCookie(s);
            case COOKIE_RECEIVED -> handleSession(s);
            default -> connection.closeChannel(BackendError.PROTOCOL_ERROR);
        }


        return null;
    }
}
