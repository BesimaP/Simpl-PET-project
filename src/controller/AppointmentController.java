package controller;

import enums.ServiceResult;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import service.AppointmentService;

// Koordinatoren for aftaler.html (US3). Læser formularen, kalder AppointmentService og sender brugeren videre.
// Ingen SQL og ingen DAO'er her – det bor i service- og dao-laget.
public class AppointmentController {

    // Skriver ruterne på Javalins liste. Kaldes én gang fra Main: AppointmentController.setRoutes(config)
    public static void setRoutes(JavalinConfig config) {
        config.routes.post("/aftaler", ctx -> addAppointment(ctx)); // formularen "Ny aftale" på aftaler.html
    }

    // POST /aftaler – når brugeren trykker "Gem aftale". ctx = kuverten fra Javalin
    private static void addAppointment(Context ctx) {
        // 1. åbn kuverten: læs de fire felter (name="type", "location", "date", "time" i aftaler.html)
        String type = ctx.formParam("type");         // fx "SCANNING" – value i dropdownen, matcher enum AppointmentType
        String location = ctx.formParam("location"); // fx "Vitanova"
        String date = ctx.formParam("date");         // fx "2026-09-22"
        String time = ctx.formParam("time");         // fx "10:30"
        int patientId = 1; // TODO: fra session, når login husker hvem der er logget ind

        // 2. bed service gemme aftalen – den finder selv forløbet. Svar: OK = ok, ellers hvad der gik galt
        ServiceResult result = new AppointmentService().addAppointment(patientId, type, location, date, time);

        // 3. vælg side ud fra svaret – ét case per udfald (aftaler hænger på forløbet, så "ingen runde" findes ikke her)
        switch (result) {
            case OK -> ctx.redirect("/aftaler.html");
            case INVALID_INPUT -> ctx.redirect("/aftaler.html?fejl=felter");            // et felt var tomt eller ugyldigt
            case NO_ACTIVE_JOURNEY -> ctx.redirect("/aftaler.html?fejl=ingen-forloeb");
            default -> ctx.redirect("/aftaler.html?fejl=ukendt");
        }
    }
}
