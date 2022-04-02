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
    public static final Endpoint<AuthorizedUserRequest, Void> ADD_FRIEND_REQUEST_ENDPOINT
            = new Endpoint<>("add_friend", AuthorizedUserRequest.class, Void.class);
    public static final Endpoint<AuthorizedUserRequest, Void> REMOVE_FRIEND_REQUEST_ENDPOINT
            = new Endpoint<>("remove_friend", AuthorizedUserRequest.class, Void.class);
    public static final Endpoint<Integer, Void> SOLVE_CHALLENGE_REQUEST_ENDPOINT
            = new Endpoint<>("challenge", Integer.class, Void.class);
    public static final Endpoint<MessagesRequest, Messages> GET_MESSAGES_REQUEST_ENDPOINT
            = new Endpoint<>("get_message", MessagesRequest.class, Messages.class);
    public static final Endpoint<SendMessageRequest, Void> SEND_MESSAGE_REQUEST_ENDPOINT
            = new Endpoint<>("send_message", SendMessageRequest.class, Void.class);

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
            Request result = gson.fromJson(payload, requestClass);
            if (result == null && requestClass != Void.class) throw new ParsingException("null is not expected");
            return result;
        } catch (JsonParseException e) {
            throw new ParsingException(e);
        }
    }

    public BackendResponse<Response> parseResponse(Gson gson, String payload) throws ParsingException {
        try {
            return BackendResponse.fromJson(gson, payload, responseClass);
        } catch (JsonParseException e) {
            throw new ParsingException(e);
        }
    }


    public String writeRequest(Gson gson, Request request) {
        return gson.toJson(request);
    }

    public String writeResponse(Gson gson, BackendResponse<Response> response) {
        return response.toJson(gson);
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
