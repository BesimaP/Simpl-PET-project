package controllers;

import enums.ServiceResult;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import services.MedicationService;

// Koordinatoren for medicin.html (US8). Læser formularen, kalder MedicationService og sender brugeren videre.
// Ingen SQL og ingen DAO'er her – det bor i service- og dao-laget.
public class MedicationController {

    // Skriver ruterne på Javalins liste. Kaldes én gang fra Main: MedicationController.setRoutes(config)
    public static void setRoutes(JavalinConfig config) {
        config.routes.get("/medicin", ctx -> showLogs(ctx));
        config.routes.post("/medicin", ctx -> logDose(ctx));
        config.routes.post("/medicin/taget", ctx -> markTaken(ctx));   // knappen på hver dosis
    }

    // GET /medicin – hent dagens og tidligere doser + medicinnavne, og fyld skabelonen
    private static void showLogs(Context ctx) {
        // hvem er logget ind? (sat i sessionen ved login) – null = ikke logget ind
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        MedicationService service = new MedicationService();
        ctx.attribute("today", service.getTodayLogs(patientId));   // requestscope -> ${today}
        ctx.attribute("past", service.getPastLogs(patientId));     // requestscope -> ${past}
        ctx.attribute("names", service.getMedicationNames());      // requestscope -> ${names[m.medicationId]}
        // besked fra sidste POST (?gemt= / ?fejl= i URL'en) -> request scope -> fragmentet besked.html
        ctx.attribute("gemt", ctx.queryParam("gemt"));
        ctx.attribute("fejl", ctx.queryParam("fejl"));
        ctx.render("medicin");                                     // templates/medicin.html
    }

    // POST /medicin/taget – knappen "markér som taget" på én dosis (id i et skjult felt)
    private static void markTaken(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        int logId = Integer.parseInt(ctx.formParam("id"));   // "17" -> 17
        new MedicationService().markTaken(logId);
        ctx.redirect("/medicin");
    }

    // POST /medicin – når brugeren trykker "Gem". ctx = kuverten fra Javalin
    private static void logDose(Context ctx) {
        // 1. åbn kuverten: læs felterne (name="medication", "dose", "unit", "date", "time", "taken" i medicin.html)
        String medication = ctx.formParam("medication"); // fx "GONAL_F" – value i dropdownen, matcher medication.name i databasen
        String dose = ctx.formParam("dose");             // fx "150" – tekst endnu, service laver den om til tal
        String unit = ctx.formParam("unit");             // fx "IU"
        String date = ctx.formParam("date");             // fx "2026-09-22"
        String time = ctx.formParam("time");             // fx "08:00"
        boolean taken = ctx.formParam("taken") != null;  // en afkrydset checkbox sendes med, en tom sendes slet ikke (null)

        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }

        // 2. bed service gemme dosen – den finder selv forløb, runde og lægemiddel. Svar: OK = ok, ellers hvad der gik galt
        ServiceResult result = new MedicationService().logDose(patientId, medication, dose, unit, date, time, taken);

        // 3. vælg side ud fra svaret – ét case per udfald
        switch (result) {
            case OK -> ctx.redirect("/medicin?gemt=1");
            case INVALID_INPUT -> ctx.redirect("/medicin?fejl=felter");            // tomt felt, ugyldigt tal/dato eller ukendt medicin
            case NO_ACTIVE_JOURNEY -> ctx.redirect("/medicin?fejl=ingen-forloeb");
            case NO_ACTIVE_ROUND -> ctx.redirect("/medicin?fejl=ingen-runde");
            default -> ctx.redirect("/medicin?fejl=ukendt");
        }
    }
}
