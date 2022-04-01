package ru.justagod.vk.frontend;

import com.google.gson.Gson;
import ru.justagod.vk.data.GsonHolder;
import ru.justagod.vk.frontend.gui.UltraForm;
import ru.justagod.vk.frontend.http.HttpClient;

import javax.swing.*;

public class Main {

    public static final Gson gson = GsonHolder.gson;

    private static String serverUrl() {
        String result = "http://localhost:8888";
        String override = System.getenv("ru.justagod.vk.client.server_url");
        if (override != null) result = override;
        return result;
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
