package ru.justagod.vk.backend.dos;

public class ClientChallenge {
    private final String challenge;
    private final int answer;

    public static ClientChallenge generate() {
        int a = (int) (Math.random() * 300);
        int b = (int) (Math.random() * 300);

        String challenge = String.format("%d + %d =", a, b);
        int answer = a + b;

        return new ClientChallenge(challenge, answer);
    }

    private ClientChallenge(String challenge, int answer) {
        this.challenge = challenge;
        this.answer = answer;
    }

    public String getChallenge() {
        return challenge;
    }

    public int getAnswer() {
        return answer;
    }
}
