package ru.justagod.vk.data;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

public final class SignUpRequest {
    private String username;
    private String password;

    public SignUpRequest(
            String username,
            String password // Not hash
    ) {
        this.username = username;
        this.password = password;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

}
