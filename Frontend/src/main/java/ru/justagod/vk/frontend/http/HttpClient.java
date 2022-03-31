package ru.justagod.vk.frontend.http;

import com.google.gson.Gson;
import ru.justagod.vk.network.Endpoint;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class HttpClient {

    private final Gson gson;
    private final String serverAddress;

    public HttpClient(Gson gson, String serverAddress) {
        this.gson = gson;
        this.serverAddress = serverAddress;
    }

    public <Request, Response> ServerResponse<Response> sendRequest(Endpoint<Request, Response> endpoint, Request payload) throws IOException {
        String address = serverAddress + "/" + endpoint.name;

        HttpURLConnection connection = (HttpURLConnection) (new URL(address).openConnection());
        try {
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.getOutputStream().write(endpoint.writeRequest(gson, payload).getBytes(StandardCharsets.UTF_8));
            connection.getOutputStream().flush();
            connection.getOutputStream().close();
            int code = connection.getResponseCode();

            InputStream input;
            if (code < 200 || code >= 300) {
                input = connection.getErrorStream();
            } else {
                input = connection.getInputStream();
            }

            String response = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            try {
                if (code < 200 || code >= 300) {
                    return ServerResponse.err(code, endpoint.parseError(gson, response));
                } else {
                    return ServerResponse.success(code, endpoint.parseResponse(gson, response));
                }
            } catch (Endpoint.ParsingException e) {
                throw new RuntimeException(e);
            }
        } finally {
            connection.disconnect();
        }
    }


}
