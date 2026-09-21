package controller;

import enums.ServiceResult;
import io.javalin.Javalin;
import io.javalin.http.Context;
import service.HormoneService;

// Koordinatoren for hormoner.html (US9). Læser formularen, kalder HormoneService og sender brugeren videre.
// Ingen SQL og ingen DAO'er her – det bor i service- og dao-laget.
public class HormoneController {

    // Skriver ruterne på Javalins liste. Kaldes én gang fra Main: HormoneController.registerRoutes(app)
    public static void registerRoutes(Javalin app) {
        // "når der kommer POST til /hormoner (formularen på hormoner.html), så kald saveLog med den ctx, Javalin rækker os"
        app.post("/hormoner", ctx -> saveLog(ctx));
    }

    // POST /hormoner – når brugeren trykker "Gem måling". ctx = kuverten fra Javalin: felterne ligger i den, og svaret sendes gennem den
    public static void saveLog(Context ctx) {
        // 1. åbn kuverten: læs de fire felter (name="hormone", "value", "unit", "date" i hormoner.html)
        String hormone = ctx.formParam("hormone");   // fx "E2_OESTRADIOL" – value i dropdownen, matcher enum HormoneType
        String value = ctx.formParam("value");       // fx "450" – tekst endnu, service laver den om til tal
        String unit = ctx.formParam("unit");         // fx "pmol/L"
        String date = ctx.formParam("date");         // fx "2026-09-21"
        int patientId = 1; // TODO: fra session, når login husker hvem der er logget ind

        // 2. bed service gemme målingen – den finder selv forløb og runde. Svar: SAVED = ok, ellers hvad der gik galt
        ServiceResult result = new HormoneService().saveLog(patientId,hormone,value,unit,date);

        // 3. vælg side ud fra svaret – ét case per udfald.
        switch (result) {
            case SAVED -> ctx.redirect("/hormoner.html");
            case NO_ACTIVE_JOURNEY -> ctx.redirect("/hormoner.html?fejl=intet-forloeb");
            case NO_ACTIVE_ROUND -> ctx.redirect("/hormoner.html?fejl=ingen-runde");
            default -> ctx.redirect("/hormoner.html?fejl=ukendt");
        }
    }
}
