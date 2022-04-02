package ru.justagod.vk.backend.db;

import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.LinkedList;

public class ConnectionPool {

    public static final int INITIAL_CONNECTIONS = 2;
    public static final int MAX_TIMEOUT_SECONDS = 5;

    private final LinkedList<Connection> pool = new LinkedList<>();
    private final String databaseUrl;

    private ConnectionPool(String databaseUrl) {
        this.databaseUrl = databaseUrl;
    }

    private boolean isAlive(Connection connection) {
        try {
            if (connection.isClosed()) return false;
            return connection.isValid(MAX_TIMEOUT_SECONDS);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private synchronized Connection getConnection() {
        Connection connection = pool.poll();

        while (connection != null && !isAlive(connection)) {
            connection = pool.poll();
        }

        if (connection == null) return createConnection();

        return connection;
    }

    private synchronized void freeConnection(@NotNull Connection connection) {
        pool.add(connection);
    }

    public <T> T mapStatement(@NotNull SQLFunction<Statement, T> block) {
        return mapConnection(connection -> {
            try (Statement s = connection.createStatement()) {
                return block.apply(s);
            }
        });
    }
    public <T> T mapPreparedStatement(@Language("SQLite") String sql, @NotNull SQLFunction<PreparedStatement, T> block) {
        return mapConnection(connection -> {
            try (PreparedStatement s = connection.prepareStatement(sql)) {
                return block.apply(s);
            }
        });
    }
    public <T> T mapConnection(@NotNull SQLFunction<Connection, T> block) {
        Connection connection = getConnection();
        try {
            return block.apply(connection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            freeConnection(connection);
        }
    }


    public void withPreparedStatement(@Language("SQLite") String pattern, SQLConsumer<PreparedStatement> block) {
        withConnection(connection -> {
            try (PreparedStatement s = connection.prepareStatement(pattern)) {
                block.accept(s);
                s.executeUpdate();
            }
        });
    }
    public void withStatement(SQLConsumer<Statement> block) {
        withConnection(connection -> {
            try (Statement s = connection.createStatement()) {
                block.accept(s);
            }
        });
    }

    public void withConnection(SQLConsumer<Connection> block) {
        Connection connection = getConnection();
        try {
            block.accept(connection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            freeConnection(connection);
        }
    }

    @NotNull
    private Connection createConnection() {
        try {
            return DriverManager.getConnection(databaseUrl);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static ConnectionPool create(String databaseUrl) {
        ConnectionPool r = new ConnectionPool(databaseUrl);

        for (int i = 0; i < INITIAL_CONNECTIONS; i++) {
            r.pool.add(r.createConnection());
        }

        return r;
    }

    public void close() throws SQLException {
        for (Connection connection : pool) {
            connection.close();
        }
    }

    @FunctionalInterface
    public interface SQLConsumer<T> {
        void accept(T value) throws SQLException;
    }

    @FunctionalInterface
    public interface SQLFunction<T, R> {
        R apply(T value) throws SQLException;
    }

}
