package controllers;

import entities.Appointment;
import entities.FertilityJourney;
import entities.HormoneLog;
import entities.Round;
import enums.Result;
import enums.TreatmentType;
import enums.ServiceResult;
import exceptions.NoActiveJourneyException;
import exceptions.NoActiveRoundException;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import persistence.ConnectionPool;
import services.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

// Controller for dashboard, start/afslut runde, forløb og rundehistorik.
// Den bruger mange services, fordi dashboardet viser lidt fra hver del af appen.
public class DashboardController {
    private ProfileService profileService;
    private NotificationService notificationService;
    private DashboardService dashboardService;
    private DiaryService diaryService;
    private RoundService roundService;
    private MedicationService medicationService;
    private HormoneService hormoneService;
    private AppointmentService appointmentService;

    public DashboardController(ConnectionPool connectionPool) {
        this.profileService = new ProfileService(connectionPool);
        this.dashboardService = new DashboardService(connectionPool);
        this.notificationService = new NotificationService(connectionPool);
        this.diaryService = new DiaryService(connectionPool);
        this.roundService = new RoundService(connectionPool);
        this.medicationService = new MedicationService(connectionPool);
        this.hormoneService = new HormoneService(connectionPool);
        this.appointmentService = new AppointmentService(connectionPool);
    }

    public void setRoutes(JavalinConfig config) {
        config.routes.get("/dashboard", ctx -> showDashboard(ctx));        // forsiden efter login
        config.routes.get("/rundehistorik", ctx -> showRounds(ctx));
        config.routes.post("/opret-forloeb", ctx -> createJourney(ctx));   // "Start dit forløb" på dashboard (US1)
        config.routes.get("/start-runde", ctx -> showStartRound(ctx));
        config.routes.post("/start-runde", ctx -> startRound(ctx));        // US10a
        config.routes.get("/afslut-runde", ctx -> showEndRound(ctx));      // bekræft-side, hvis JavaScript er slået fra
        config.routes.post("/afslut-runde", ctx -> endRound(ctx));         // US10a
        config.routes.get("/afslut-forloeb", ctx -> showEndJourney(ctx));
        config.routes.post("/afslut-forloeb", ctx -> endJourney(ctx));     // US1: man kan have flere forløb over tid
    }

    // GET /dashboard – tre tilstande: intet forløb, forløb uden runde eller runde i gang
    private void showDashboard(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }

        ctx.attribute("patient", profileService.getPatient(patientId));
        ctx.attribute("today", LocalDate.now());

        // vi opretter påmindelser, når forsiden åbnes (US12)
        notificationService.createMedicationReminders(patientId);
        notificationService.createAppointmentReminders(patientId);   // aftaler i dag og i morgen
        ctx.attribute("unread", notificationService.countUnread(patientId));
        ctx.attribute("notifications", notificationService.getNotifications(patientId));   // til pop-op'en ved klokken

        // null, hvis de ikke findes – skabelonen vælger tilstand med th:if
        FertilityJourney journey = null;
        Round round = null;
        try {
            journey = dashboardService.findActiveJourney(patientId);
            round = roundService.findActiveRound(patientId);
        } catch (NoActiveJourneyException | NoActiveRoundException e) {
            // ikke en fejl, bare en af tilstandene
        }
        ctx.attribute("journey", journey);
        ctx.attribute("round", round);
        if (round != null) {
            long dayNumber = ChronoUnit.DAYS.between(round.getStartDate(), LocalDate.now()) + 1;
            ctx.attribute("dayNumber", dayNumber);
            // hvor langt vi er i en runde på ca. 28 dage, holdt mellem 0 og 100 %
            ctx.attribute("progressPercent", Math.max(0, Math.min(100, dayNumber * 100 / 28)));
        }

        // de små bokse: dagens medicin, næste aftale, seneste hormonværdi og antal noter
        ctx.attribute("todayMeds", medicationService.getTodayLogs(patientId));
        ctx.attribute("names", medicationService.getMedicationNames());
        List<Appointment> upcoming = appointmentService.getUpcoming(patientId);
        ctx.attribute("nextAppointment", upcoming.isEmpty() ? null : upcoming.get(0));   // listen er sorteret, så den første er den næste
        List<HormoneLog> logs = hormoneService.getLogs(patientId);
        ctx.attribute("latestHormone", logs.isEmpty() ? null : logs.get(0));             // nyeste ligger først
        ctx.attribute("noteCount", diaryService.getEntries(patientId).size());
        ctx.attribute("results", Result.values());   // til dropdownen i "Afslut runde"-dialogen

        // ?gemt= / ?fejl= fra sidste POST vises af fragmentet besked.html
        ctx.attribute("gemt", ctx.queryParam("gemt"));
        ctx.attribute("fejl", ctx.queryParam("fejl"));
        ctx.render("dashboard");
    }

    // GET /rundehistorik
    private void showRounds(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        // alle forløb med deres runder, så de afsluttede også kan ses
        ctx.attribute("journeys", dashboardService.getJourneys(patientId));
        ctx.attribute("roundsPerJourney", roundService.getRoundsPerJourney(patientId)); // ${roundsPerJourney[j.id]} i skabelonen
        // ?gemt= / ?fejl= fra sidste POST vises af fragmentet besked.html
        ctx.attribute("gemt", ctx.queryParam("gemt"));
        ctx.attribute("fejl", ctx.queryParam("fejl"));
        ctx.render("rundehistorik");
    }

    // POST /opret-forloeb
    private void createJourney(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }

        String startDate = ctx.formParam("startDate");

        // service tjekker, at der kun er ét aktivt forløb
        ServiceResult result = dashboardService.createJourney(patientId, startDate);

        switch (result) {
            case OK -> ctx.redirect("/dashboard?gemt=forloeb");
            case ALREADY_EXISTS -> ctx.redirect("/dashboard?fejl=forloeb-findes");
            case INVALID_INPUT -> ctx.redirect("/dashboard?fejl=dato");
            default -> ctx.redirect("/dashboard?fejl=ukendt");
        }
    }

    // GET /start-runde
    private void showStartRound(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        ctx.attribute("today", LocalDate.now());   // startdatoen er udfyldt med dags dato
        ctx.attribute("treatmentTypes", TreatmentType.values());   // NFR5: en ny type tilføjes kun i enum'en
        ctx.attribute("gemt", ctx.queryParam("gemt"));
        ctx.attribute("fejl", ctx.queryParam("fejl"));
        ctx.render("start-runde");
    }

    // POST /start-runde
    private void startRound(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }

        // type er fx "IVF" eller "FET" fra dropdownen
        String type = ctx.formParam("type");
        String startDate = ctx.formParam("startDate");

        ServiceResult result = roundService.startRound(patientId, type, startDate);

        switch (result) {
            case OK -> ctx.redirect("/dashboard?gemt=runde");
            case NO_ACTIVE_JOURNEY -> ctx.redirect("/dashboard");                    // dashboard viser "Start dit forløb"
            case ROUND_IN_PROGRESS -> ctx.redirect("/dashboard?fejl=runde-i-gang");
            case INVALID_INPUT -> ctx.redirect("/start-runde?fejl=felter");
            default -> ctx.redirect("/dashboard?fejl=ukendt");
        }
    }

    // GET /afslut-runde – bekræft-side, hvis JavaScript er slået fra (ellers åbner dashboard.js en dialog).
    // bekraeft.html deles med "Afslut forløb" og "Slet konto", så teksterne sættes her
    private void showEndRound(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        Round round;
        try {
            round = roundService.findActiveRound(patientId);
        } catch (NoActiveJourneyException | NoActiveRoundException e) {
            ctx.redirect("/dashboard?fejl=ingen-runde");
            return;
        }
        ctx.attribute("title", "Afslut runde " + round.getRoundNumber() + "?");
        ctx.attribute("text", "Runden markeres som afsluttet, og du kan starte en ny. Dine logs og noter gemmes.");
        ctx.attribute("action", "/afslut-runde");
        ctx.attribute("button", "Afslut runde");
        ctx.attribute("cancel", "/dashboard");
        ctx.attribute("showResult", true);   // kun "Afslut runde" spørger om resultat
        ctx.attribute("results", Result.values());
        ctx.render("bekraeft");
    }

    // GET /afslut-forloeb – bekræft-side, hvis JavaScript er slået fra
    private void showEndJourney(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        ctx.attribute("title", "Afslut dit forløb?");
        ctx.attribute("text", "Forløbet markeres som afsluttet. Dine runder, logs og noter gemmes, og du kan starte et nyt forløb bagefter.");
        ctx.attribute("action", "/afslut-forloeb");
        ctx.attribute("button", "Afslut forløb");
        ctx.attribute("cancel", "/dashboard");
        ctx.attribute("showResult", false);
        ctx.render("bekraeft");
    }

    // POST /afslut-forloeb
    private void endJourney(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }

        // service tjekker, at runden er afsluttet først
        ServiceResult result = dashboardService.endJourney(patientId);

        switch (result) {
            case OK -> ctx.redirect("/dashboard?gemt=forloeb-afsluttet");
            case ROUND_IN_PROGRESS -> ctx.redirect("/dashboard?fejl=runde-i-gang-forloeb");
            case NO_ACTIVE_JOURNEY -> ctx.redirect("/dashboard");
            default -> ctx.redirect("/dashboard?fejl=ukendt");
        }
    }

    // POST /afslut-runde
    private void endRound(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }

        // resultatet er valgfrit (POSITIVE, NEGATIVE eller tomt).
        // En ukendt værdi giver en fejlbesked i stedet for en 500-fejl
        String resultParam = ctx.formParam("result");
        Result result;
        try {
            result = (resultParam == null || resultParam.isBlank()) ? null : Result.valueOf(resultParam);
        } catch (IllegalArgumentException e) {
            ctx.redirect("/dashboard?fejl=ukendt");
            return;
        }

        ServiceResult outcome = roundService.endRound(patientId, result);

        switch (outcome) {
            case OK -> ctx.redirect("/rundehistorik?gemt=afsluttet");
            case NO_ACTIVE_ROUND -> ctx.redirect("/dashboard?fejl=ingen-runde");
            default -> ctx.redirect("/dashboard?fejl=ukendt");
        }
    }
}