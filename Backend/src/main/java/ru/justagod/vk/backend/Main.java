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
import ru.justagod.vk.backend.servlet.SignInServlet;
import ru.justagod.vk.backend.servlet.SignUpServlet;
import ru.justagod.vk.data.GsonHolder;
import ru.justagod.vk.network.Endpoint;

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

    private static Server makeServer() {
        Server server = new Server(new ExecutorThreadPool(5));
        ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory());

        connector.setPort(8888);

        server.addConnector(connector);

        ServletContextHandler handler = new ServletContextHandler();
        server.setHandler(handler);

        handler.addServlet(new ServletHolder(new SignUpServlet(database)), "/" + Endpoint.SIGN_UP_REQUEST_ENDPOINT.name);
        handler.addServlet(new ServletHolder(new SignInServlet(database)), "/" + Endpoint.SIGN_IN_REQUEST_ENDPOINT.name);

        return server;
    }
}
