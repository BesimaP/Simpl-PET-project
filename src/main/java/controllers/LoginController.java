package controllers;

import entities.UserAccount;
import enums.ServiceResult;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import services.AuthService;

// Koordinatoren for login.html og opretprofil.html.
// Ruterne er én linje hver og peger på en metode nedenunder. Al logik ligger i AuthService.
public class LoginController {

    // Skriver ruterne på Javalins liste. Kaldes én gang fra Main: LoginController.setRoutes(config)
    public static void setRoutes(JavalinConfig config) {
        // ctx -> login(ctx) = "kald metoden login med den ctx, Javalin rækker os"
        config.routes.post("/login", ctx -> login(ctx));              // formularen på login.html
        config.routes.post("/opretprofil", ctx -> createProfile(ctx)); // formularen på opretprofil.html
    }

    // POST /login – læs kuverten, spørg service, send brugeren videre
    private static void login(Context ctx) {
        // 1. åbn kuverten: de to felter (name="username", "password" i login.html)
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");

        // 2. spørg service. Svaret er selve kortet (UserAccount) eller null – ikke ServiceResult, fordi vi senere skal bruge user.getId() til sessionen
        UserAccount user = new AuthService().login(username, password);

        // 3. vælg side
        if (user == null) {
            ctx.redirect("/login.html");      // nej -> tilbage til login
        } else {
            ctx.redirect("/dashboard.html");  // ja -> ind på dashboard
        }
    }

    // POST /opretprofil – læs kuverten, spørg service, send brugeren videre
    private static void createProfile(Context ctx) {
        // 1. åbn kuverten: felterne fra opretprofil.html
        String name = ctx.formParam("name");
        String dateOfBirth = ctx.formParam("dateOfBirth");
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");
        String hasJourney = ctx.formParam("hasJourney");     // "yes" eller "no" (radio-knapperne)
        String journeyStart = ctx.formParam("journeyStart"); // startdato for forløbet – kun brugt ved "yes"

        // 2. bed service oprette konto + patient (+ forløb ved "yes"). Svar: OK, ellers hvad der gik galt
        ServiceResult result = new AuthService().createProfile(name, dateOfBirth, username, password, hasJourney, journeyStart);

        // 3. vælg side ud fra svaret – ét case per udfald
        switch (result) {
            case OK -> ctx.redirect("/dashboard.html");
            case ALREADY_EXISTS -> ctx.redirect("/opretprofil.html?fejl=brugernavn"); // brugernavnet er optaget
            case INVALID_INPUT -> ctx.redirect("/opretprofil.html?fejl=felter");      // tomt felt eller ugyldig dato
            default -> ctx.redirect("/opretprofil.html?fejl=ukendt");
        }
    }
}
