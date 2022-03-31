package ru.justagod.vk.backend.dos;

public final class UserStateController {
    private UserState currentState = UserState.initial();

    UserStateController() {}

    public boolean isEmpty() {
        return currentState == null || (currentState = currentState.update()) == null;
    }

    private UserState getState() {
        currentState = currentState.update();
        if (currentState == null) {
            currentState = UserState.initial();
        }
        return currentState;
    }

    public void requestReceived() {
        currentState = getState().requestReceived();
    }

    public boolean isBanned() {
        return isEmpty() || currentState.isBanned();
    }

}
