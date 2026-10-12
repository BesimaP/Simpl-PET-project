package controllers;

import enums.ServiceResult;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import persistence.ConnectionPool;
import services.ProfileService;

// Controller for Min profil (US6b): ret profil, skift kodeord og slet konto.
public class ProfileController {
    private ProfileService profileService;

    public ProfileController(ConnectionPool connectionPool) {
        this.profileService = new ProfileService(connectionPool);
    }

    public void setRoutes(JavalinConfig config) {
        config.routes.get("/min-profil", ctx -> showProfile(ctx));
        config.routes.post("/min-profil", ctx -> updateProfile(ctx));      // navn og fødselsdato
        config.routes.post("/skift-kodeord", ctx -> changePassword(ctx));
        config.routes.get("/slet-konto", ctx -> showDeleteAccount(ctx));   // bekræft-side, hvis JavaScript er slået fra
        config.routes.post("/slet-konto", ctx -> deleteAccount(ctx));
    }

    // GET /min-profil
    private void showProfile(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        ctx.attribute("patient", profileService.getPatient(patientId));
        // ?gemt= / ?fejl= fra sidste POST vises af fragmentet besked.html
        ctx.attribute("gemt", ctx.queryParam("gemt"));
        ctx.attribute("fejl", ctx.queryParam("fejl"));
        ctx.attribute("today", java.time.LocalDate.now());   // max på fødselsdato, så man ikke kan vælge en dato i fremtiden
        ctx.render("min-profil");
    }

    // GET /slet-konto – bekræft-side, hvis JavaScript er slået fra (ellers åbner min-profil.js en dialog).
    // bekraeft.html bruges også til "Afslut runde" og "Afslut forløb"
    private void showDeleteAccount(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        ctx.attribute("title", "Slet din konto?");
        ctx.attribute("text", "Alle dine data – forløb, runder, noter og dokumenter – slettes og kan ikke gendannes.");
        ctx.attribute("action", "/slet-konto");
        ctx.attribute("button", "Slet konto");
        ctx.attribute("cancel", "/min-profil");
        ctx.attribute("showResult", false);          // kun "Afslut runde" spørger om resultat
        ctx.render("bekraeft");
    }

    // POST /min-profil – ret navn og fødselsdato
    private void updateProfile(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }

        String firstName = ctx.formParam("firstName");
        String lastName = ctx.formParam("lastName");
        String dateOfBirth = ctx.formParam("dateOfBirth");

        // service tjekker tomme felter og at datoen er gyldig
        ServiceResult result = profileService.updateProfile(patientId, firstName, lastName, dateOfBirth);

        // navnet i sessionen bruges til forbogstavet i avataren, så det skal også opdateres
        if (result == ServiceResult.OK) {
            ctx.sessionAttribute("patientName", firstName.trim() + " " + lastName.trim());
        }

        switch (result) {
            case OK -> ctx.redirect("/min-profil?gemt=1");
            case INVALID_INPUT -> ctx.redirect("/min-profil?fejl=felter");
            default -> ctx.redirect("/min-profil?fejl=ukendt");
        }
    }

    // POST /skift-kodeord
    private void changePassword(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }

        String currentPassword = ctx.formParam("current-password");
        String newPassword = ctx.formParam("new-password");
        String repeatPassword = ctx.formParam("repeat-password");

        // service tjekker tomme felter, at de to nye er ens, og at det gamle passer
        ServiceResult result = profileService.changePassword(patientId, currentPassword, newPassword, repeatPassword);

        switch (result) {
            case OK -> ctx.redirect("/min-profil?gemt=kodeord");
            case INVALID_INPUT -> ctx.redirect("/min-profil?fejl=kodeord");
            default -> ctx.redirect("/min-profil?fejl=ukendt");
        }
    }

    // POST /slet-konto
    private void deleteAccount(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }

        // vi sletter patienten, og resten af hendes data ryger med via ON DELETE CASCADE
        ServiceResult result = profileService.deleteAccount(patientId);
        if (result != ServiceResult.OK) {
            ctx.redirect("/min-profil?fejl=ukendt");   // kontoen blev ikke slettet, så hun forbliver logget ind
            return;
        }

        // kontoen er væk, så sessionen skal også væk
        ctx.req().getSession().invalidate();
        ctx.redirect("/login");
    }
}