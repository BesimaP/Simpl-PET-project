package controllers;

import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import persistence.ConnectionPool;
import services.ProfileService;
import services.TimelineService;
import services.RoundService;
import entities.Round;
import exceptions.NoActiveJourneyException;
import exceptions.NoActiveRoundException;

// Controller for tidslinjen (US2). Viser rundens trin (Event) – patienten tilføjer ikke selv trin.
public class TimelineController {
    private ProfileService profileService;
    private TimelineService timelineService;
    private RoundService roundService;   // til at finde den runde, tidslinjen viser

    public TimelineController(ConnectionPool connectionPool) {
        this.profileService = new ProfileService(connectionPool);
        this.timelineService = new TimelineService(connectionPool);
        this.roundService = new RoundService(connectionPool);
    }

    public void setRoutes(JavalinConfig config) {
        config.routes.get("/tidslinje", ctx -> showTimeline(ctx));
    }

    // GET /tidslinje
    private void showTimeline(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        // ?runde=12 (fx fra rundehistorik) viser en bestemt runde, ellers den aktive.
        // findRound finder kun patientens egne runder
        Round round = null;
        try {
            if (ctx.queryParam("runde") != null) {
                round = roundService.findRound(patientId, Integer.parseInt(ctx.queryParam("runde")));
            }
        } catch (NumberFormatException e) {
            // ugyldigt tal i URL'en – vi viser bare den aktive runde
        }
        if (round == null) {
            try {
                round = roundService.findActiveRound(patientId);
            } catch (NoActiveJourneyException | NoActiveRoundException e) {
                // ingen runde i gang og ingen valgt -> tom tidslinje
            }
        }
        ctx.attribute("round", round);   // null = ingen runde
        ctx.attribute("events", round == null ? new java.util.ArrayList<>() : timelineService.getEvents(patientId, round.getId()));
        ctx.attribute("today", java.time.LocalDate.now());                      // så skabelonen kan se, om et trin er sket eller kommende
        ctx.attribute("patient", profileService.getPatient(patientId));   // til forbogstavet i avataren
        // ?gemt= / ?fejl= fra sidste POST vises af fragmentet besked.html
        ctx.attribute("gemt", ctx.queryParam("gemt"));
        ctx.attribute("fejl", ctx.queryParam("fejl"));
        ctx.render("tidslinje");
    }
}