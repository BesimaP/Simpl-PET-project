package controller;

import enums.ServiceResult;
import io.javalin.Javalin;
import io.javalin.http.Context;
import service.DiagnosisService;

// Koordinatoren for diagnoser.html (US7). Læser formularen, kalder DiagnosisService og sender brugeren videre.
public class DiagnosisController {

    // Skriver ruterne på Javalins liste. Kaldes én gang fra Main: DiagnosisController.registerRoutes(app)
    public static void registerRoutes(Javalin app) {
        // "når der kommer POST til /diagnoser (formularen på diagnoser.html), så kald addDiagnosis med den ctx, Javalin rækker os"
        app.post("/diagnoser", ctx -> addDiagnosis(ctx));
    }

    // POST /diagnoser – når brugeren trykker "Tilføj diagnose". ctx = kuverten fra Javalin
    public static void addDiagnosis(Context ctx) {
        // 1. åbn kuverten: læs de to felter (name="diagnosis", "description" i diagnoser.html)
        String name = ctx.formParam("diagnosis");         // fx "PCOS"
        String description = ctx.formParam("description"); // må være tom
        int patientId = 1; // TODO: fra session, når login husker hvem der er logget ind

        // 2. bed service gemme diagnosen – den hænger direkte på patienten, så ingen forløb/runde at finde
        ServiceResult result = new DiagnosisService().addDiagnosis(patientId, name, description);

        // 3. vælg side ud fra svaret – ét case per udfald
        switch (result) {
            case OK -> ctx.redirect("/diagnoser.html");
            case INVALID_INPUT -> ctx.redirect("/diagnoser.html?fejl=felter"); // navnet var tomt
            default -> ctx.redirect("/diagnoser.html?fejl=ukendt");
        }
    }
}
