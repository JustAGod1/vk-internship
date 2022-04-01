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
import ru.justagod.vk.backend.servlet.*;
import ru.justagod.vk.data.GsonHolder;
import ru.justagod.vk.network.Endpoint;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Main {
    public static final Gson gson = GsonHolder.gson;
    public static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(3);
    public static final DatabaseManager database = DatabaseManager.create("database.sqlite");
    public static final SessionsManager sessions = SessionsManager.create(executor);
    public static final DosProtection protection = DosProtection.create(executor);

    public static void main(String[] args) throws Exception {
        makeServer().start();
    }

    private static int serverPort() {
        int result = 8888;
        String override = System.getenv("ru.justagod.vk.client.server_url");
        if (override != null) {
            try {
                result = Integer.parseInt(override);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }
    private static Server makeServer() {
        Server server = new Server(new ExecutorThreadPool(5));
        ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory());

        connector.setPort(serverPort());

        server.addConnector(connector);

        ServletContextHandler handler = new ServletContextHandler();
        server.setHandler(handler);

        List<ServletBase<?, ?>> servlets = List.of(
                new SignUpServlet(database),
                new SignInServlet(database),
                new FriendsListServlet(database),
                new UsersServlet(database),
                new AddFriendServlet(database),
                new RemoveFriendServlet(database),
                new ChallengeServlet(database)
        );
        for (ServletBase<?, ?> servlet : servlets) {
            handler.addServlet(new ServletHolder(servlet), "/" + servlet.getEndpoint().name);
        }

        return server;
    }
}
