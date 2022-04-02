package ru.justagod.vk.backend.poll;

import com.google.gson.Gson;
import org.jetbrains.annotations.Nullable;
import ru.justagod.vk.backend.dos.RequestsWindow;
import ru.justagod.vk.data.BackendError;
import ru.justagod.vk.data.BackendResponse;
import ru.justagod.vk.data.Message;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;

public class LongPollServerConnection {

    public static final int MAX_MESSAGE_SIZE = 2 * 4096;
    public static final int MAX_REQUESTS = 50;
    public static final Duration TRACKING_DURATION = Duration.of(2, ChronoUnit.SECONDS);

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

    private final RequestsWindow window = new RequestsWindow(TRACKING_DURATION);

    public LongPollServerConnection(Gson gson, SocketChannel channel, LongPollConnectionHandler handler) throws IOException {
        this.gson = gson;
        this.channel = channel;
        setHandler(handler);
        mySelector = Selector.open();
        channel.configureBlocking(false);
        channel.register(mySelector, SelectionKey.OP_WRITE | SelectionKey.OP_READ);
    }

    public void setHandler(LongPollConnectionHandler handler) throws IOException {
        Objects.requireNonNull(handler);
        handler.connection(this);
        this.handler = handler;
    }

    public boolean loop() throws IOException {
        if (!channel.isConnected()) return false;
        mySelector.selectNow();

        for (SelectionKey key : mySelector.selectedKeys()) {
            if (!key.isValid()) continue;
            if (key.isReadable()) {
                read();
            }
            write();
        }
        return !closed;
    }

    private boolean checkWindow() throws IOException {
        window.addRequest();
        if (window.getRequestsCount() > MAX_REQUESTS) {
            closeChannel(BackendError.TOO_MANY_REQUESTS);
            return true;
        }

        return false;
    }

    private void read() throws IOException {
        if (closing) return;
        if (messageInReceiving != null) {
            ByteBuffer msg = messageInReceiving.read();
            if (msg != null) {
                messageInReceiving = null;
                msg.position(0);
                if (checkWindow()) return;
                handler.handle(msg);
            } else {
                return;
            }
        }
        channel.read(sizeBuffer);
        if (!sizeBuffer.hasRemaining()) {
            sizeBuffer.position(0);
            int size = sizeBuffer.getInt();
            sizeBuffer.flip();
            if (size > MAX_MESSAGE_SIZE) {
                closeChannel(BackendError.PROTOCOL_ERROR);
                return;
            }
            messageInReceiving = new MessageInReceiving(size);
            read();
        }
    }

    public void closeChannel(int errorKind) {
        closeChannel(new BackendError(errorKind, null));
    }

    public void closeChannel(BackendError error) {
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
        closing = true;
    }

    /**
     * @return true if no messages are waiting to be written
     */
    private boolean tryToWrite() throws IOException {
        MessageInSending msg = messageInSending;
        if (closed) return true;
        if (msg == null) return true;
        channel.write(msg.payload);
        channel.socket().getOutputStream().flush();
        if (!msg.payload.hasRemaining()) {
            if (msg.listeners != null) {
                for (Runnable listener : msg.listeners) {
                    listener.run();
                }
            }
            messageInSending = null;
            return true;
        }
        return false;
    }

    public <T> void sendMessage(BackendResponse<T> payload, Runnable... listeners) throws IOException {
        if (closing) return;
        // Here we should somehow optimize a lot of redundant coping
        byte[] bytes = payload.toJson(gson).getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(bytes.length + 4);
        buffer.putInt(bytes.length);
        buffer.put(bytes);
        buffer.position(0);

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
