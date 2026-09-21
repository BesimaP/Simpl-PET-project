package controller;

import dao.DatabaseConnection;
import dao.FertilityJourneyDAO;
import dao.RoundDAO;
import entities.FertilityJourney;
import entities.Round;
import enums.JourneyStatus;
import enums.Result;
import enums.RoundStatus;
import enums.TreatmentType;
import io.javalin.Javalin;

import java.time.LocalDate;

// Koordinatoren for dashboard.html, dashboardtom.html, start-runde.html og rundehistorik.html.
// Forløb (FertilityJourney) og runder (Round) oprettes og afsluttes herfra.
public class DashboardController {

    public static void registerRoutes(Javalin app) {

        // POST /opret-forloeb – når brugeren trykker "Start dit forløb" på dashboardtom.html (US1)
        app.post("/opret-forloeb", ctx -> {
            // 1. åbn kuverten: startdatoen fra formularen
            String startDate = ctx.formParam("startDate");
            int patientId = 1; // TODO: fra session, når login husker hvem der er logget ind

            // 2. tom dato? (feltet har required, men serveren stoler aldrig blindt på browseren) -> tilbage til siden
            if (startDate == null || startDate.isBlank()) {
                ctx.redirect("/dashboardtom.html?fejl=dato");
                return;
            }

            FertilityJourneyDAO journeyDao = new FertilityJourneyDAO(DatabaseConnection.getConnection());

            // 3. regel fra US1: kun ét aktivt forløb ad gangen – opret kun, hvis der ikke allerede er et
            if (journeyDao.findActiveByPatient(patientId) == null) {
                journeyDao.save(new FertilityJourney(0, patientId, LocalDate.parse(startDate), JourneyStatus.ACTIVE));
            }

            // 4. ind på dashboard – nu findes der et forløb (med eller uden runde)
            ctx.redirect("/dashboard.html");
        });

        // POST /start-runde – når brugeren trykker "Start runde" på start-runde.html (US10a)
        app.post("/start-runde", ctx -> {
            // 1. åbn kuverten: behandlingstype og startdato
            String type = ctx.formParam("type");           // "IVF", "ICSI", "IUI" eller "FET" (value i dropdownen)
            String startDate = ctx.formParam("startDate");
            int patientId = 1; // TODO: fra session

            FertilityJourneyDAO journeyDao = new FertilityJourneyDAO(DatabaseConnection.getConnection());
            RoundDAO roundDao = new RoundDAO(DatabaseConnection.getConnection());

            // 2. find patientens aktive forløb – uden forløb er der ingen skuffe at lægge runden i
            FertilityJourney journey = journeyDao.findActiveByPatient(patientId);
            if (journey == null) {
                ctx.redirect("/dashboardtom.html");
                return;
            }

            // 3. er der allerede en runde i gang? så må der ikke startes en ny (kun én ad gangen)
            if (roundDao.findActiveByJourney(journey.getId()) != null) {
                ctx.redirect("/dashboard.html?fejl=runde-i-gang");
                return;
            }

            // 4. rundenummer = antal runder i forløbet + 1 (første runde bliver nr. 1)
            int roundNumber = roundDao.findByJourney(journey.getId()).size() + 1;

            // 5. byg kortet og gem. end_date og result er null, til runden afsluttes
            Round round = new Round(0, journey.getId(), roundNumber, TreatmentType.valueOf(type),
                    LocalDate.parse(startDate), null, RoundStatus.IN_PROGRESS, null);
            roundDao.save(round);

            // 6. ind på dashboard
            ctx.redirect("/dashboard.html");
        });

        // POST /afslut-runde – når brugeren bekræfter "Afslut runde" i dialogen på dashboard.html (US10b)
        app.post("/afslut-runde", ctx -> {
            // 1. resultatet er valgfrit: "POSITIVE", "NEGATIVE" eller tomt (kan udfyldes senere)
            String resultParam = ctx.formParam("result");
            Result result = (resultParam == null || resultParam.isBlank()) ? null : Result.valueOf(resultParam);
            int patientId = 1; // TODO: fra session

            FertilityJourneyDAO journeyDao = new FertilityJourneyDAO(DatabaseConnection.getConnection());
            RoundDAO roundDao = new RoundDAO(DatabaseConnection.getConnection());

            // 2. find forløbet og den runde, der er i gang
            FertilityJourney journey = journeyDao.findActiveByPatient(patientId);
            Round round = (journey == null) ? null : roundDao.findActiveByJourney(journey.getId());
            if (round == null) {
                ctx.redirect("/dashboard.html?fejl=ingen-runde"); // intet at afslutte
                return;
            }

            // 3. afslut den: slutdato = i dag, status COMPLETED
            roundDao.endRound(round.getId(), LocalDate.now(), result);

            // 4. videre til historikken, hvor den nu står som afsluttet
            ctx.redirect("/rundehistorik.html");
        });

        // GET /dashboard og GET /rundehistorik (vis forløb, runde og historik fra databasen) kommer med templates
    }
}
