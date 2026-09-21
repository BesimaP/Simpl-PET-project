package controller;

import io.javalin.Javalin;
import io.javalin.http.Context;

// Koordinatoren for min-profil.html (US6b). Læser formularer, kalder ProfileService, vælger side.
public class ProfileController {

    public static void registerRoutes(Javalin app) {
        // app.post("/min-profil", ctx -> updateName(ctx));       formular 1
        // app.post("/skift-kodeord", ctx -> changePassword(ctx)); formular 2
        // app.post("/slet-konto", ctx -> deleteAccount(ctx));     dialogen
    }

    private static void updateName(Context ctx) {
        // 1. String name = ctx.formParam("name"); int patientId = 1; // TODO session
        // 2. ServiceResult result = new ProfileService().updateName(patientId, name);
        // 3. switch: OK -> /min-profil.html?gemt=1 · INVALID_INPUT -> ?fejl=felter · default -> ?fejl=ukendt
    }

    private static void changePassword(Context ctx) {
        // 1. læs "current-password", "new-password", "repeat-password"; int accountId = 1; // TODO session
        // 2. ServiceResult result = new ProfileService().changePassword(accountId, current, newPw, repeat);
        // 3. switch: OK -> /min-profil.html?kodeord=1 · INVALID_INPUT -> ?fejl=kodeord · default -> ?fejl=ukendt
    }

    private static void deleteAccount(Context ctx) {
        // 1. int accountId = 1; // TODO session
        // 2. new ProfileService().deleteAccount(accountId);
        // 3. ctx.redirect("/login.html");   (senere: også slet session)
    }
}
