package controllers;

import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import services.TimelineService;

// Koordinatoren for tidslinje (US2). Viser rundens trin (Event) – patienten tilføjer ikke selv trin.
public class TimelineController {

    public static void setRoutes(JavalinConfig config) {
        config.routes.get("/tidslinje", ctx -> showTimeline(ctx));
    }

    // GET /tidslinje – hent rundens trin og fyld skabelonen
    private static void showTimeline(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        ctx.attribute("events", new TimelineService().getEvents(patientId));   // requestscope -> ${events}
        ctx.render("tidslinje");
    }
}