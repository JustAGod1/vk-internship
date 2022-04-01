package ru.justagod.vk.backend.dos;

import org.jetbrains.annotations.Nullable;

public final class UserStateController {
    private UserState currentState = UserState.initial();

    UserStateController() {}

    public boolean isEmpty() {
        return currentState == null || (currentState = currentState.update()) == null;
    }

    private UserState getState() {
        if (currentState == null) {
            currentState = UserState.initial();
        }
        currentState = currentState.update();
        if (currentState == null) {
            currentState = UserState.initial();
        }
        return currentState;
    }

    public void requestReceived() {
        currentState = getState().requestReceived();
    }

    public void solveChallenge() {
        currentState = getState().solveChallenge();
    }

    @Nullable
    public ClientChallenge getChallenge() {
        if (isEmpty()) return null;
        return currentState.challenge();
    }

}
