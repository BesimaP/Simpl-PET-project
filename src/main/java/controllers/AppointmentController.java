package controllers;

import enums.AppointmentType;
import enums.ServiceResult;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import persistence.ConnectionPool;
import services.AppointmentService;
import services.DashboardService;
import entities.FertilityJourney;
import exceptions.NoActiveJourneyException;

// Koordinatoren for aftaler (US3). Læser formularen, kalder AppointmentService og sender brugeren videre.
// Ingen SQL og ingen mappers her – det bor i service- og persistence-laget.
public class AppointmentController {
    private AppointmentService appointmentService;
    private DashboardService dashboardService;   // til at finde patientens forløb (det aktive og de afsluttede)

    public AppointmentController(ConnectionPool connectionPool) {
        this.appointmentService = new AppointmentService(connectionPool);
        this.dashboardService = new DashboardService(connectionPool);
    }

    // Skriver ruterne på Javalins liste. Kaldes én gang fra RouteConfig
    public void setRoutes(JavalinConfig config) {
        config.routes.get("/aftaler", ctx -> showAppointments(ctx));  // vis siden med aftalerne (Thymeleaf)
        config.routes.post("/aftaler", ctx -> addAppointment(ctx));   // gem en aftale (formularen på siden)
    }

    // GET /aftaler – hent kommende og tidligere aftaler og fyld skabelonen
    private void showAppointments(Context ctx) {
        // hvem er logget ind? (sat i sessionen ved login) – null = ikke logget ind
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        // hvilket forløb vises? ?forloeb=7 i adressen (fx fra rundehistorik) – ellers det aktive.
        // findJourney giver kun patientens EGNE forløb, så man ikke kan se andres ved at rette tallet
        FertilityJourney active = null;
        try {
            active = dashboardService.findActiveJourney(patientId);
        } catch (NoActiveJourneyException e) {
            // intet aktivt forløb – så vises enten det valgte eller ingenting
        }
        FertilityJourney shown = active;
        try {
            if (ctx.queryParam("forloeb") != null) {
                FertilityJourney chosen = dashboardService.findJourney(patientId, Integer.parseInt(ctx.queryParam("forloeb")));
                if (chosen != null) {
                    shown = chosen;
                }
            }
        } catch (NumberFormatException e) {
            // ugyldigt tal i adressen – vis det aktive
        }

        ctx.attribute("journeys", dashboardService.getJourneys(patientId));   // requestscope -> links til de andre forløb
        ctx.attribute("appointmentTypes", AppointmentType.values());   // dropdownen bygges af enum'en
        ctx.attribute("shown", shown);                                       // det forløb, siden viser (kan være null)
        // formularen "Ny aftale" vises kun på det aktive forløb – man kan ikke tilføje aftaler til et afsluttet
        ctx.attribute("canAdd", active != null && shown != null && shown.getId() == active.getId());   // intet aktivt forløb = ingen formular
        if (shown == null) {
            ctx.attribute("upcoming", new java.util.ArrayList<>());
            ctx.attribute("past", new java.util.ArrayList<>());
        } else {
            ctx.attribute("upcoming", appointmentService.getUpcoming(patientId, shown.getId()));   // requestscope -> ${upcoming}
            ctx.attribute("past", appointmentService.getPast(patientId, shown.getId()));           // requestscope -> ${past}
        }
        // besked fra sidste POST (?gemt= / ?fejl= i URL'en) -> request scope -> fragmentet besked.html
        ctx.attribute("gemt", ctx.queryParam("gemt"));
        ctx.attribute("fejl", ctx.queryParam("fejl"));
        ctx.render("aftaler");                                       // templates/aftaler.html
    }

    // POST /aftaler – når brugeren trykker "Gem aftale". ctx = kuverten fra Javalin
    private void addAppointment(Context ctx) {
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
        ServiceResult result = appointmentService.addAppointment(patientId, type, location, date, time);

        // 4. vælg side ud fra svaret – redirect til RUTEN /aftaler (aftaler hænger på forløbet, så "ingen runde" findes ikke her)
        switch (result) {
            case OK -> ctx.redirect("/aftaler?gemt=1");
            case INVALID_INPUT -> ctx.redirect("/aftaler?fejl=felter");            // et felt var tomt eller ugyldigt
            case NO_ACTIVE_JOURNEY -> ctx.redirect("/aftaler?fejl=intet-forloeb");
            default -> ctx.redirect("/aftaler?fejl=ukendt");
        }
    }
}