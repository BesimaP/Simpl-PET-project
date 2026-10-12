package controllers;

import enums.ServiceResult;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import persistence.ConnectionPool;
import services.MedicationService;
import services.RoundService;

// Controller for medicin.html (US8). Læser formularen, kalder MedicationService og sender brugeren videre.
// SQL og mappers ligger i service- og persistence-laget.
public class MedicationController {

    private MedicationService medicationService;
    private RoundService roundService;     // kun til rundens startdato (min på datofeltet)

    public MedicationController(ConnectionPool connectionPool){
        this.medicationService = new MedicationService(connectionPool);
        this.roundService = new RoundService(connectionPool);
    }

    // bliver kaldt én gang fra RouteConfig
    public void setRoutes(JavalinConfig config) {
        config.routes.get("/medicin", ctx -> showLogs(ctx));
        config.routes.post("/medicin", ctx -> logDose(ctx));
        config.routes.post("/medicin/taget", ctx -> markTaken(ctx));
        config.routes.post("/medicin/ikke-taget", ctx -> markNotTaken(ctx));   // fortryd "taget"
        config.routes.post("/medicin/slet", ctx -> deleteDose(ctx));           // fx en fejlindtastning
    }

    // GET /medicin – dagens, kommende og tidligere doser
    private void showLogs(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }

        ctx.attribute("today", medicationService.getTodayLogs(patientId));
        ctx.attribute("upcoming", medicationService.getUpcomingLogs(patientId)); // fra i morgen og frem
        ctx.attribute("past", medicationService.getPastLogs(patientId));
        ctx.attribute("names", medicationService.getMedicationNames());      // ${names[m.medicationId]} i skabelonen
        ctx.attribute("medications", medicationService.getMedications());    // til dropdownen
        ctx.attribute("roundStart", roundService.getActiveRoundStart(patientId)); // min på datofeltet (null = ingen runde)
        // ?gemt= / ?fejl= fra sidste POST vises af fragmentet besked.html
        ctx.attribute("gemt", ctx.queryParam("gemt"));
        ctx.attribute("fejl", ctx.queryParam("fejl"));
        ctx.render("medicin");
    }

    // POST /medicin/taget – id kommer fra et skjult felt
    private void markTaken(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        // et ugyldigt id giver en fejlbesked i stedet for en 500-fejl
        int logId;
        try {
            logId = Integer.parseInt(ctx.formParam("id"));
        } catch (NumberFormatException e) {
            ctx.redirect("/medicin?fejl=ukendt");
            return;
        }
        ServiceResult result = medicationService.markTaken(patientId, logId);   // service tjekker, at dosen er patientens egen
        ctx.redirect(result == ServiceResult.OK ? "/medicin" : "/medicin?fejl=ukendt");
    }

    // POST /medicin/slet – service tjekker, at dosen er patientens egen
    private void deleteDose(Context ctx) {
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
        ServiceResult result = medicationService.deleteDose(patientId, logId);
        ctx.redirect(result == ServiceResult.OK ? "/medicin?gemt=slettet" : "/medicin?fejl=ukendt");
    }

    // POST /medicin/ikke-taget – fortryder "markér som taget"
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
        ServiceResult result = medicationService.markNotTaken(patientId, logId);
        ctx.redirect(result == ServiceResult.OK ? "/medicin" : "/medicin?fejl=ukendt");
    }

    // POST /medicin – når brugeren trykker "Gem"
    private void logDose(Context ctx) {
        // ingen enhed i formularen – den følger lægemidlet (medication.unit)
        String medication = ctx.formParam("medication"); // fx "GONAL_F", skal matche medication.name i databasen
        String dose = ctx.formParam("dose");             // stadig tekst, service laver den om til et tal
        String date = ctx.formParam("date");
        String time = ctx.formParam("time");
        boolean taken = ctx.formParam("taken") != null;  // en tom checkbox bliver slet ikke sendt med

        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }

        // service finder selv forløb, runde og lægemiddel
        ServiceResult result = medicationService.logDose(patientId, medication, dose, date, time, taken);

        switch (result) {
            case OK -> ctx.redirect("/medicin?gemt=1");
            case INVALID_INPUT -> ctx.redirect("/medicin?fejl=felter");            // tomt felt, ugyldigt tal/dato eller ukendt medicin
            case NO_ACTIVE_JOURNEY -> ctx.redirect("/medicin?fejl=intet-forloeb");
            case NO_ACTIVE_ROUND -> ctx.redirect("/medicin?fejl=ingen-runde");
            default -> ctx.redirect("/medicin?fejl=ukendt");
        }
    }
}
