package controllers;

import enums.ServiceResult;
import exceptions.UserNotFoundException;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import persistence.ConnectionPool;
import services.AuthService;
import entities.Patient;

// Koordinatoren for login.html og opretprofil.html.
// Ruterne er én linje hver og peger på en metode nedenunder. Al logik ligger i AuthService.
public class LoginController {
    private AuthService authService;

    public LoginController(ConnectionPool connectionPool) {
        this.authService = new AuthService(connectionPool);
    }

    // Skriver ruterne på Javalins liste. Kaldes én gang fra RouteConfig: new LoginController(connectionPool).setRoutes(config)
    public void setRoutes(JavalinConfig config) {
        // ctx -> login(ctx) = "kald metoden login med den ctx, Javalin rækker os";
        config.routes.get("/login", ctx -> ctx.render("login"));
        config.routes.post("/login", ctx -> login(ctx));
        config.routes.get("/opretprofil", ctx -> showCreateProfile(ctx));   // siden er nu en Thymeleaf-template (fejlbesked uden JavaScript)
        config.routes.post("/opretprofil", ctx -> createProfile(ctx));
        config.routes.get("/logout", ctx -> logout(ctx));
    }

    // GET /opretprofil – vis formularen. ?fejl= fra sidste forsøg -> request scope -> ${fejl} i opretprofil.html
    private void showCreateProfile(Context ctx) {
        ctx.attribute("fejl", ctx.queryParam("fejl"));
        ctx.render("opretprofil");
    }

    // GET /logout – glem hvem der er logget ind og send til login. Bruges af "Log ud" i menuen
    private void logout(Context ctx) {
        ctx.req().getSession().invalidate();   // sletter hele sessionen (patientId og patientName)
        ctx.redirect("/login");
    }

    // POST /login – læs kuverten, spørg service, send brugeren videre
    private void login(Context ctx) {
        // 1. åbn kuverten: de to felter (name="username", "password" i login.html)
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");

        try {
            // 2. spørg service. Svaret er patientens kort – eller en UserNotFoundException, som fanges nedenfor
            Patient patient = authService.login(username, password);

            // 3. sessionscope: husk hvem der er logget ind, til browseren lukkes. Alle andre controllere læser herfra
            ctx.sessionAttribute("patientId", patient.getId());              // hvem er patienten
            ctx.sessionAttribute("patientName", patient.getName());

            // 4. ind på dashboard
            ctx.redirect("/dashboard");

        } catch (UserNotFoundException e) {
            // requestscope: gælder kun for dette ene svar. Thymeleaf viser det som ${error} på login-siden
            ctx.attribute("error", e.getMessage());
            ctx.render("login");
        }
    }


    // POST /opretprofil – læs kuverten, spørg service, send brugeren videre
    private void createProfile(Context ctx) {
        // 1. åbn kuverten: felterne fra opretprofil.html
        String firstName = ctx.formParam("firstName");
        String lastName = ctx.formParam("lastName");
        String dateOfBirth = ctx.formParam("dateOfBirth");
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");
        String hasJourney = ctx.formParam("hasJourney");     // "yes" eller "no" (radio-knapperne)
        String journeyStart = ctx.formParam("journeyStart"); // startdato for forløbet – kun brugt ved "yes"

        // 2. bed service oprette patienten (+ forløb ved "yes"). Svar: OK, ellers hvad der gik galt
        ServiceResult result = authService.createProfile(firstName, lastName, dateOfBirth, username, password, hasJourney, journeyStart);

        // 3. vælg side ud fra svaret – ét case per udfald
        switch (result) {
            case OK -> {
                // profilen findes nu – log brugeren ind med det samme, så hun ikke skal skrive det hele igen
                try {
                    Patient patient = authService.login(username, password);
                    ctx.sessionAttribute("patientId", patient.getId());
                    ctx.sessionAttribute("patientName", patient.getName());
                    ctx.redirect("/dashboard?gemt=oprettet");
                } catch (UserNotFoundException e) {
                    ctx.redirect("/login"); // burde ikke ske – men så må hun logge ind manuelt
                }
            }
            case ALREADY_EXISTS -> ctx.redirect("/opretprofil?fejl=brugernavn"); // brugernavnet er optaget
            case INVALID_INPUT -> ctx.redirect("/opretprofil?fejl=felter");      // tomt felt eller ugyldig dato
            default -> ctx.redirect("/opretprofil?fejl=ukendt");
        }
    }
}
