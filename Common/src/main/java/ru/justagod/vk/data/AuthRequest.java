package ru.justagod.vk.data;

public record AuthRequest (
    String login,
    String password
) {}
