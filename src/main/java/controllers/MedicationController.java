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
        config.routes.post("/medicin", ctx -> logDose(ctx)); // formularen "Ny medicin" på medicin.html
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
        int patientId = 1; // TODO: fra session, når login husker hvem der er logget ind

        // 2. bed service gemme dosen – den finder selv forløb, runde og lægemiddel. Svar: OK = ok, ellers hvad der gik galt
        ServiceResult result = new MedicationService().logDose(patientId, medication, dose, unit, date, time, taken);

        // 3. vælg side ud fra svaret – ét case per udfald
        switch (result) {
            case OK -> ctx.redirect("/medicin.html");
            case INVALID_INPUT -> ctx.redirect("/medicin.html?fejl=felter");            // tomt felt, ugyldigt tal/dato eller ukendt medicin
            case NO_ACTIVE_JOURNEY -> ctx.redirect("/medicin.html?fejl=ingen-forloeb");
            case NO_ACTIVE_ROUND -> ctx.redirect("/medicin.html?fejl=ingen-runde");
            default -> ctx.redirect("/medicin.html?fejl=ukendt");
        }
    }
}
