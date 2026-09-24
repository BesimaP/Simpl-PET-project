package controllers;

import enums.ServiceResult;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import services.ProfileService;

// Koordinatoren for min-profil.html (US6b).
// Ruterne er én linje hver. Metoderne læser formularen, kalder ProfileService og sender brugeren videre – ingen DAO'er her.
public class ProfileController {

    public static void setRoutes(JavalinConfig config) {
        config.routes.post("/min-profil", ctx -> updateName(ctx));        // formular "Ret navn" på min-profil.html
        config.routes.post("/skift-kodeord", ctx -> changePassword(ctx)); // formular "Skift kodeord" på min-profil.html
        config.routes.post("/slet-konto", ctx -> deleteAccount(ctx));     // bekræft-knappen i slet-dialogen
    }

    // POST /min-profil
    private static void updateName(Context ctx) {
        // åbn kuverten: det nye navn
        String name = ctx.formParam("name");
        int patientId = 1; // TODO: fra session, når login husker hvem der er logget ind

        // bed service rette navnet – den tjekker, at feltet ikke er tomt
        ServiceResult result = new ProfileService().updateName(patientId, name);

        // vælg side ud fra svaret
        switch (result) {
            case OK -> ctx.redirect("/min-profil.html?gemt=1");
            case INVALID_INPUT -> ctx.redirect("/min-profil.html?fejl=felter");
            default -> ctx.redirect("/min-profil.html?fejl=ukendt");
        }
    }

    // POST /skift-kodeord
    private static void changePassword(Context ctx) {
        // åbn kuverten: gammelt kodeord, nyt kodeord og nyt kodeord gentaget (name-attributter i min-profil.html)
        String currentPassword = ctx.formParam("current-password");
        String newPassword = ctx.formParam("new-password");
        String repeatPassword = ctx.formParam("repeat-password");
        int accountId = 1; // TODO: fra session

        // bed service skifte kodeord – den tjekker tomme felter, at de to nye er ens, og at det gamle passer
        ServiceResult result = new ProfileService().changePassword(accountId, currentPassword, newPassword, repeatPassword);

        // vælg side ud fra svaret
        switch (result) {
            case OK -> ctx.redirect("/min-profil.html?kodeord=1");
            case INVALID_INPUT -> ctx.redirect("/min-profil.html?fejl=kodeord");
            default -> ctx.redirect("/min-profil.html?fejl=ukendt");
        }
    }

    // POST /slet-konto
    private static void deleteAccount(Context ctx) {
        // ingen felter at læse – kun hvem der er logget ind
        int accountId = 1; // TODO: fra session

        // bed service slette kontoen (patient og alt under den ryger med via CASCADE)
        new ProfileService().deleteAccount(accountId);

        // kontoen findes ikke mere, så tilbage til login (senere: også slet sessionen)
        ctx.redirect("/login.html");
    }
}
