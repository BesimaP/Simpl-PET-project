package controllers;

import enums.Result;
import enums.ServiceResult;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import services.DashboardService;
import services.RoundService;

// Koordinatoren for dashboard.html, dashboardtom.html, start-runde.html og rundehistorik.html.
// Ruterne er én linje hver. Metoderne læser formularen, kalder service og sender brugeren videre – ingen DAO'er her.
public class DashboardController {

    public static void setRoutes(JavalinConfig config) {
        config.routes.post("/opret-forloeb", ctx -> createJourney(ctx)); // "Start dit forløb" på dashboardtom.html (US1)
        config.routes.post("/start-runde", ctx -> startRound(ctx));      // "Start runde" på start-runde.html (US10a)
        config.routes.post("/afslut-runde", ctx -> endRound(ctx));       // bekræft i dialogen på dashboard.html (US10b)
        config.routes.get("/rundehistorik", ctx -> showRounds(ctx));
        // GET /dashboard kommer med templates
    }

    // GET /rundehistorik – hent alle runder og fyld skabelonen
    private static void showRounds(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        ctx.attribute("rounds", new RoundService().getRounds(patientId));   // requestscope -> ${rounds}
        ctx.render("rundehistorik");
    }

    // POST /opret-forloeb
    private static void createJourney(Context ctx) {
        // åbn kuverten: startdatoen fra formularen
        String startDate = ctx.formParam("startDate");

        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }

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
        Integer patientId = ctx.sessionAttribute("patientId");

        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }

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

        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }

        ServiceResult outcome = new RoundService().endRound(patientId, result);

        switch (outcome) {
            case OK -> ctx.redirect("/rundehistorik");
            case NO_ACTIVE_ROUND -> ctx.redirect("/dashboard.html?fejl=ingen-runde");
            default -> ctx.redirect("/dashboard.html?fejl=ukendt");
        }
    }
}
