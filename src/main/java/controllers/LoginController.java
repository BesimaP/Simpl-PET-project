package controllers;

import enums.ServiceResult;
import exceptions.UserNotFoundException;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import persistence.ConnectionPool;
import services.AuthService;
import entities.Patient;

// Controller for login, log ud og opret profil. Selve logikken ligger i AuthService.
public class LoginController {
    private AuthService authService;

    public LoginController(ConnectionPool connectionPool) {
        this.authService = new AuthService(connectionPool);
    }

    // bliver kaldt én gang fra RouteConfig
    public void setRoutes(JavalinConfig config) {
        config.routes.get("/login", ctx -> ctx.render("login"));
        config.routes.post("/login", ctx -> login(ctx));
        config.routes.get("/opretprofil", ctx -> showCreateProfile(ctx));
        config.routes.post("/opretprofil", ctx -> createProfile(ctx));
        config.routes.post("/logout", ctx -> logout(ctx));   // POST, så en anden side ikke kan logge en ud via et link
    }

    // GET /opretprofil – ?fejl= fra sidste forsøg vises som ${fejl}
    private void showCreateProfile(Context ctx) {
        ctx.attribute("fejl", ctx.queryParam("fejl"));
        ctx.attribute("today", java.time.LocalDate.now());   // max på fødselsdato, så man ikke kan vælge en dato i fremtiden
        ctx.render("opretprofil");
    }

    // POST /logout – bruges af "Log ud" i menuen og på Min profil
    private void logout(Context ctx) {
        ctx.req().getSession().invalidate();   // fjerner både patientId og patientName
        ctx.redirect("/login");
    }

    // POST /login
    private void login(Context ctx) {
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");

        try {
            // forkert brugernavn eller kodeord giver en UserNotFoundException
            Patient patient = authService.login(username, password);

            // vi gemmer patienten i sessionen, så de andre controllere ved, hvem der er logget ind
            ctx.sessionAttribute("patientId", patient.getId());
            ctx.sessionAttribute("patientName", patient.getName());

            ctx.redirect("/dashboard");

        } catch (UserNotFoundException e) {
            // fejlen vises som ${fejl} på login-siden
            ctx.attribute("fejl", e.getMessage());
            ctx.attribute("username", username);   // så man kun skal skrive kodeordet igen
            ctx.render("login");
        }
    }


    // POST /opretprofil
    private void createProfile(Context ctx) {
        String firstName = ctx.formParam("firstName");
        String lastName = ctx.formParam("lastName");
        String dateOfBirth = ctx.formParam("dateOfBirth");
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");
        String hasJourney = ctx.formParam("hasJourney");     // "yes" eller "no" fra radioknapperne
        String journeyStart = ctx.formParam("journeyStart"); // bruges kun ved "yes"

        // service opretter patienten og ved "yes" også et forløb
        ServiceResult result = authService.createProfile(firstName, lastName, dateOfBirth, username, password, hasJourney, journeyStart);

        switch (result) {
            case OK -> {
                // vi logger brugeren ind med det samme, så hun ikke skal gøre det selv
                try {
                    Patient patient = authService.login(username, password);
                    ctx.sessionAttribute("patientId", patient.getId());
                    ctx.sessionAttribute("patientName", patient.getName());
                    ctx.redirect("/dashboard?gemt=oprettet");
                } catch (UserNotFoundException e) {
                    ctx.redirect("/login"); // burde ikke ske, men så må hun logge ind selv
                }
            }
            case ALREADY_EXISTS -> ctx.redirect("/opretprofil?fejl=brugernavn"); // brugernavnet er taget
            case INVALID_INPUT -> ctx.redirect("/opretprofil?fejl=felter");      // tomt felt eller ugyldig dato
            default -> ctx.redirect("/opretprofil?fejl=ukendt");
        }
    }
}
