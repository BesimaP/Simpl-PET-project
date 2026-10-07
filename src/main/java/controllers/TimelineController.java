package controllers;

import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import persistence.ConnectionPool;
import services.ProfileService;
import services.TimelineService;

// Koordinatoren for tidslinje (US2). Viser rundens trin (Event) – patienten tilføjer ikke selv trin.
public class TimelineController {
    private ProfileService profileService; // "den der bestemmer" for patienten (bruges til forbogstav i avataren)
    private TimelineService timelineService;

    // nøgleringen gives med fra RouteConfig og videre til ProfileService
    public TimelineController(ConnectionPool connectionPool) {
        this.profileService = new ProfileService(connectionPool);
        this.timelineService = new TimelineService(connectionPool);
    }

    public void setRoutes(JavalinConfig config) {
        config.routes.get("/tidslinje", ctx -> showTimeline(ctx));
    }

    // GET /tidslinje – hent rundens trin og fyld skabelonen
    private void showTimeline(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        ctx.attribute("events", timelineService.getEvents(patientId));   // requestscope -> ${events}
        ctx.attribute("today", java.time.LocalDate.now());                      // skabelonen sammenligner: sket eller kommende?
        ctx.attribute("patient", profileService.getPatient(patientId));   // forbogstav i avataren
        ctx.render("tidslinje");
    }
}