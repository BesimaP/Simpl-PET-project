package controllers;

import entities.Appointment;
import entities.FertilityJourney;
import entities.HormoneLog;
import entities.Round;
import enums.Result;
import enums.ServiceResult;
import exceptions.NoActiveJourneyException;
import exceptions.NoActiveRoundException;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import services.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

// Koordinatoren for dashboard, start-runde.html og rundehistorik.
// Ruterne er én linje hver. Metoderne læser formularen, kalder service og sender brugeren videre – ingen DAO'er her.
public class DashboardController {

    public static void setRoutes(JavalinConfig config) {
        config.routes.get("/dashboard", ctx -> showDashboard(ctx));        // forsiden efter login (Thymeleaf)
        config.routes.get("/rundehistorik", ctx -> showRounds(ctx));       // alle runder (Thymeleaf)
        config.routes.post("/opret-forloeb", ctx -> createJourney(ctx));   // "Start dit forløb" på dashboard (US1)
        config.routes.post("/start-runde", ctx -> startRound(ctx));        // "Start runde" på start-runde.html (US10a)
        config.routes.post("/afslut-runde", ctx -> endRound(ctx));         // bekræft i dialogen på dashboard (US10b)
    }

    // GET /dashboard – samler data fra alle emner og fylder skabelonen. Tre tilstande: intet forløb, forløb uden runde, runde i gang
    private static void showDashboard(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }

        ctx.attribute("patient", new ProfileService().getPatient(patientId));
        ctx.attribute("today", LocalDate.now());

        // påmindelser om dagens medicin oprettes, når forsiden åbnes (US12). Tallet bruges til prikken på klokken
        NotificationService notificationService = new NotificationService();
        notificationService.createMedicationReminders(patientId);
        ctx.attribute("unread", notificationService.countUnread(patientId));

        // forløb og runde: null, hvis de ikke findes -> skabelonen viser den rigtige tilstand med th:if
        FertilityJourney journey = null;
        Round round = null;
        try {
            journey = new DashboardService().findActiveJourney(patientId);
            round = new RoundService().findActiveRound(patientId);
        } catch (NoActiveJourneyException | NoActiveRoundException e) {
            // ingen fejl – bare en af de to tilstande
        }
        ctx.attribute("journey", journey);
        ctx.attribute("round", round);
        if (round != null) {
            // dag-nummer i runden: dage siden start + 1
            ctx.attribute("dayNumber", ChronoUnit.DAYS.between(round.getStartDate(), LocalDate.now()) + 1);
        }

        // små kort: dagens medicin, næste aftale, seneste hormonværdi, antal noter
        MedicationService medicationService = new MedicationService();
        ctx.attribute("todayMeds", medicationService.getTodayLogs(patientId));
        ctx.attribute("names", medicationService.getMedicationNames());
        List<Appointment> upcoming = new AppointmentService().getUpcoming(patientId);
        ctx.attribute("nextAppointment", upcoming.isEmpty() ? null : upcoming.get(0));   // første = nærmeste
        List<HormoneLog> logs = new HormoneService().getLogs(patientId);
        ctx.attribute("latestHormone", logs.isEmpty() ? null : logs.get(0));             // nyeste først
        ctx.attribute("noteCount", new DiaryService().getEntries(patientId).size());

        ctx.render("dashboard");
    }

    // GET /rundehistorik – hent alle runder og fyld skabelonen
    private static void showRounds(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        ctx.attribute("rounds", new RoundService().getRounds(patientId));   // requestscope -> ${rounds}
        ctx.render("rundehistorik");
    }

    // POST /opret-forloeb
    private static void createJourney(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }

        // åbn kuverten: startdatoen fra formularen
        String startDate = ctx.formParam("startDate");

        // bed service oprette forløbet – den kender reglen "kun ét aktivt"
        ServiceResult result = new DashboardService().createJourney(patientId, startDate);

        // dashboard viser selv "start dit forløb"-delen, hvis der stadig intet forløb er
        switch (result) {
            case OK, ALREADY_EXISTS -> ctx.redirect("/dashboard");   // der findes nu et aktivt forløb
            case INVALID_INPUT -> ctx.redirect("/dashboard?fejl=dato");
            default -> ctx.redirect("/dashboard?fejl=ukendt");
        }
    }

    // POST /start-runde
    private static void startRound(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }

        // åbn kuverten: behandlingstype ("IVF", "ICSI", "IUI", "FET" = value i dropdownen) og startdato
        String type = ctx.formParam("type");
        String startDate = ctx.formParam("startDate");

        ServiceResult result = new RoundService().startRound(patientId, type, startDate);

        // vælg side ud fra svaret
        switch (result) {
            case OK -> ctx.redirect("/dashboard");
            case NO_ACTIVE_JOURNEY -> ctx.redirect("/dashboard");                    // dashboard viser "start dit forløb"
            case ROUND_IN_PROGRESS -> ctx.redirect("/dashboard?fejl=runde-i-gang");
            case INVALID_INPUT -> ctx.redirect("/start-runde.html?fejl=felter");    // start-runde ligger stadig i public
            default -> ctx.redirect("/dashboard?fejl=ukendt");
        }
    }

    // POST /afslut-runde
    private static void endRound(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }

        // resultatet er valgfrit: "POSITIVE", "NEGATIVE" eller tomt (kan udfyldes senere)
        String resultParam = ctx.formParam("result");
        Result result = (resultParam == null || resultParam.isBlank()) ? null : Result.valueOf(resultParam);

        ServiceResult outcome = new RoundService().endRound(patientId, result);

        switch (outcome) {
            case OK -> ctx.redirect("/rundehistorik");
            case NO_ACTIVE_ROUND -> ctx.redirect("/dashboard?fejl=ingen-runde");
            default -> ctx.redirect("/dashboard?fejl=ukendt");
        }
    }
}