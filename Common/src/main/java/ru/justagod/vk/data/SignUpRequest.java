package ru.justagod.vk.data;

import com.google.gson.annotations.SerializedName;

public record SignUpRequest(
        String username,
        String password, // Not hash
        String firstname,
        String surname
) {
}
