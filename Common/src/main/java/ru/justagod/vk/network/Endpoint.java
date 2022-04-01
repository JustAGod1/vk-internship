package ru.justagod.vk.network;

import com.google.gson.*;
import ru.justagod.vk.data.*;

public class Endpoint<Request, Response> {

    public static final Endpoint<UserPasswordRequest, SessionResponse> SIGN_UP_REQUEST_ENDPOINT
            = new Endpoint<>("signup", UserPasswordRequest.class, SessionResponse.class);
    public static final Endpoint<UserPasswordRequest, SessionResponse> SIGN_IN_REQUEST_ENDPOINT
            = new Endpoint<>("signin", UserPasswordRequest.class, SessionResponse.class);
    public static final Endpoint<AuthorizedRequest, UsersListResponse> FRIENDS_REQUEST_ENDPOINT
            = new Endpoint<>("friends", AuthorizedRequest.class, UsersListResponse.class);
    public static final Endpoint<AuthorizedRequest, UsersListResponse> USERS_REQUEST_ENDPOINT
            = new Endpoint<>("users", AuthorizedRequest.class, UsersListResponse.class);

    public final String name;
    public final Class<Request> requestClass;
    public final Class<Response> responseClass;

    public Endpoint(String name, Class<Request> requestClass, Class<Response> responseClass) {
        this.name = name;
        this.requestClass = requestClass;
        this.responseClass = responseClass;
    }

    public Request parseRequest(Gson gson, String payload) throws ParsingException {
        try {
            return gson.fromJson(payload, requestClass);
        } catch (JsonParseException e) {
            throw new ParsingException(e);
        }
    }

    public Response parseResponse(Gson gson, String payload) throws ParsingException {
        try {
            return gson.fromJson(payload, responseClass);
        } catch (JsonParseException e) {
            throw new ParsingException(e);
        }
    }

    public BackendError parseError(Gson gson, String payload) throws ParsingException {
        try {
            return gson.fromJson(payload, BackendError.class);
        } catch (JsonParseException e) {
            throw new ParsingException(e);
        }
    }

    public String writeRequest(Gson gson, Request request) {
        return gson.toJson(request);
    }

    public String writeResponse(Gson gson, BackendResponse<Response> response) {
        if (response.error() != null) return gson.toJson(response.error());
        return gson.toJson(response.payload());
    }

    public static class ParsingException extends Exception {
        public ParsingException(String message) {
            super(message);
        }

        public ParsingException(String message, Throwable cause) {
            super(message, cause);
        }

        public ParsingException(Throwable cause) {
            super(cause);
        }

    }

}
