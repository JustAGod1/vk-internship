package ru.justagod.vk.frontend.poll;

import ru.justagod.vk.data.BackendResponse;
import ru.justagod.vk.frontend.Main;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class LongPollClientConnection {

    private final Socket socket;
    private final DataOutputStream output;
    private final DataInputStream input;

    private LongPollClientConnection(InetSocketAddress address) throws IOException {
        socket = new Socket(address.getAddress(), address.getPort());
        output = new DataOutputStream(socket.getOutputStream());
        input = new DataInputStream(socket.getInputStream());
    }

    public static LongPollClientConnection connect(InetSocketAddress address) throws IOException {
        return new LongPollClientConnection(address);
    }


    public void write(Object value) throws IOException {
        byte[] json = Main.gson.toJson(value).getBytes(StandardCharsets.UTF_8);

        output.writeInt(json.length);
        output.write(json);
        output.flush();
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
