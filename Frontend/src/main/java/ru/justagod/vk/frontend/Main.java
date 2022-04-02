package ru.justagod.vk.frontend;

import com.google.gson.Gson;
import ru.justagod.vk.EnvHelper;
import ru.justagod.vk.data.GsonHolder;
import ru.justagod.vk.frontend.gui.UltraForm;
import ru.justagod.vk.frontend.http.HttpClient;

import javax.swing.*;

public class Main {

    public static final Gson gson = GsonHolder.gson;

    private static String serverUrl() {
        return EnvHelper.stringEnv("ru.justagod.vk.client.server_url", "http://localhost:8888");
    }
    public static String pollAddress() {
        return EnvHelper.stringEnv("ru.justagod.vk.client.poll.address", "localhost");
    }
    public static int pollPort() {
        return EnvHelper.intEnv("ru.justagod.vk.client.poll.address", 9999);
    }


    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        HttpClient client = new HttpClient(gson, serverUrl());
        UltraForm form = new UltraForm(client);
        form.setVisible(true);
    }
}
