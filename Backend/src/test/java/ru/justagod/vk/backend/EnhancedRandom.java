package ru.justagod.vk.backend;

import ru.justagod.vk.data.Message;
import ru.justagod.vk.data.User;

import java.time.Instant;
import java.util.Random;
import java.util.UUID;

public class EnhancedRandom {

    public static String randomString(Random random, int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append((char) ('a' + random.nextInt('z' - 'a')));
        }

        return sb.toString();
    }

    public static Message randomMessageAndDirection(User from, User to, Random random, int len) {
        if (random.nextInt(2) == 1) return randomMessage(from, to, random, len);
        else return randomMessage(to, from, random, len);
    }
    public static Message randomMessage(User from, User to, Random random, int len) {
        String content = randomString(random, len);
        UUID uuid = UUID.randomUUID();

        return new Message(uuid, from, to, Instant.ofEpochSecond(random.nextInt()), content);
    }

}
