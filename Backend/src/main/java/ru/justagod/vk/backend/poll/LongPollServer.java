package ru.justagod.vk.backend.poll;

import com.google.gson.Gson;
import ru.justagod.vk.backend.control.SessionsManager;
import ru.justagod.vk.data.BackendError;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class LongPollServer {

    private List<LongPollServerConnection> connections = new ArrayList<>();

    private final Selector selector = createServerSelector();
    private final ScheduledExecutorService executor;
    private final Gson gson;
    private final SessionsManager sessions;
    private final int port;

    private LongPollServer(ScheduledExecutorService executor,
                           Gson gson,
                           SessionsManager sessions,
                           int port) throws IOException {
        this.executor = executor;
        this.gson = gson;
        this.sessions = sessions;
        this.port = port;
    }

    private void loop() {
        try {
            selector.select();
            for (SelectionKey key : selector.selectedKeys()) {
                if (!key.isValid()) continue;
                if (key.isAcceptable()) {
                    accept(key);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<LongPollServerConnection> newList = Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < connections.size(); i++) {
            final int idx = i;
            executor.submit(() -> {
                LongPollServerConnection connection = connections.get(idx);
                try {
                    if (connection.loop()) {
                        newList.add(connection);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    connection.closeChannel(new BackendError(BackendError.GENERIC_ERROR, e.getMessage()));
                }
            });
        }

        connections = newList;

        executor.schedule(this::loop, 10, TimeUnit.SECONDS);
    }

    private void accept(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        try {
            LongPollServerConnection connection = new LongPollServerConnection(
                    gson,
                    channel,
                    new LongPollAuthorizationHandler(gson, sessions)
            );
            connections.add(connection);
        } catch (IOException e) {
            e.printStackTrace();
            try {
                channel.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private Selector createServerSelector() throws IOException {
        Selector selector = Selector.open();

        ServerSocketChannel server = ServerSocketChannel.open();
        server.configureBlocking(false);
        server.register(selector, SelectionKey.OP_ACCEPT);

        server.bind(new InetSocketAddress(port));

        return selector;
    }

}
