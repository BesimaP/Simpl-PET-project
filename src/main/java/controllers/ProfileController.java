package controllers;

import enums.ServiceResult;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import persistence.ConnectionPool;
import services.ProfileService;

// Koordinatoren for min-profil (US6b).
// Ruterne er én linje hver. Metoderne læser formularen, kalder ProfileService og sender brugeren videre – ingen mappers her.
public class ProfileController {
    private ProfileService profileService; // "den der bestemmer" for min-profil

    public ProfileController(ConnectionPool connectionPool) {
        this.profileService = new ProfileService(connectionPool);
    }

    public void setRoutes(JavalinConfig config) {
        config.routes.get("/min-profil", ctx -> showProfile(ctx));         // vis siden med navnet forudfyldt (Thymeleaf)
        config.routes.post("/min-profil", ctx -> updateProfile(ctx));      // formular "Profil" (navn + fødselsdato)
        config.routes.post("/skift-kodeord", ctx -> changePassword(ctx));  // formular "Skift kodeord"
        config.routes.get("/slet-konto", ctx -> showDeleteAccount(ctx));   // bekræft-side – bruges, når JavaScript er slået fra
        config.routes.post("/slet-konto", ctx -> deleteAccount(ctx));      // bekræft-knappen (i dialogen eller på bekræft-siden)
    }

    // GET /min-profil – hent patientens kort og fyld skabelonen
    private void showProfile(Context ctx) {
        // hvem er logget ind? (sat i sessionen ved login) – null = ikke logget ind
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        ctx.attribute("patient", profileService.getPatient(patientId));   // requestscope -> ${patient.name}
        // besked fra sidste POST (?gemt= / ?fejl= i URL'en) -> request scope -> fragmentet besked.html
        ctx.attribute("gemt", ctx.queryParam("gemt"));
        ctx.attribute("fejl", ctx.queryParam("fejl"));
        ctx.render("min-profil");                                              // templates/min-profil.html
    }

    // GET /slet-konto – bekræft-side uden JavaScript. Med JavaScript åbner min-profil.js i stedet dialogen på min-profil.
    // Siden er skabelonen bekraeft.html, som også bruges til "Afslut runde" og "Afslut forløb"
    private void showDeleteAccount(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        ctx.attribute("title", "Slet din konto?");
        ctx.attribute("text", "Alle dine data – forløb, runder, noter og dokumenter – slettes og kan ikke gendannes.");
        ctx.attribute("action", "/slet-konto");      // hvor "Slet konto"-knappen sender hen
        ctx.attribute("button", "Slet konto");
        ctx.attribute("cancel", "/min-profil");      // "Annullér" går tilbage hertil
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

        // åbn kuverten: det nye for- og efternavn og fødselsdatoen
        String firstName = ctx.formParam("firstName");
        String lastName = ctx.formParam("lastName");
        String dateOfBirth = ctx.formParam("dateOfBirth");

        // bed service rette profilen – den tjekker tomme felter og at datoen er gyldig
        ServiceResult result = profileService.updateProfile(patientId, firstName, lastName, dateOfBirth);

        // vælg side ud fra svaret
        // navnet i sessionen bruges til forbogstavet i avataren – opdatér det også
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
        // patientens id (sat i sessionen ved login) – kodeordet ligger på patienten
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }

        // åbn kuverten: gammelt kodeord, nyt kodeord og nyt kodeord gentaget (name-attributter i min-profil.html)
        String currentPassword = ctx.formParam("current-password");
        String newPassword = ctx.formParam("new-password");
        String repeatPassword = ctx.formParam("repeat-password");

        // bed service skifte kodeord – den tjekker tomme felter, at de to nye er ens, og at det gamle passer
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

        // bed service slette kontoen = patienten (alt under den ryger med via CASCADE)
        profileService.deleteAccount(patientId);

        // kontoen findes ikke mere: glem sessionen, og tilbage til login
        ctx.req().getSession().invalidate();
        ctx.redirect("/login");
    }
}