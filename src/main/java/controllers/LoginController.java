package controllers;

import enums.ServiceResult;
import exceptions.UserNotFoundException;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import services.AuthService;
import entities.Patient;

// Koordinatoren for login.html og opretprofil.html.
// Ruterne er én linje hver og peger på en metode nedenunder. Al logik ligger i AuthService.
public class LoginController {

    // Skriver ruterne på Javalins liste. Kaldes én gang fra Main: LoginController.setRoutes(config)
    public static void setRoutes(JavalinConfig config) {
        // ctx -> login(ctx) = "kald metoden login med den ctx, Javalin rækker os";
        config.routes.get("/login", ctx -> ctx.render("login"));
        config.routes.post("/login", ctx -> login(ctx));
        config.routes.post("/opretprofil", ctx -> createProfile(ctx));
    }

    // POST /login – læs kuverten, spørg service, send brugeren videre
    private static void login(Context ctx) {
        // 1. åbn kuverten: de to felter (name="username", "password" i login.html)
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");

        try {
            // 2. spørg service. Svaret er patientens kort – eller en UserNotFoundException, som fanges nedenfor
            Patient patient = new AuthService().login(username, password);

            // 3. sessionscope: husk hvem der er logget ind, til browseren lukkes. Alle andre controllere læser herfra
            ctx.sessionAttribute("patientId", patient.getId());              // hvem er patienten
            ctx.sessionAttribute("accountId", patient.getUserAccountId());   // hvilken konto (til kodeord/slet på min profil)

            // 4. ind på dashboard
            ctx.redirect("/dashboard");

        } catch (UserNotFoundException e) {
            // requestscope: gælder kun for dette ene svar. Thymeleaf viser det som ${error} på login-siden
            ctx.attribute("error", e.getMessage());
            ctx.render("login");
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
            case OK -> {
                // profilen findes nu – log brugeren ind med det samme, så hun ikke skal skrive det hele igen
                try {
                    Patient patient = new AuthService().login(username, password);
                    ctx.sessionAttribute("patientId", patient.getId());
                    ctx.sessionAttribute("accountId", patient.getUserAccountId());
                    ctx.redirect("/dashboard");
                } catch (UserNotFoundException e) {
                    ctx.redirect("/login"); // burde ikke ske – men så må hun logge ind manuelt
                }
            }
            case ALREADY_EXISTS -> ctx.redirect("/opretprofil.html?fejl=brugernavn"); // brugernavnet er optaget
            case INVALID_INPUT -> ctx.redirect("/opretprofil.html?fejl=felter");      // tomt felt eller ugyldig dato
            default -> ctx.redirect("/opretprofil.html?fejl=ukendt");
        }
    }
}
