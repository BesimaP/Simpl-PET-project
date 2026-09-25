package controllers;

import enums.ServiceResult;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import services.ProfileService;

// Koordinatoren for min-profil (US6b).
// Ruterne er én linje hver. Metoderne læser formularen, kalder ProfileService og sender brugeren videre – ingen DAO'er her.
public class ProfileController {

    public static void setRoutes(JavalinConfig config) {
        config.routes.get("/min-profil", ctx -> showProfile(ctx));         // vis siden med navnet forudfyldt (Thymeleaf)
        config.routes.post("/min-profil", ctx -> updateName(ctx));         // formular "Ret navn"
        config.routes.post("/skift-kodeord", ctx -> changePassword(ctx));  // formular "Skift kodeord"
        config.routes.post("/slet-konto", ctx -> deleteAccount(ctx));      // bekræft-knappen i slet-dialogen
    }

    // GET /min-profil – hent patientens kort og fyld skabelonen
    private static void showProfile(Context ctx) {
        // hvem er logget ind? (sat i sessionen ved login) – null = ikke logget ind
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        ctx.attribute("patient", new ProfileService().getPatient(patientId));   // requestscope -> ${patient.name}
        // besked fra sidste POST (?gemt= / ?fejl= i URL'en) -> request scope -> fragmentet besked.html
        ctx.attribute("gemt", ctx.queryParam("gemt"));
        ctx.attribute("fejl", ctx.queryParam("fejl"));
        ctx.render("min-profil");                                              // templates/min-profil.html
    }

    // POST /min-profil – ret navn
    private static void updateName(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }

        // åbn kuverten: det nye navn
        String name = ctx.formParam("name");

        // bed service rette navnet – den tjekker, at feltet ikke er tomt
        ServiceResult result = new ProfileService().updateName(patientId, name);

        // vælg side ud fra svaret
        switch (result) {
            case OK -> ctx.redirect("/min-profil?gemt=1");
            case INVALID_INPUT -> ctx.redirect("/min-profil?fejl=felter");
            default -> ctx.redirect("/min-profil?fejl=ukendt");
        }
    }

    // POST /skift-kodeord
    private static void changePassword(Context ctx) {
        // kontoens id (sat i sessionen ved login) – kodeordet ligger på kontoen, ikke på patienten
        Integer accountId = ctx.sessionAttribute("accountId");
        if (accountId == null) {
            ctx.redirect("/login");
            return;
        }

        // åbn kuverten: gammelt kodeord, nyt kodeord og nyt kodeord gentaget (name-attributter i min-profil.html)
        String currentPassword = ctx.formParam("current-password");
        String newPassword = ctx.formParam("new-password");
        String repeatPassword = ctx.formParam("repeat-password");

        // bed service skifte kodeord – den tjekker tomme felter, at de to nye er ens, og at det gamle passer
        ServiceResult result = new ProfileService().changePassword(accountId, currentPassword, newPassword, repeatPassword);

        switch (result) {
            case OK -> ctx.redirect("/min-profil?gemt=kodeord");
            case INVALID_INPUT -> ctx.redirect("/min-profil?fejl=kodeord");
            default -> ctx.redirect("/min-profil?fejl=ukendt");
        }
    }

    // POST /slet-konto
    private static void deleteAccount(Context ctx) {
        Integer accountId = ctx.sessionAttribute("accountId");
        if (accountId == null) {
            ctx.redirect("/login");
            return;
        }

        // bed service slette kontoen (patient og alt under den ryger med via CASCADE)
        new ProfileService().deleteAccount(accountId);

        // kontoen findes ikke mere: glem sessionen, og tilbage til login
        ctx.req().getSession().invalidate();
        ctx.redirect("/login");
    }
}