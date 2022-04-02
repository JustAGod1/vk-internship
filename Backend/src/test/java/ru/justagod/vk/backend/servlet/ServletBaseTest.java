package ru.justagod.vk.backend.servlet;

import org.assertj.core.util.Files;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.Fields;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.junit.jupiter.api.BeforeEach;
import ru.justagod.vk.backend.control.SessionsManager;
import ru.justagod.vk.backend.db.DatabaseManager;
import ru.justagod.vk.backend.dos.DosProtection;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.*;

class ServletBaseTest {

    private static final String databaseFile = Files.newTemporaryFile().getAbsoluteFile().getAbsolutePath();

    protected ScheduledExecutorService executor;
    protected DatabaseManager database;
    protected DosProtection protection;
    protected SessionsManager sessions;

    @BeforeEach
    private void prepare() {
        executor = Executors.newScheduledThreadPool(5);
        database = DatabaseManager.create(databaseFile);
        protection = DosProtection.create(executor);
        sessions = SessionsManager.create(executor);
    }

    protected Server makeServer() {
        Server server = new Server(new ExecutorThreadPool(5));
        ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory());

        connector.setPort(0);

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
                new ChallengeServlet(database, protection, sessions)
        );
        for (ServletBase<?, ?> servlet : servlets) {
            handler.addServlet(new ServletHolder(servlet), "/" + servlet.getEndpoint().name);
        }

        return server;
    }

}