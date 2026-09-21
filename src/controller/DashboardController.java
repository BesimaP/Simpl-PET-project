package controller;

import enums.Result;
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

        // tom dato? (feltet har required, men serveren stoler aldrig blindt på browseren) -> tilbage til siden
        if (startDate == null || startDate.isBlank()) {
            ctx.redirect("/dashboardtom.html?fejl=dato");
            return;
        }

        // bed service oprette forløbet – den kender reglen "kun ét aktivt"
        new DashboardService().createJourney(patientId, startDate);

        // ind på dashboard – uanset om der blev oprettet et nyt, findes der nu et aktivt forløb
        ctx.redirect("/dashboard.html");
    }

    // POST /start-runde
    private static void startRound(Context ctx) {
        // åbn kuverten: behandlingstype ("IVF", "ICSI", "IUI", "FET" = value i dropdownen) og startdato
        String type = ctx.formParam("type");
        String startDate = ctx.formParam("startDate");
        int patientId = 1; // TODO: fra session

        // bed service starte runden – svaret er null (ok) eller en fejlkode
        String fejl = new RoundService().startRound(patientId, type, startDate);

        // vælg side ud fra svaret
        if ("intet-forloeb".equals(fejl)) {
            ctx.redirect("/dashboardtom.html");              // intet forløb -> opret et først
        } else if (fejl != null) {
            ctx.redirect("/dashboard.html?fejl=" + fejl);    // fx runde-i-gang
        } else {
            ctx.redirect("/dashboard.html");                 // ok
        }
    }

    // POST /afslut-runde
    private static void endRound(Context ctx) {
        // resultatet er valgfrit: "POSITIVE", "NEGATIVE" eller tomt (kan udfyldes senere)
        String resultParam = ctx.formParam("result");
        Result result = (resultParam == null || resultParam.isBlank()) ? null : Result.valueOf(resultParam);
        int patientId = 1; // TODO: fra session

        // bed service afslutte runden – svaret er null (ok) eller en fejlkode
        String fejl = new RoundService().endRound(patientId, result);

        if (fejl != null) {
            ctx.redirect("/dashboard.html?fejl=" + fejl);    // fx ingen-runde
        } else {
            ctx.redirect("/rundehistorik.html");             // ok -> historikken, hvor runden nu står som afsluttet
        }
    }
}
