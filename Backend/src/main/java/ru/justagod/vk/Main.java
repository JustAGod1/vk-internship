package ru.justagod.vk;

import com.google.gson.Gson;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.ResourceService;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import ru.justagod.vk.db.DatabaseManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static final Gson gson = new Gson();
    public static final ExecutorService executor = Executors.newCachedThreadPool();
    public static final DatabaseManager manager = DatabaseManager.create();

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
