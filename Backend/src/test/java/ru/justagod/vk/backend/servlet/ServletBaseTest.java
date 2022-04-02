package ru.justagod.vk.backend.servlet;

import org.assertj.core.util.Files;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import ru.justagod.vk.backend.control.SessionsManager;
import ru.justagod.vk.backend.db.DatabaseManager;
import ru.justagod.vk.backend.dos.DosProtection;
import ru.justagod.vk.data.BackendError;
import ru.justagod.vk.data.BackendResponse;
import ru.justagod.vk.data.GsonHolder;
import ru.justagod.vk.frontend.http.HttpClient;
import ru.justagod.vk.frontend.http.ServerResponse;
import ru.justagod.vk.network.Endpoint;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

public class ServletBaseTest {

    private static final String databaseFile = Files.newTemporaryFile().getAbsoluteFile().getAbsolutePath();

    protected ScheduledExecutorService executor;
    protected DatabaseManager database;
    protected DosProtection protection;
    protected SessionsManager sessions;
    protected Server server;

    protected final <T> BackendError assertError(ServerResponse<T> response) {
        return assertError(response.response());
    }

    protected final <T> BackendError assertError(BackendResponse<T> response) {
        assertFalse(response.isSuccess(), response.toString());
        return response.error();
    }

    protected final <T> T assertSuccess(ServerResponse<T> response) {
        return assertSuccess(response.response());
    }

    protected final <T> T assertSuccess(BackendResponse<T> response) {
        assertNull(response.error());
        return response.payload();
    }

    protected final void assertUnauthorized(ServerResponse<?> response) {
        assertUnauthorized(response.response());
    }

    protected final void assertUnauthorized(BackendResponse<?> response) {
        assertErrorKind(BackendError.FORBIDDEN, response);
    }

    protected final void assertBadRequest(ServerResponse<?> response) {
        assertBadRequest(response.response());
    }

    protected final void assertBadRequest(BackendResponse<?> response) {
        assertErrorKind(BackendError.BAD_REQUEST, response);
    }

    protected final void assertErrorKind(int errorKind, ServerResponse<?> response) {
        assertErrorKind(errorKind, response.response());
    }

    protected final void assertErrorKind(int errorKind, BackendResponse<?> response) {
        assertEquals(BackendError.codeName(errorKind), BackendError.codeName(assertError(response).kind()));

    }

    protected void mockDatabaseReadOnly() throws Exception {
        mockDatabase(this::makeMockDatabaseReadOnly);
    }
    protected DatabaseManager makeMockDatabaseReadOnly() {
        DatabaseManager spy = Mockito.spy(DatabaseManager.create(databaseFile));
        Mockito.doThrow(AssertionError.class).when(spy).addFriend(Mockito.any(), Mockito.any());
        Mockito.doThrow(AssertionError.class).when(spy).removeFriend(Mockito.any(), Mockito.any());
        Mockito.doThrow(AssertionError.class).when(spy).addMessage(Mockito.any());
        Mockito.doThrow(AssertionError.class).when(spy).addUser(Mockito.any(), Mockito.any());

        return spy;
    }

    protected void mockDatabase(Supplier<DatabaseManager> supplier) throws Exception {
        destroy();
        prepare(supplier.get());
    }

    @BeforeEach
    protected void prepare() throws Exception {
        prepare(DatabaseManager.create(databaseFile));
    }
    protected void prepare(DatabaseManager database) throws Exception {
        executor = Executors.newScheduledThreadPool(5);
        this.database = database;
        protection = DosProtection.create(executor);
        sessions = SessionsManager.create(executor);
        server = makeServer();
        server.start();
    }

    @AfterEach
    protected void destroy() throws Exception {
        executor.shutdownNow();
        database.close();
        server.stop();
        new File(databaseFile).delete();
    }

    protected <Request, Response> ServerResponse<Response> challengeByPass(
            HttpClient client,
            Endpoint<Request, Response> endpoint,
            Request request
    ) throws IOException {
        var response = client.sendRequest(endpoint, request);
        if (!response.success() && response.response().error().kind() == BackendError.CHALLENGE_REQUIRED) {
            protection.solveChallenge("127.0.0.1");
            response = client.sendRequest(endpoint, request);
        }
        return response;
    }

    protected HttpClient connect() {
        ServerConnector connector = (ServerConnector) server.getConnectors()[0];
        return new HttpClient(GsonHolder.gson, "http://localhost:" + connector.getLocalPort());
    }

    private Server makeServer() {
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