package controllers;

import enums.HormoneType;
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
        HormoneService service = new HormoneService();
        ctx.attribute("logs", service.getLogs(patientId));   // requestscope -> ${logs}

        // kurven: ?hormon=E2_OESTRADIOL vælger hormonet (knapperne over kurven). Ingen/ukendt værdi -> null -> nyeste måling bestemmer
        HormoneType chosen = null;
        try {
            if (ctx.queryParam("hormon") != null) {
                chosen = HormoneType.valueOf(ctx.queryParam("hormon"));
            }
        } catch (IllegalArgumentException e) {
            // ukendt hormon i URL'en – ignorér, vis standard
        }
        ctx.attribute("curve", service.getCurve(patientId, chosen));   // requestscope -> ${curve}
        ctx.attribute("hormoneTypes", HormoneType.values());           // requestscope -> ${hormoneTypes} (knapperne)
        // besked fra sidste POST (?gemt= / ?fejl= i URL'en) -> request scope -> fragmentet besked.html
        ctx.attribute("gemt", ctx.queryParam("gemt"));
        ctx.attribute("fejl", ctx.queryParam("fejl"));
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
            case OK -> ctx.redirect("/hormoner?gemt=1");
            case INVALID_INPUT -> ctx.redirect("/hormoner?fejl=felter");
            case NO_ACTIVE_JOURNEY -> ctx.redirect("/hormoner?fejl=intet-forloeb");
            case NO_ACTIVE_ROUND -> ctx.redirect("/hormoner?fejl=ingen-runde");
            default -> ctx.redirect("/hormoner?fejl=ukendt");
        }
    }
}
