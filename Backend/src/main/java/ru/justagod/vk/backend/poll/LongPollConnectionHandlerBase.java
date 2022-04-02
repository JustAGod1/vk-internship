package ru.justagod.vk.backend.poll;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import org.jetbrains.annotations.NotNull;
import ru.justagod.vk.data.BackendError;
import ru.justagod.vk.data.BackendResponse;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public abstract class LongPollConnectionHandlerBase<Input, Output> implements LongPollConnectionHandler {

    protected final Gson gson;
    protected LongPollServerConnection connection;
    private final Class<Input> inputClass;

    protected LongPollConnectionHandlerBase(Gson gson, Class<Input> inputClass) {
        this.gson = gson;
        this.inputClass = inputClass;
    }


    @Override
    public void connection(@NotNull LongPollServerConnection connection) throws IOException {
        this.connection = connection;
    }

    @Override
    public final void handle(@NotNull ByteBuffer payload) throws IOException {
         try {
             String payloadString = new String(payload.array(), StandardCharsets.UTF_8);
             Input input = gson.fromJson(payloadString, inputClass);

             Output output = handleRequest(input);

             connection.sendMessage(BackendResponse.success(output));
         } catch (JsonParseException e) {
             connection.closeChannel(BackendError.BAD_REQUEST);
         }
    }

    protected abstract Output handleRequest(Input input) throws IOException;


}
