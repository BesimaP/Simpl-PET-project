package controllers;

import enums.ServiceResult;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import services.DiaryService;

// Koordinatoren for dagbog.html (US4). Læser formularen, kalder DiaryService og sender brugeren videre.
// Ingen SQL og ingen DAO'er her – det bor i service- og dao-laget.
public class DiaryController {

    // Skriver ruterne på Javalins liste. Kaldes én gang fra Main: DiaryController.setRoutes(config)
    public static void setRoutes(JavalinConfig config) {
        // "når der kommer POST til /dagbog (formularen på dagbog.html), så kald saveEntry med den ctx, Javalin rækker os"
        config.routes.post("/dagbog", ctx -> saveEntry(ctx));
        config.routes.get("/dagbog", ctx -> showEntries(ctx));
    }

    public static void showEntries(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        ctx.attribute("entries", new DiaryService().getEntries(patientId));   // requestscope -> ${entries}
        ctx.render("dagbog");

    }

    // POST /dagbog – når brugeren trykker "Gem note". ctx = kuverten fra Javalin
    public static void saveEntry(Context ctx) {
        // 1. åbn kuverten: læs de tre felter (name="date", "title", "note" i dagbog.html)
        String date = ctx.formParam("date");     // fx "2026-09-21"
        String title = ctx.formParam("title");   // fx "Scanning i dag"
        String note = ctx.formParam("note");     // selve teksten fra textarea

        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }

        // 2. bed service gemme noten – den finder selv forløbet. Svar: OK = ok, ellers hvad der gik galt
        ServiceResult result = new DiaryService().saveEntry(patientId, date, title, note);

        // 3. vælg side ud fra svaret – ét case per udfald (noter hænger på forløbet, så "ingen runde" findes ikke her)
        switch (result) {
            case OK -> ctx.redirect("/dagbog");
            case INVALID_INPUT -> ctx.redirect("/dagbog?fejl=felter"); // et felt var tomt
            case NO_ACTIVE_JOURNEY -> ctx.redirect("/dagbog?fejl=intet-forloeb");
            default -> ctx.redirect("/dagbog?fejl=ukendt");
        }
    }
}
