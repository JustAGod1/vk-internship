package ru.justagod.vk.backend;

import com.google.gson.Gson;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import ru.justagod.vk.backend.db.DatabaseManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Main {
    public static final Gson gson = new Gson();
    public static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(3);
    public static final DatabaseManager database = DatabaseManager.create();

    public static void main(String[] args) throws Exception {
        Server server = new Server(new ExecutorThreadPool(5));
        ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory());

        connector.setPort(8888);

        server.addConnector(connector);

        ServletContextHandler handler = new ServletContextHandler();
        server.setHandler(handler);

        server.start();
    }
}
