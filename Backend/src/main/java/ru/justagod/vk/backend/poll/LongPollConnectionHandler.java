package ru.justagod.vk.backend.poll;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface LongPollConnectionHandler {

    void handle(@NotNull ByteBuffer payload) throws IOException;

    void connection(@NotNull LongPollServerConnection connection) throws IOException;

    default void deregister() {}
}
