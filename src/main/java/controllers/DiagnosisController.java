package controllers;

import enums.ServiceResult;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import org.jetbrains.annotations.NotNull;
import services.DiagnosisService;

// Koordinatoren for /diagnoser (US7). Læser formularen, kalder DiagnosisService og sender brugeren videre.
public class DiagnosisController {

    // Skriver ruterne på Javalins liste. Kaldes én gang fra Main: DiagnosisController.setRoutes(config)
    public static void setRoutes(JavalinConfig config) {
        // "når der kommer POST til /diagnoser (formularen på diagnoser.html), så kald addDiagnosis med den ctx, Javalin rækker os"
        config.routes.post("/diagnoser", ctx -> addDiagnosis(ctx));
        config.routes.get("/diagnoser", ctx -> showDiagnoses(ctx));
    }

    // GET /diagnoser – hent listen og fyld skabelonen
    private static void showDiagnoses(Context ctx) {
        // 1. hvem er logget ind? (sat i sessionen ved login). Integer, fordi den er null, hvis ingen er logget ind
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");   // ikke logget ind -> til login
            return;
        }

        // 2. bed service om listen, og læg den i requestscope – Thymeleaf læser den som ${diagnoses}
        ctx.attribute("diagnoses", new DiagnosisService().getDiagnoses(patientId));

        // 3. vis skabelonen templates/diagnoser.html
        // besked fra sidste POST (?gemt= / ?fejl= i URL'en) -> request scope -> fragmentet besked.html
        ctx.attribute("gemt", ctx.queryParam("gemt"));
        ctx.attribute("fejl", ctx.queryParam("fejl"));
        ctx.render("diagnoser");
    }

    // POST /diagnoser – når brugeren trykker "Gem diagnose". ctx = kuverten fra Javalin
    private static void addDiagnosis(Context ctx) {
        // 1. hvem er logget ind? (sat i sessionen ved login)
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }

        // 2. åbn kuverten: læs de to felter (name="diagnosis", "description" i diagnoser.html)
        String name = ctx.formParam("diagnosis");         // fx "PCOS"
        String description = ctx.formParam("description"); // må være tom

        // 3. bed service gemme diagnosen – den hænger direkte på patienten, så ingen forløb/runde at finde
        ServiceResult result = new DiagnosisService().addDiagnosis(patientId, name, description);

        // 4. vælg side ud fra svaret – redirect til RUTEN /diagnoser (ikke .html – filen ligger i templates nu)
        switch (result) {
            case OK -> ctx.redirect("/diagnoser?gemt=1");
            case INVALID_INPUT -> ctx.redirect("/diagnoser?fejl=felter"); // navnet var tomt
            default -> ctx.redirect("/diagnoser?fejl=ukendt");
        }
    }
}
