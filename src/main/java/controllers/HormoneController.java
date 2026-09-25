package controllers;

import enums.ServiceResult;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import services.HormoneService;

// Koordinatoren for hormoner.html (US9). Læser formularen, kalder HormoneService og sender brugeren videre.
// Ingen SQL og ingen DAO'er her – det bor i service- og dao-laget.
public class HormoneController {

    // Skriver ruterne på Javalins liste. Kaldes én gang fra Main: HormoneController.setRoutes(config)
    public static void setRoutes(JavalinConfig config) {
        // "når der kommer POST til /hormoner (formularen på hormoner.html), så kald saveLog med den ctx, Javalin rækker os"
        config.routes.post("/hormoner", ctx -> saveLog(ctx));
        config.routes.get("/hormoner", ctx -> showLogs(ctx));
    }

    // GET /hormoner – hent rundens målinger og fyld skabelonen
    public static void showLogs(Context ctx) {
        // hvem er logget ind? (sat i sessionen ved login) – null = ikke logget ind
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        ctx.attribute("logs", new HormoneService().getLogs(patientId));   // requestscope -> ${logs}
        ctx.render("hormoner");                                            // templates/hormoner.html
    }

    // POST /hormoner – når brugeren trykker "Gem måling". ctx = kuverten fra Javalin: felterne ligger i den, og svaret sendes gennem den
    public static void saveLog(Context ctx) {
        String hormone = ctx.formParam("hormone");
        String value = ctx.formParam("value");
        String unit = ctx.formParam("unit");
        String date = ctx.formParam("date");

        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }

        ServiceResult result = new HormoneService().saveLog(patientId, hormone, value, unit, date);

        switch (result) {
            case OK -> ctx.redirect("/hormoner");
            case INVALID_INPUT -> ctx.redirect("/hormoner?fejl=felter");
            case NO_ACTIVE_JOURNEY -> ctx.redirect("/hormoner?fejl=intet-forloeb");
            case NO_ACTIVE_ROUND -> ctx.redirect("/hormoner?fejl=ingen-runde");
            default -> ctx.redirect("/hormoner?fejl=ukendt");
        }
    }
}
