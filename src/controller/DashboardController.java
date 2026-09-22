package controller;

import enums.Result;
import enums.ServiceResult;
import io.javalin.Javalin;
import io.javalin.http.Context;
import service.DashboardService;
import service.RoundService;

// Koordinatoren for dashboard.html, dashboardtom.html, start-runde.html og rundehistorik.html.
// Ruterne er én linje hver. Metoderne læser formularen, kalder service og sender brugeren videre – ingen DAO'er her.
public class DashboardController {

    public static void registerRoutes(Javalin app) {
        app.post("/opret-forloeb", ctx -> createJourney(ctx)); // "Start dit forløb" på dashboardtom.html (US1)
        app.post("/start-runde", ctx -> startRound(ctx));      // "Start runde" på start-runde.html (US10a)
        app.post("/afslut-runde", ctx -> endRound(ctx));       // bekræft i dialogen på dashboard.html (US10b)
        // GET /dashboard og GET /rundehistorik (vis data fra databasen) kommer med templates
    }

    // POST /opret-forloeb
    private static void createJourney(Context ctx) {
        // åbn kuverten: startdatoen fra formularen
        String startDate = ctx.formParam("startDate");
        int patientId = 1; // TODO: fra session, når login husker hvem der er logget ind

        // bed service oprette forløbet – den kender reglen "kun ét aktivt"
       ServiceResult result = new DashboardService().createJourney(patientId, startDate);

        switch (result) {
            case OK, ALREADY_EXISTS -> ctx.redirect("/dashboard.html");     // der findes nu et aktivt forløb
            case INVALID_INPUT -> ctx.redirect("/dashboardtom.html?fejl=dato");
            default -> ctx.redirect("/dashboardtom.html?fejl=ukendt");
        }
    }

    // POST /start-runde
    private static void startRound(Context ctx) {
        // åbn kuverten: behandlingstype ("IVF", "ICSI", "IUI", "FET" = value i dropdownen) og startdato
        String type = ctx.formParam("type");
        String startDate = ctx.formParam("startDate");
        int patientId = 1; // TODO: fra session

        ServiceResult result = new RoundService().startRound(patientId, type, startDate);

        // vælg side ud fra svaret
        switch (result) {
            case OK -> ctx.redirect("/dashboard.html");
            case NO_ACTIVE_JOURNEY -> ctx.redirect("/dashboardtom.html");
            case ROUND_IN_PROGRESS -> ctx.redirect("/dashboard.html?fejl=runde-i-gang");
            case INVALID_INPUT -> ctx.redirect("/start-runde.html?fejl=felter");
            default -> ctx.redirect("/dashboard.html?fejl=ukendt");
        }

    }

    // POST /afslut-runde
    private static void endRound(Context ctx) {
        // resultatet er valgfrit: "POSITIVE", "NEGATIVE" eller tomt (kan udfyldes senere)
        String resultParam = ctx.formParam("result");
        Result result = (resultParam == null || resultParam.isBlank()) ? null : Result.valueOf(resultParam);
        int patientId = 1; // TODO: fra session

        ServiceResult outcome = new RoundService().endRound(patientId, result);

        switch (outcome) {
            case OK -> ctx.redirect("/rundehistorik.html");
            case NO_ACTIVE_ROUND -> ctx.redirect("/dashboard.html?fejl=ingen-runde");
            default -> ctx.redirect("/dashboard.html?fejl=ukendt");
        }
    }
}
