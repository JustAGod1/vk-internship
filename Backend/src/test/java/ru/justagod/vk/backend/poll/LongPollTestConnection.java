package ru.justagod.vk.backend.poll;

import ru.justagod.vk.backend.Main;
import ru.justagod.vk.data.BackendResponse;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class LongPollTestConnection {

    private static final ThreadLocal<Random> random = ThreadLocal.withInitial(() -> new Random(777));

    private final Socket socket;
    private final DataInputStream input;

    private LongPollTestConnection(InetSocketAddress address) throws IOException {
        socket = new Socket(address.getAddress(), address.getPort());
        socket.setSoTimeout(1000);
        input = new DataInputStream(socket.getInputStream());
    }

    public static LongPollTestConnection connect(InetSocketAddress address) throws IOException {
        return new LongPollTestConnection(address);
    }


    public void write(Object value) throws IOException {
        byte[] json = Main.gson.toJson(value).getBytes(StandardCharsets.UTF_8);

        // Here we're simulating splitting of tcp packets
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        DataOutputStream output = new DataOutputStream(baos);

        output.writeInt(json.length);
        output.write(json);

        int idx = 0;
        byte[] data = baos.toByteArray();
        while (idx < data.length) {
            int n = random.get().nextInt(data.length - idx + 1);
            socket.getOutputStream().write(data, idx, n);
            socket.getOutputStream().flush();
            idx += n;
        }
    }

    public <T> BackendResponse<T> read(Class<T> responseClass) throws IOException {
        int len = input.readInt();
        byte[] data = new byte[len];
        input.readFully(data);

        return BackendResponse.fromJson(Main.gson, new String(data, StandardCharsets.UTF_8), responseClass);
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
