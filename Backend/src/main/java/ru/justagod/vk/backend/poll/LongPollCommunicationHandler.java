package ru.justagod.vk.backend.poll;

import com.google.gson.Gson;
import org.jetbrains.annotations.Nullable;
import ru.justagod.vk.data.BackendError;
import ru.justagod.vk.data.Message;
import ru.justagod.vk.data.Messages;
import ru.justagod.vk.data.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class LongPollCommunicationHandler extends LongPollConnectionHandlerBase<Void, Messages> {

    private static final Map<User, LongPollCommunicationHandler> connectedUsers = new ConcurrentHashMap<>();

    private static final int MAX_QUEUE_SIZE = 100;

    private final ConcurrentLinkedDeque<Message> events = new ConcurrentLinkedDeque<>();

    @Nullable
    public static LongPollCommunicationHandler getConnection(User user) {
        return connectedUsers.get(user);
    }

    protected LongPollCommunicationHandler(Gson gson, User user) {
        super(gson, Void.class);
        LongPollCommunicationHandler before = connectedUsers.put(user, this);
        if (before != null) {
            before.connection.closeChannel(BackendError.CONNECTED_FROM_ANOTHER_LOCATION);
        }
    }

    public void addEvent(Message message) {
        try {
            if (events.size() > MAX_QUEUE_SIZE) {
                connection.closeChannel(BackendError.QUEUE_OVERFLOW);
            }
            events.add(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deregister() {
        connectedUsers.values().remove(this);
    }

    @Override
    protected Messages handleRequest(Void unused) throws IOException {
        int l = events.size();
        List<Message> result = new ArrayList<>();

        for (int i = 0; i < l; i++) {
            Message message = events.poll();
            result.add(message);
        }

        return new Messages(result);
    }
}
