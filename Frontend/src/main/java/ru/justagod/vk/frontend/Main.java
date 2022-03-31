package ru.justagod.vk.frontend;

import com.google.gson.Gson;
import ru.justagod.vk.data.GsonHolder;
import ru.justagod.vk.frontend.gui.UltraForm;
import ru.justagod.vk.frontend.http.HttpClient;

import javax.swing.*;

public class Main {

    public static final Gson gson = GsonHolder.gson;
    public static final HttpClient client = new HttpClient(gson, "http://localhost:8888");

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        UltraForm form = new UltraForm();
        form.setVisible(true);
    }
}
