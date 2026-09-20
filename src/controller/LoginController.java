package controller;

import dao.DatabaseConnection;
import dao.UserAccountDAO;
import entities.UserAccount;
import io.javalin.Javalin;

// Koordinatoren for login.html. Modtager formularen, spørger DAO'en og beslutter, hvor brugeren sendes hen.
public class LoginController {

    // Skriver ruterne på Javalins liste. Kaldes én gang fra Main: LoginController.registerRoutes(app)
    public static void registerRoutes(Javalin app) {

        // formularen på login.html sender hertil (action="/login" method="post")
        app.post("/login", ctx -> {
            // 1. åbn kuverten: læs felterne fra formularen (name="username" og name="password" i HTML)
            String username = ctx.formParam("username");
            String password = ctx.formParam("password");

            // 2. lav arkivaren og bed den finde kortet – et almindeligt metodekald, ingen SQL her
            UserAccountDAO dao = new UserAccountDAO(DatabaseConnection.getConnection());
            UserAccount user = dao.findByUsername(username); // kortet, eller null hvis brugeren ikke findes

            // 3. beslut: findes kortet, og passer kodeordet? (senere: BCrypt.checkpw i stedet for equals)
            if (user == null || !password.equals(user.getPasswordHash())) {
                ctx.redirect("/login.html");      // nej -> tilbage til login
            } else {
                ctx.redirect("/dashboard.html");  // ja -> ind på dashboard
            }
        });
    }
}
