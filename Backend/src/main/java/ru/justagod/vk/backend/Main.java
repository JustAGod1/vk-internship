package ru.justagod.vk.backend;

import com.google.gson.Gson;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import ru.justagod.vk.backend.control.SessionsManager;
import ru.justagod.vk.backend.db.DatabaseManager;
import ru.justagod.vk.backend.dos.DosProtection;
import ru.justagod.vk.backend.poll.LongPollServer;
import ru.justagod.vk.backend.servlet.*;
import ru.justagod.vk.data.GsonHolder;
import ru.justagod.vk.network.Endpoint;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static ru.justagod.vk.EnvHelper.intEnv;


public class Main {
    public static final Gson gson = GsonHolder.gson;
    public static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(3);
    private static final DatabaseManager database = DatabaseManager.create("database.sqlite");
    private static final SessionsManager sessions = SessionsManager.create(executor);
    private static final DosProtection protection = DosProtection.create(executor);
    public static final LongPollServer longPoll = LongPollServer.start(executor, gson, sessions, longPollPort());

    public static void main(String[] args) throws Exception {
        makeServer().start();
    }

    public static int serverPort() {
        return intEnv("ru.justagod.vk.server.port", 8888);
    }
    public static int longPollPort() {
        return intEnv("ru.justagod.vk.server.poll.port", 9999);
    }
    private static Server makeServer() {
        Server server = new Server(new ExecutorThreadPool(5));
        ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory());

        connector.setPort(serverPort());

        server.addConnector(connector);

        ServletContextHandler handler = new ServletContextHandler();
        server.setHandler(handler);

        List<ServletBase<?, ?>> servlets = List.of(
                new SignUpServlet(database, protection, sessions),
                new SignInServlet(database, protection, sessions),
                new FriendsListServlet(database, protection, sessions),
                new UsersServlet(database, protection, sessions),
                new AddFriendServlet(database, protection, sessions),
                new RemoveFriendServlet(database, protection, sessions),
                new ChallengeServlet(database, protection, sessions),
                new GetMessagesServlet(database, protection, sessions),
                new SendMessageServlet(database, protection, sessions)
        );
        for (ServletBase<?, ?> servlet : servlets) {
            handler.addServlet(new ServletHolder(servlet), "/" + servlet.getEndpoint().name);
        }

        return server;
    }
}
