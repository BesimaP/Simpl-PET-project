package controllers;

import enums.ServiceResult;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import persistence.ConnectionPool;
import services.MedicationService;

// Koordinatoren for medicin.html (US8). Læser formularen, kalder MedicationService og sender brugeren videre.
// Ingen SQL og ingen mappers her – det bor i service- og persistence-laget.
public class MedicationController {

    private MedicationService medicationService;

    public MedicationController(ConnectionPool connectionPool){
        this.medicationService = new MedicationService(connectionPool);
    }

    // Skriver ruterne på Javalins liste. Kaldes én gang fra Main: MedicationController.setRoutes(config)
    public void setRoutes(JavalinConfig config) {
        config.routes.get("/medicin", ctx -> showLogs(ctx));
        config.routes.post("/medicin", ctx -> logDose(ctx));
        config.routes.post("/medicin/taget", ctx -> markTaken(ctx));   // knappen på hver dosis
        config.routes.post("/medicin/ikke-taget", ctx -> markNotTaken(ctx));   // fortryd på hver dosis
    }

    // GET /medicin – hent dagens og tidligere doser + medicinnavne, og fyld skabelonen
    private void showLogs(Context ctx) {
        // hvem er logget ind? (sat i sessionen ved login) – null = ikke logget ind
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }

        ctx.attribute("today", medicationService.getTodayLogs(patientId));   // requestscope -> ${today}
        ctx.attribute("upcoming", medicationService.getUpcomingLogs(patientId)); // requestscope -> ${upcoming} (fra i morgen og frem)
        ctx.attribute("past", medicationService.getPastLogs(patientId));     // requestscope -> ${past}
        ctx.attribute("names", medicationService.getMedicationNames());      // requestscope -> ${names[m.medicationId]}
        ctx.attribute("medications", medicationService.getMedications());    // requestscope -> dropdownen (th:each)
        // besked fra sidste POST (?gemt= / ?fejl= i URL'en) -> request scope -> fragmentet besked.html
        ctx.attribute("gemt", ctx.queryParam("gemt"));
        ctx.attribute("fejl", ctx.queryParam("fejl"));
        ctx.render("medicin");                                     // templates/medicin.html
    }

    // POST /medicin/taget – knappen "markér som taget" på én dosis (id i et skjult felt)
    private void markTaken(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        // "17" -> 17. try/catch: et ugyldigt id (fx rettet i det skjulte felt) giver en fejlbesked i stedet for en 500-fejl
        int logId;
        try {
            logId = Integer.parseInt(ctx.formParam("id"));
        } catch (NumberFormatException e) {
            ctx.redirect("/medicin?fejl=ukendt");
            return;
        }
        medicationService.markTaken(patientId, logId);   // service tjekker, at dosen er patientens egen
        ctx.redirect("/medicin");
    }

    // POST /medicin/ikke-taget – fortryd "markér som taget" (id i et skjult felt)
    private void markNotTaken(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        int logId;
        try {
            logId = Integer.parseInt(ctx.formParam("id"));
        } catch (NumberFormatException e) {
            ctx.redirect("/medicin?fejl=ukendt");
            return;
        }
        medicationService.markNotTaken(patientId, logId);
        ctx.redirect("/medicin");
    }

    // POST /medicin – når brugeren trykker "Gem". ctx = kuverten fra Javalin
    private void logDose(Context ctx) {
        // 1. åbn kuverten: læs felterne (name="medication", "dose", "date", "time", "taken" i medicin.html).
        //    Ingen enhed: den følger lægemidlet (medication.unit)
        String medication = ctx.formParam("medication"); // fx "GONAL_F" – value i dropdownen, matcher medication.name i databasen
        String dose = ctx.formParam("dose");             // fx "150" – tekst endnu, service laver den om til tal
        String date = ctx.formParam("date");             // fx "2026-09-22"
        String time = ctx.formParam("time");             // fx "08:00"
        boolean taken = ctx.formParam("taken") != null;  // en afkrydset checkbox sendes med, en tom sendes slet ikke (null)

        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }

        // 2. bed service gemme dosen – den finder selv forløb, runde og lægemiddel. Svar: OK = ok, ellers hvad der gik galt
        ServiceResult result = medicationService.logDose(patientId, medication, dose, date, time, taken);

        // 3. vælg side ud fra svaret – ét case per udfald
        switch (result) {
            case OK -> ctx.redirect("/medicin?gemt=1");
            case INVALID_INPUT -> ctx.redirect("/medicin?fejl=felter");            // tomt felt, ugyldigt tal/dato eller ukendt medicin
            case NO_ACTIVE_JOURNEY -> ctx.redirect("/medicin?fejl=intet-forloeb");
            case NO_ACTIVE_ROUND -> ctx.redirect("/medicin?fejl=ingen-runde");
            default -> ctx.redirect("/medicin?fejl=ukendt");
        }
    }
}
