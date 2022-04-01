package ru.justagod.vk.backend.poll;

import com.google.gson.Gson;
import org.jetbrains.annotations.Nullable;
import ru.justagod.vk.data.BackendError;
import ru.justagod.vk.data.BackendResponse;
import ru.justagod.vk.data.Message;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;

public class LongPollServerConnection {
    private final Gson gson;
    private final SocketChannel channel;
    private final Selector mySelector;

    private volatile boolean closed = false;
    private volatile boolean closing = false;

    @Nullable
    private MessageInReceiving messageInReceiving;
    private final ByteBuffer sizeBuffer = ByteBuffer.allocateDirect(4);


    @Nullable
    private LongPollServerConnection.MessageInSending messageInSending;
    private final ConcurrentLinkedDeque<MessageInSending> queue = new ConcurrentLinkedDeque<>();

    public LongPollConnectionHandler handler;

    public LongPollServerConnection(Gson gson, SocketChannel channel, LongPollConnectionHandler handler) throws IOException {
        this.gson = gson;
        this.channel = channel;
        setHandler(handler);
        mySelector = Selector.open();
        channel.configureBlocking(false);
        channel.register(mySelector, SelectionKey.OP_WRITE);
    }

    public void setHandler(LongPollConnectionHandler handler) throws IOException {
        Objects.requireNonNull(handler);
        handler.connection(this);
        this.handler = handler;
    }

    public boolean loop() throws IOException {
        if (!channel.isConnected()) return false;
        mySelector.select();

        for (SelectionKey key : mySelector.selectedKeys()) {
            if (!key.isValid()) continue;
            if (key.isReadable()) {
                read();
            } else if (key.isWritable()) {
                write();
            } else {
                throw new RuntimeException("WTF");
            }
        }
        return !closed;
    }

    private void read() throws IOException {
        if (messageInReceiving != null) {
            ByteBuffer msg = messageInReceiving.read();
            if (msg != null) {
                handler.handle(msg);
                sizeBuffer.flip();
            }
            read();
        }
        channel.read(sizeBuffer);
        if (!sizeBuffer.hasRemaining()) {
            int size = sizeBuffer.getInt();
            messageInReceiving = new MessageInReceiving(size);
            read();
        }
    }

    public void closeChannel(int errorKind) throws IOException {
        closeChannel(new BackendError(errorKind, null));
    }

    public void closeChannel(BackendError error) {
        closing = true;
        try {
            channel.shutdownInput();
            sendMessage(BackendResponse.error(error), () -> {
                closed = true;
                try {
                    channel.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return true if no messages are waiting to be written
     */
    private boolean tryToWrite() throws IOException {
        MessageInSending msg = messageInSending;
        if (msg == null) return true;
        channel.write(msg.payload);
        if (!msg.payload.hasRemaining()) {
            if (msg.listeners != null)
                for (Runnable listener : msg.listeners) {
                    listener.run();
                }
            messageInSending = null;
            return true;
        }
        return false;
    }

    public <T> void sendMessage(BackendResponse<T> payload, Runnable... listeners) throws IOException {
        if (closing) return;
        byte[] bytes = payload.toJson(gson).getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        MessageInSending messageInSending = new MessageInSending(buffer);
        if (listeners.length > 0) {
            messageInSending.listeners = new ArrayList<>();
            messageInSending.listeners.addAll(Arrays.asList(listeners));
        }

        queue.add(messageInSending);
    }


    private void write() throws IOException {
        while (tryToWrite()) {
            MessageInSending msg = queue.poll();
            if (msg == null) return;
            messageInSending = msg;
        }
    }

    private static class MessageInSending {
        public final ByteBuffer payload;
        @Nullable
        public List<Runnable> listeners;

        private MessageInSending(ByteBuffer payload) {
            this.payload = payload;
        }
    }

    private class MessageInReceiving {
        private final ByteBuffer data;

        private MessageInReceiving(int size) {
            data = ByteBuffer.allocate(size);
        }

        @Nullable
        public ByteBuffer read() throws IOException {
            channel.read(data);
            if (!data.hasRemaining()) return data;
            return null;
        }
    }

}
