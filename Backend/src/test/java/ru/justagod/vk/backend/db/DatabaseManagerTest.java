package ru.justagod.vk.backend.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.justagod.vk.backend.model.Message;
import ru.justagod.vk.backend.model.User;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseManagerTest {

    private DatabaseManager manager;

    @BeforeEach
    void setUp() throws IOException {
        manager = DatabaseManager.create(Files.createTempFile(null, null).toAbsolutePath().toString());
    }

    @Test
    void createUsers() {
        User user = manager.addUser("****", "ip");

        assertEquals(manager.findUser("ip"), user);
        assertNull(manager.findUser("ipp"));
    }

    @Test
    void findUserByPassword() {
        User user = manager.addUser("***", "ip");

        assertEquals(manager.findUser("ip", "***"), user);
        assertNull(manager.findUser("ip", "****"));
        assertNull(manager.findUser("ipp", "***"));
        assertNull(manager.findUser("ipp", "****"));
    }

    @Test
    void hashPersistence() {
        User user = manager.addUser(PasswordsManager.hashed("***"), "ip");

        assertEquals(manager.findUser("ip", PasswordsManager.hashed("***")), user);
        assertNull(manager.findUser("ipp", PasswordsManager.hashed("***")));
        assertNull(manager.findUser("ip", PasswordsManager.hashed("****")));
        assertNull(manager.findUser("ipp", PasswordsManager.hashed("****")));
    }

    @Test
    void batchMessages() {
        User ivan = manager.addUser("***", "ip");
        User sergey = manager.addUser("***", "sp");

        Instant epoch = Instant.EPOCH;

        Random random = new Random(777);

        Stream<Message> messagesGenerator = IntStream
                .generate(new AtomicInteger()::incrementAndGet)
                .mapToObj((v) ->
                Message.create(
                        v % 2 == 0 ? ivan : sergey, v % 2 == 1 ? ivan : sergey,
                        epoch.plusMillis(v * 2L),
                        IntStream
                                .range(0, random.nextInt(20))
                                .mapToObj(a -> String.valueOf('a' + random.nextInt('z'-'a')))
                                .collect(Collectors.joining(""))
                        )
                );

        int count = random.nextInt(2000);

        List<Message> messages = messagesGenerator.limit(count).collect(Collectors.toList());
        Collections.reverse(messages);

        for (Message message : messages) {
            manager.addMessage(message);
        }

        for (int i = 0; i < (messages.size() + 99) / 100; i++) {
            List<Message> batch = new ArrayList<>();

            for (int j = 0; j < 100; j++) {
                int idx = 100 * i + j;
                if (idx >= messages.size()) break;
                batch.add(messages.get(idx));
            }

            List<Message> read = manager.readMessages(i * 100, ivan, sergey);

            assertEquals(batch, read);
        }
    }
    @Test
    void bigMessage() {
        User ivan = manager.addUser("***", "ip");
        User sergey = manager.addUser("***", "sp");

        Instant sentAt = Instant.EPOCH;
        Message sent = Message.create(ivan, sergey, sentAt, "k".repeat(4096));
        manager.addMessage(sent);

        Message received = manager.readMessages(0, ivan, sergey).get(0);

        assertEquals(sent, received);
    }
}