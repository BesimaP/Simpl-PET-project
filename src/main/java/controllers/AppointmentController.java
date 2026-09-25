package controllers;

import enums.ServiceResult;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import services.AppointmentService;

// Koordinatoren for aftaler (US3). Læser formularen, kalder AppointmentService og sender brugeren videre.
// Ingen SQL og ingen DAO'er her – det bor i service- og dao-laget.
public class AppointmentController {

    // Skriver ruterne på Javalins liste. Kaldes én gang fra RouteConfig
    public static void setRoutes(JavalinConfig config) {
        config.routes.get("/aftaler", ctx -> showAppointments(ctx));  // vis siden med aftalerne (Thymeleaf)
        config.routes.post("/aftaler", ctx -> addAppointment(ctx));   // gem en aftale (formularen på siden)
    }

    // GET /aftaler – hent kommende og tidligere aftaler og fyld skabelonen
    private static void showAppointments(Context ctx) {
        // hvem er logget ind? (sat i sessionen ved login) – null = ikke logget ind
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        AppointmentService service = new AppointmentService();
        ctx.attribute("upcoming", service.getUpcoming(patientId));   // requestscope -> ${upcoming}
        ctx.attribute("past", service.getPast(patientId));           // requestscope -> ${past}
        // besked fra sidste POST (?gemt= / ?fejl= i URL'en) -> request scope -> fragmentet besked.html
        ctx.attribute("gemt", ctx.queryParam("gemt"));
        ctx.attribute("fejl", ctx.queryParam("fejl"));
        ctx.render("aftaler");                                       // templates/aftaler.html
    }

    // POST /aftaler – når brugeren trykker "Gem aftale". ctx = kuverten fra Javalin
    private static void addAppointment(Context ctx) {
        // 1. hvem er logget ind?
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }

        // 2. åbn kuverten: læs de fire felter (name="type", "location", "date", "time" i aftaler.html)
        String type = ctx.formParam("type");         // fx "SCANNING" – value i dropdownen, matcher enum AppointmentType
        String location = ctx.formParam("location"); // fx "Vitanova"
        String date = ctx.formParam("date");         // fx "2026-09-22"
        String time = ctx.formParam("time");         // fx "10:30"

        // 3. bed service gemme aftalen – den finder selv forløbet. Svar: OK = ok, ellers hvad der gik galt
        ServiceResult result = new AppointmentService().addAppointment(patientId, type, location, date, time);

        // 4. vælg side ud fra svaret – redirect til RUTEN /aftaler (aftaler hænger på forløbet, så "ingen runde" findes ikke her)
        switch (result) {
            case OK -> ctx.redirect("/aftaler?gemt=1");
            case INVALID_INPUT -> ctx.redirect("/aftaler?fejl=felter");            // et felt var tomt eller ugyldigt
            case NO_ACTIVE_JOURNEY -> ctx.redirect("/aftaler?fejl=ingen-forloeb");
            default -> ctx.redirect("/aftaler?fejl=ukendt");
        }
    }
}