package ru.justagod.vk.servlet;

import com.google.gson.stream.JsonReader;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ru.justagod.vk.Main;
import ru.justagod.vk.data.AuthRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        InputStream input = req.getInputStream();

        AuthRequest data = Main.gson.fromJson(new JsonReader(new InputStreamReader(input)), AuthRequest.class);

        if (data.login() == null || data.password() == null) {
            resp.setStatus(400);
            return;
        }


    }

}
