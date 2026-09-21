package controller;

import enums.ServiceResult;
import io.javalin.Javalin;
import io.javalin.http.Context;
import service.DiaryService;

// Koordinatoren for dagbog.html (US4). Læser formularen, kalder DiaryService og sender brugeren videre.
// Ingen SQL og ingen DAO'er her – det bor i service- og dao-laget.
public class DiaryController {

    // Skriver ruterne på Javalins liste. Kaldes én gang fra Main: DiaryController.registerRoutes(app)
    public static void registerRoutes(Javalin app) {
        // "når der kommer POST til /dagbog (formularen på dagbog.html), så kald saveEntry med den ctx, Javalin rækker os"
        app.post("/dagbog", ctx -> saveEntry(ctx));
    }

    // POST /dagbog – når brugeren trykker "Gem note". ctx = kuverten fra Javalin
    public static void saveEntry(Context ctx) {
        // 1. åbn kuverten: læs de tre felter (name="date", "title", "note" i dagbog.html)
        String date = ctx.formParam("date");     // fx "2026-09-21"
        String title = ctx.formParam("title");   // fx "Scanning i dag"
        String note = ctx.formParam("note");     // selve teksten fra textarea
        int patientId = 1; // TODO: fra session, når login husker hvem der er logget ind

        // 2. bed service gemme noten – den finder selv forløbet. Svar: OK = ok, ellers hvad der gik galt
        ServiceResult result = new DiaryService().saveEntry(patientId, date, title, note);

        // 3. vælg side ud fra svaret – ét case per udfald (noter hænger på forløbet, så "ingen runde" findes ikke her)
        switch (result) {
            case OK -> ctx.redirect("/dagbog.html");
            case INVALID_INPUT -> ctx.redirect("/dagbog.html?fejl=felter"); // et felt var tomt
            case NO_ACTIVE_JOURNEY -> ctx.redirect("/dagbog.html?fejl=intet-forloeb");
            default -> ctx.redirect("/dagbog.html?fejl=ukendt");
        }
    }
}
