package ru.justagod.vk.backend.db;

import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.justagod.vk.backend.model.Message;
import ru.justagod.vk.backend.model.User;

import java.sql.ResultSet;
import java.util.UUID;

public class DatabaseManager {

    private static final String CHAT_HISTORY_TABLE = "vk_chat_history";
    private static final String USERS_TABLE = "vk_users";

    // Actually everything is written in a such way that we can replace sqlite with any other much more decent
    // database but for the sake of easy start of the application I use sqlite here
    private static final String DATABASE_URL = "jdbc:sqlite:database.sqlite";

    private final ConnectionPool pool;

    public static DatabaseManager create() {
        return new DatabaseManager();
    }

    private DatabaseManager() {
        pool = ConnectionPool.create(DATABASE_URL);
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
                            "    firstname VARCHAR(256),\n" +
                            "    surname VARCHAR(256),\n" +
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
        @Language("SQLite") String sql = "INSERT INTO %s (sender_id, receiver_id, content) VALUES (?, ?, ?)"
                .formatted(CHAT_HISTORY_TABLE);
        pool.withPreparedStatement(sql, s -> {
            s.setString(1, message.sender().id().toString());
            s.setString(2, message.receiver().id().toString());
            s.setString(3, message.content());
        });
    }

    @NotNull
    public User addUser(String firstname, String surname, String passwordHash, String username) {
        User user = new User(UUID.randomUUID());

        @Language("SQLite") String sql = "INSERT INTO %s (uuid, firstname, surname, password_hash, username) VALUES (?, ?, ?, ?, ?)"
                .formatted(USERS_TABLE);

        pool.withPreparedStatement(sql, s -> {
            s.setString(1, user.id().toString());
            s.setString(2, firstname);
            s.setString(3, surname);
            s.setString(4, passwordHash);
            s.setString(5, username);
        });

        return user;
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
