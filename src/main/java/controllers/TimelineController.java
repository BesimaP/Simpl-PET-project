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

// Koordinatoren for tidslinje (US2). Viser rundens trin (Event) – patienten tilføjer ikke selv trin.
public class TimelineController {
    private ProfileService profileService; // "den der bestemmer" for patienten (bruges til forbogstav i avataren)
    private TimelineService timelineService;
    private RoundService roundService;   // til at finde den runde, tidslinjen viser

    // nøgleringen gives med fra RouteConfig og videre til ProfileService
    public TimelineController(ConnectionPool connectionPool) {
        this.profileService = new ProfileService(connectionPool);
        this.timelineService = new TimelineService(connectionPool);
        this.roundService = new RoundService(connectionPool);
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
        // hvilken runde? ?runde=12 i adressen (fx fra rundehistorik) – ellers den, der er i gang.
        // findRound giver kun patientens EGNE runder (i alle hendes forløb)
        Round round = null;
        try {
            if (ctx.queryParam("runde") != null) {
                round = roundService.findRound(patientId, Integer.parseInt(ctx.queryParam("runde")));
            }
        } catch (NumberFormatException e) {
            // ugyldigt tal i adressen – vis runden, der er i gang
        }
        if (round == null) {
            try {
                round = roundService.findActiveRound(patientId);
            } catch (NoActiveJourneyException | NoActiveRoundException e) {
                // ingen runde i gang og ingen valgt -> tom tidslinje
            }
        }
        ctx.attribute("round", round);   // requestscope -> overskriften "Runde 2 · IVF" (null = ingen runde)
        ctx.attribute("events", round == null ? new java.util.ArrayList<>() : timelineService.getEvents(patientId, round.getId()));
        ctx.attribute("today", java.time.LocalDate.now());                      // skabelonen sammenligner: sket eller kommende?
        ctx.attribute("patient", profileService.getPatient(patientId));   // forbogstav i avataren
        ctx.render("tidslinje");
    }
}