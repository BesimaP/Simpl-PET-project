package controller;

import entities.UserAccount;
import io.javalin.Javalin;
import io.javalin.http.Context;
import service.AuthService;

// Koordinatoren for login.html og opretprofil.html.
// Ruterne er én linje hver og peger på en metode nedenunder. Al logik ligger i AuthService.
public class LoginController {

    // Skriver ruterne på Javalins liste. Kaldes én gang fra Main: LoginController.registerRoutes(app)
    public static void registerRoutes(Javalin app) {
        // ctx -> login(ctx) = "kald metoden login med den ctx, Javalin rækker os"
        app.post("/login", ctx -> login(ctx));              // formularen på login.html
        app.post("/opretprofil", ctx -> createProfile(ctx)); // formularen på opretprofil.html
    }

    // POST /login – læs kuverten, spørg service, send brugeren videre
    private static void login(Context ctx) {
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");

        UserAccount user = new AuthService().login(username, password);

        if (user == null) {
            ctx.redirect("/login.html");      // nej -> tilbage til login
        } else {
            ctx.redirect("/dashboard.html");  // ja -> ind på dashboard
        }
    }

    // POST /opretprofil – læs kuverten, spørg service, send brugeren videre
    private static void createProfile(Context ctx) {
        String name = ctx.formParam("name");
        String dateOfBirth = ctx.formParam("dateOfBirth");
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");

        boolean ok = new AuthService().createProfile(name, dateOfBirth, username, password);

        if (!ok) {
            ctx.redirect("/opretprofil.html?fejl=brugernavn"); // brugernavnet er optaget
        } else {
            ctx.redirect("/dashboard.html");
        }
    }
}