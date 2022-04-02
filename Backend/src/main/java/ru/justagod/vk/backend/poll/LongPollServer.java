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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LongPollServer {

    private List<LongPollServerConnection> connections = new ArrayList<>();

    private final ServerSocketChannel server = createServer();
    private final Selector selector = createServerSelector(server);
    private final ScheduledExecutorService executor;
    private final Gson gson;
    private final SessionsManager sessions;

    private LongPollServer(ScheduledExecutorService executor,
                           Gson gson,
                           SessionsManager sessions,
                           int port) throws IOException {
        this.executor = executor;
        this.gson = gson;
        this.sessions = sessions;
        server.bind(new InetSocketAddress(port));
    }

    public static LongPollServer start(
            ScheduledExecutorService executor,
            Gson gson,
            SessionsManager sessions,
            int port
    ) {
        try {
            LongPollServer server = new LongPollServer(executor, gson, sessions, port);
            executor.submit(server::loop);
            return server;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void loop() {
        try {
            selector.selectNow();
            for (SelectionKey key : selector.selectedKeys()) {
                if (!key.isValid()) continue;
                if (key.isAcceptable()) {
                    accept();
                }
            }
            selector.selectedKeys().clear();
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<LongPollServerConnection> newList = Collections.synchronizedList(new ArrayList<>());
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < connections.size(); i++) {
            final int idx = i;
            Future<?> future = executor.submit(() -> {
                LongPollServerConnection connection = connections.get(idx);
                try {
                    if (connection.loop()) {
                        newList.add(connection);
                    } else {
                        connection.handler.deregister();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    connection.closeChannel(new BackendError(BackendError.GENERIC_ERROR, e.getMessage()));
                    connection.handler.deregister();
                }
            });
            futures.add(future);
        }
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        connections = newList;

        executor.schedule(this::loop, 10, TimeUnit.MILLISECONDS);
    }

    private void accept() throws IOException {
        SocketChannel channel = server.accept();
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

    private ServerSocketChannel createServer() throws IOException {
        ServerSocketChannel server = ServerSocketChannel.open();
        server.configureBlocking(false);

        return server;
    }

    private Selector createServerSelector(ServerSocketChannel server) throws IOException {
        Selector selector = Selector.open();
        server.register(selector, SelectionKey.OP_ACCEPT);

        return selector;
    }

    public int getPort() {
        return server.socket().getLocalPort();
    }

    public void stop() throws IOException {
        server.close();
    }
}
