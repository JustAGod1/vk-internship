package ru.justagod.vk.backend.dos;

import org.jetbrains.annotations.Nullable;
import ru.justagod.vk.backend.Main;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class DosProtection {

    private final Map<String, UserStateController> states = new ConcurrentHashMap<>();

    public static DosProtection create(ScheduledExecutorService service) {
        return new DosProtection(service);
    }

    private DosProtection(ScheduledExecutorService service) {
        cleanUpLoop(service);
    }

    private void cleanUpLoop(ScheduledExecutorService service) {
        states.values().removeIf(UserStateController::isEmpty);
        service.schedule(() -> cleanUpLoop(service), 1, TimeUnit.MINUTES);
    }

    private UserStateController getController(String ip) {
        return states.computeIfAbsent(ip, a -> new UserStateController());
    }

    @Nullable
    public ClientChallenge onRequest(String ip) {
        UserStateController controller = getController(ip);
        controller.requestReceived();

        return controller.getChallenge();
    }

    public void solveChallenge(String ip) {
        UserStateController controller = getController(ip);
        controller.solveChallenge();
    }


}
