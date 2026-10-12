package controllers;

import enums.HormoneType;
import enums.ServiceResult;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import persistence.ConnectionPool;
import services.HormoneService;
import services.RoundService;

// Controller for hormoner.html (US9). Læser formularen, kalder HormoneService og sender brugeren videre.
// SQL og mappers ligger i service- og persistence-laget.
public class HormoneController {
    private HormoneService hormoneService;
    private RoundService roundService;     // kun til rundens startdato (min på datofeltet)

    public HormoneController(ConnectionPool connectionPool) {
        this.hormoneService = new HormoneService(connectionPool);
        this.roundService = new RoundService(connectionPool);
    }

    // bliver kaldt én gang fra RouteConfig
    public void setRoutes(JavalinConfig config) {
        config.routes.post("/hormoner", ctx -> saveLog(ctx));
        config.routes.get("/hormoner", ctx -> showLogs(ctx));
        config.routes.post("/hormoner/slet", ctx -> deleteLog(ctx));   // "Slet" på én måling
    }

    // GET /hormoner – målingerne i den aktive runde
    public void showLogs(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        ctx.attribute("logs", hormoneService.getLogs(patientId));

        // ?hormon=E2_OESTRADIOL vælger hormonet til kurven. Er den tom eller ukendt, bruger vi den nyeste måling
        HormoneType chosen = null;
        try {
            if (ctx.queryParam("hormon") != null) {
                chosen = HormoneType.valueOf(ctx.queryParam("hormon"));
            }
        } catch (IllegalArgumentException e) {
            // ukendt hormon i URL'en – vi viser standard
        }
        ctx.attribute("curve", hormoneService.getCurve(patientId, chosen));
        ctx.attribute("units", HormoneService.UNITS);                  // samme liste, som service tjekker imod
        ctx.attribute("hormoneTypes", HormoneType.values());           // knapperne over kurven
        ctx.attribute("roundStart", roundService.getActiveRoundStart(patientId)); // min på datofeltet (null = ingen runde)
        // ?gemt= / ?fejl= fra sidste POST vises af fragmentet besked.html
        ctx.attribute("gemt", ctx.queryParam("gemt"));
        ctx.attribute("fejl", ctx.queryParam("fejl"));
        ctx.render("hormoner");
    }

    // POST /hormoner – når brugeren trykker "Gem måling"
    public void saveLog(Context ctx) {
        String hormone = ctx.formParam("hormone");
        String value = ctx.formParam("value");
        String unit = ctx.formParam("unit");
        String date = ctx.formParam("date");

        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }

        ServiceResult result = hormoneService.saveLog(patientId, hormone, value, unit, date);

        switch (result) {
            case OK -> ctx.redirect("/hormoner?gemt=1");
            case INVALID_INPUT -> ctx.redirect("/hormoner?fejl=felter");
            case NO_ACTIVE_JOURNEY -> ctx.redirect("/hormoner?fejl=intet-forloeb");
            case NO_ACTIVE_ROUND -> ctx.redirect("/hormoner?fejl=ingen-runde");
            default -> ctx.redirect("/hormoner?fejl=ukendt");
        }
    }

    // POST /hormoner/slet – id kommer fra et skjult felt. Service tjekker, at målingen er patientens egen
    private void deleteLog(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        int logId;
        try {
            logId = Integer.parseInt(ctx.formParam("id"));
        } catch (NumberFormatException e) {
            ctx.redirect("/hormoner?fejl=ukendt");
            return;
        }
        ServiceResult result = hormoneService.deleteLog(patientId, logId);
        ctx.redirect(result == ServiceResult.OK ? "/hormoner?gemt=slettet" : "/hormoner?fejl=ukendt");
    }
}
