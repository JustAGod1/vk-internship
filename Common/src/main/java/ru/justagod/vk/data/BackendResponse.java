package ru.justagod.vk.data;

import org.jetbrains.annotations.Nullable;

public record BackendResponse(
        boolean success,
        @Nullable
        String error,
        @Nullable
        String payload
) {
}
