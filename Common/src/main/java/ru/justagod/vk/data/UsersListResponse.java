package ru.justagod.vk.data;

import java.util.List;

public class UsersListResponse {
    private List<UserName> users;

    public UsersListResponse(List<UserName> users) {
        this.users = users;
    }

    public List<UserName> users() {
        return users;
    }
}
