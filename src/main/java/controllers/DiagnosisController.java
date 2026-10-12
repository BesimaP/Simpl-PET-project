package controllers;

import enums.ServiceResult;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import persistence.ConnectionPool;
import services.DiagnosisService;

// Controller for /diagnoser (US7). Læser formularen, kalder DiagnosisService og sender brugeren videre.
public class DiagnosisController {
    private DiagnosisService diagnosisService;

    public DiagnosisController(ConnectionPool connectionPool) {
        this.diagnosisService = new DiagnosisService(connectionPool);
    }

    // bliver kaldt én gang fra RouteConfig
    public void setRoutes(JavalinConfig config) {
        config.routes.post("/diagnoser", ctx -> addDiagnosis(ctx));
        config.routes.get("/diagnoser", ctx -> showDiagnoses(ctx));
        config.routes.post("/diagnoser/slet", ctx -> deleteDiagnosis(ctx));   // "Slet" på én diagnose
    }

    // GET /diagnoser
    private void showDiagnoses(Context ctx) {
        // Integer og ikke int, fordi den er null, hvis ingen er logget ind
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        ctx.attribute("diagnoses", diagnosisService.getDiagnoses(patientId));

        // ?gemt= / ?fejl= fra sidste POST vises af fragmentet besked.html
        ctx.attribute("gemt", ctx.queryParam("gemt"));
        ctx.attribute("fejl", ctx.queryParam("fejl"));
        ctx.render("diagnoser");
    }

    // POST /diagnoser – når brugeren trykker "Gem diagnose"
    private void addDiagnosis(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }

        String name = ctx.formParam("diagnosis");
        String description = ctx.formParam("description"); // må være tom

        // diagnoser hænger direkte på patienten, så der kræves hverken forløb eller runde
        ServiceResult result = diagnosisService.addDiagnosis(patientId, name, description);

        switch (result) {
            case OK -> ctx.redirect("/diagnoser?gemt=1");
            case INVALID_INPUT -> ctx.redirect("/diagnoser?fejl=felter"); // navnet var tomt
            default -> ctx.redirect("/diagnoser?fejl=ukendt");
        }
    }

    // POST /diagnoser/slet – id kommer fra et skjult felt. Service tjekker, at diagnosen er patientens egen
    private void deleteDiagnosis(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        int diagnosisId;
        try {
            diagnosisId = Integer.parseInt(ctx.formParam("id"));
        } catch (NumberFormatException e) {
            ctx.redirect("/diagnoser?fejl=ukendt");
            return;
        }
        ServiceResult result = diagnosisService.deleteDiagnosis(patientId, diagnosisId);
        ctx.redirect(result == ServiceResult.OK ? "/diagnoser?gemt=slettet" : "/diagnoser?fejl=ukendt");
    }
}
