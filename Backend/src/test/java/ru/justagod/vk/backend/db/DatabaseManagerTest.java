package ru.justagod.vk.backend.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.justagod.vk.backend.EnhancedRandom;
import ru.justagod.vk.data.Message;
import ru.justagod.vk.data.Messages;
import ru.justagod.vk.data.User;

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
        assertNull(manager.findUser("iP"));
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

        Random random = new Random(777);

        Stream<Message> messagesGenerator = IntStream
                .generate(new AtomicInteger()::incrementAndGet)
                .mapToObj((v) -> EnhancedRandom.randomMessageAndDirection(ivan, sergey, random, 30, Instant.ofEpochSecond(v)));

        int count = random.nextInt(2000);

        List<Message> messages = messagesGenerator.limit(count).collect(Collectors.toList());
        Collections.reverse(messages);

        TreeMap<Instant, Message> treeMap = new TreeMap<>();

        for (Message message : messages) {
            treeMap.put(message.sentAt(), message);
            manager.addMessage(message);
        }

        int iterations = random.nextInt(300);

        for (int i = 0; i < iterations; i++) {
            Message pivot = messages.get(random.nextInt(messages.size()));

            List<Message> actual = manager.readMessages(pivot.sentAt(), pivot.receiver(), pivot.sender());
            List<Message> expected = new ArrayList<>();

            while (pivot != null && expected.size() < 100) {
                expected.add(pivot);
                var entry = treeMap.lowerEntry(pivot.sentAt());
                if (entry != null) {
                    pivot = entry.getValue();
                } else {
                    pivot = null;
                }
            }

            assertEquals(expected, actual);

        }
    }
    @Test
    void bigMessage() {
        User ivan = manager.addUser("***", "ip");
        User sergey = manager.addUser("***", "sp");

        Instant sentAt = Instant.EPOCH;
        Message sent = Message.create(ivan, sergey, sentAt, "k".repeat(4096));
        manager.addMessage(sent);

        Message received = manager.readMessages(sentAt, ivan, sergey).get(0);

        assertEquals(sent, received);
    }
}