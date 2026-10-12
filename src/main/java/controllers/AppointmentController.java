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

// Controller for aftaler (US3). Læser formularen, kalder AppointmentService og redirecter.
// SQL og mappers ligger i service- og persistence-laget.
public class AppointmentController {
    private AppointmentService appointmentService;
    private DashboardService dashboardService;   // bruges til at finde patientens forløb

    public AppointmentController(ConnectionPool connectionPool) {
        this.appointmentService = new AppointmentService(connectionPool);
        this.dashboardService = new DashboardService(connectionPool);
    }

    // bliver kaldt én gang fra RouteConfig
    public void setRoutes(JavalinConfig config) {
        config.routes.get("/aftaler", ctx -> showAppointments(ctx));
        config.routes.post("/aftaler", ctx -> addAppointment(ctx));
    }

    // GET /aftaler – kommende og tidligere aftaler for det valgte forløb
    private void showAppointments(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        // ?forloeb=7 (fx fra rundehistorik) viser et bestemt forløb, ellers det aktive.
        // findJourney finder kun patientens egne forløb, så man kan ikke se andres ved at ændre tallet
        FertilityJourney active = null;
        try {
            active = dashboardService.findActiveJourney(patientId);
        } catch (NoActiveJourneyException e) {
            // intet aktivt forløb – så vises det valgte eller ingenting
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
            // ugyldigt tal i URL'en – vi viser bare det aktive
        }

        ctx.attribute("journeys", dashboardService.getJourneys(patientId));   // links til de andre forløb
        ctx.attribute("appointmentTypes", AppointmentType.values());   // dropdownen bygges ud fra enum'en
        ctx.attribute("shown", shown);   // kan være null
        // man kan kun tilføje aftaler til det aktive forløb, ikke til et afsluttet
        ctx.attribute("canAdd", active != null && shown != null && shown.getId() == active.getId());
        if (shown == null) {
            ctx.attribute("upcoming", new java.util.ArrayList<>());
            ctx.attribute("past", new java.util.ArrayList<>());
        } else {
            ctx.attribute("upcoming", appointmentService.getUpcoming(patientId, shown.getId()));
            ctx.attribute("past", appointmentService.getPast(patientId, shown.getId()));
        }
        // ?gemt= / ?fejl= fra sidste POST vises af fragmentet besked.html
        ctx.attribute("gemt", ctx.queryParam("gemt"));
        ctx.attribute("fejl", ctx.queryParam("fejl"));
        ctx.render("aftaler");
    }

    // POST /aftaler – når brugeren trykker "Gem aftale"
    private void addAppointment(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }

        String type = ctx.formParam("type");         // fx "SCANNING", skal matche enum AppointmentType
        String location = ctx.formParam("location");
        String date = ctx.formParam("date");
        String time = ctx.formParam("time");

        // service finder selv det aktive forløb
        ServiceResult result = appointmentService.addAppointment(patientId, type, location, date, time);

        // aftaler hænger på forløbet og ikke på runden, så NO_ACTIVE_ROUND kan ikke ske her
        switch (result) {
            case OK -> ctx.redirect("/aftaler?gemt=1");
            case INVALID_INPUT -> ctx.redirect("/aftaler?fejl=felter");            // tomt eller ugyldigt felt
            case NO_ACTIVE_JOURNEY -> ctx.redirect("/aftaler?fejl=intet-forloeb");
            default -> ctx.redirect("/aftaler?fejl=ukendt");
        }
    }
}