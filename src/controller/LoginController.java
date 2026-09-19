package controller;

import io.javalin.Javalin;

public class LoginController {

    public static void registerRoutes(Javalin app) {
        // formularen på login.html sender hertil (action="/login" method="post")
        app.post("/login", ctx -> {
            String username = ctx.formParam("username");
            String password = ctx.formParam("password");
            System.out.println("Login forsøgt med: " + username);
            ctx.redirect("/dashboard.html");
        });
    }
}