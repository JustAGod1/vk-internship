package ru.justagod.vk.backend.db;

import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.justagod.vk.backend.model.Message;
import ru.justagod.vk.backend.model.User;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseManager {

    private static final String CHAT_HISTORY_TABLE = "vk_chat_history";
    private static final String USERS_TABLE = "vk_users";

    // Actually everything is written in a such way that we can replace sqlite with any other much more decent
    // database but for the sake of easy start of the application I use sqlite here
    private static final String DATABASE_URL_PATTERN = "jdbc:sqlite:";

    private final ConnectionPool pool;

    public static DatabaseManager create(String file) {
        return new DatabaseManager(file);
    }

    private DatabaseManager(String file) {
        pool = ConnectionPool.create(DATABASE_URL_PATTERN + file);
        initializeTables();
    }

    private boolean isTableExists(String name) {
        return pool.mapStatement(s -> s.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='%s';".formatted(name)).next());
    }
    private void initializeTables() {
        if (!isTableExists(USERS_TABLE)) {
            @Language("SQLite") String userRegistryCreation =
                    "CREATE TABLE " + USERS_TABLE + "(\n" +
                            "    id integer not null primary key autoincrement ,\n" +
                            "    uuid VARCHAR(128)," +
                            "    password_hash VARCHAR(40),\n" +
                            "    username VARCHAR(128)\n" +
                            ");\n" +
                            "CREATE INDEX username_idx ON vk_users (username);";
            pool.withStatement(s -> s.execute(userRegistryCreation));
        }

        if (!isTableExists(CHAT_HISTORY_TABLE)) {
            @Language("SQLite") String chatHistoryCreation =
                    "CREATE TABLE " + CHAT_HISTORY_TABLE + "(\n" +
                    "    id integer primary key autoincrement ,\n" +
                    "    uuid VARCHAR(128)," +
                    "    sender_id VARCHAR(128) not null,\n" +
                    "    receiver_id VARCHAR(128) not null,\n" +
                    "    \n" +
                    "    content VARCHAR(4096),\n" +
                    "    sent_at TIMESTAMP DEFAULT current_timestamp\n" +
                    ");\n" +
                    "CREATE INDEX receiver_idx ON vk_chat_history(receiver_id);\n" +
                    "CREATE INDEX sender_idx ON vk_chat_history(sender_id);\n" +
                            "CREATE INDEX sent_at_idx ON vk_chat_history(sent_at);";
            // Here can be foreign index but meh I don't like them
            pool.withStatement(s -> s.execute(chatHistoryCreation));
        }
    }

    public void addMessage(Message message) {
        @Language("SQLite") String sql = "INSERT INTO %s (sender_id, receiver_id, content, uuid, sent_at) VALUES (?, ?, ?, ?, ?)"
                .formatted(CHAT_HISTORY_TABLE);
        pool.withPreparedStatement(sql, s -> {
            s.setString(1, message.sender().id().toString());
            s.setString(2, message.receiver().id().toString());
            s.setString(3, message.content());
            s.setString(4, message.id().toString());
            s.setTimestamp(5, Timestamp.from(message.sentAt()));
        });
    }

    public List<Message> readMessages(long offset, User first, User second) {
        @Language("SQLite") String sql = "SELECT uuid, sent_at, content, sender_id, receiver_id FROM %s WHERE (receiver_id = ? AND sender_id = ?) OR (receiver_id = ? AND sender_id = ?) ORDER BY sent_at DESC LIMIT 100 OFFSET %d"
                .formatted(CHAT_HISTORY_TABLE, offset);

        return pool.mapPreparedStatement(sql, s -> {
            List<Message> result = new ArrayList<>();
            s.setString(1, second.id().toString());
            s.setString(2, first.id().toString());
            s.setString(3, first.id().toString());
            s.setString(4, second.id().toString());

            ResultSet queryResult = s.executeQuery();

            while (queryResult.next()) {
                Message message = new Message(
                        UUID.fromString(queryResult.getString("uuid")),
                        new User(UUID.fromString(queryResult.getString("sender_id"))),
                        new User(UUID.fromString(queryResult.getString("receiver_id"))),
                        queryResult.getTimestamp("sent_at").toInstant(),
                        queryResult.getString("content")
                );

                result.add(message);
            }

            return result;
        });
    }

    @NotNull
    public User addUser(String passwordHash, String username) {
        User user = new User(UUID.randomUUID());

        @Language("SQLite") String sql = "INSERT INTO %s (uuid, password_hash, username) VALUES (?, ?, ?)"
                .formatted(USERS_TABLE);

        pool.withPreparedStatement(sql, s -> {
            s.setString(1, user.id().toString());
            s.setString(2, passwordHash);
            s.setString(3, username);
        });

        return user;
    }

    @Nullable
    public User findUser(String username) {
        @Language("SQLite") String sql = "SELECT uuid FROM %s WHERE username = ? LIMIT 1"
                .formatted(USERS_TABLE);

        return pool.mapPreparedStatement(sql, s -> {
            s.setString(1, username);

            ResultSet result = s.executeQuery();
            if (!result.next()) return null;

            return new User(UUID.fromString(result.getString(1)));
        });
    }
    @Nullable
    public User findUser(String username, String passwordHash) {
        @Language("SQLite") String sql = "SELECT uuid FROM %s WHERE password_hash = ? and username = ? LIMIT 1"
                .formatted(USERS_TABLE);

        return pool.mapPreparedStatement(sql, s -> {
            s.setString(1, passwordHash);
            s.setString(2, username);

            ResultSet result = s.executeQuery();
            if (!result.next()) return null;

            return new User(UUID.fromString(result.getString(1)));
        });
    }

}
