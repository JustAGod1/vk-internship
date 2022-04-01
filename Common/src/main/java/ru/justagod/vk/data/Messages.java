package ru.justagod.vk.data;

import java.util.List;

public class Messages {
    private List<Message> messages;

    public Messages(List<Message> messages) {
        this.messages = messages;
    }

    public List<Message> messages() {
        return messages;
    }
}
