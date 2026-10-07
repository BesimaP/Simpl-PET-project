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

// Koordinatoren for dashboard, start-runde og rundehistorik.
// Ruterne er én linje hver. Metoderne læser formularen, kalder service og sender brugeren videre – ingen mappers her.
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
        config.routes.get("/dashboard", ctx -> showDashboard(ctx));        // forsiden efter login (Thymeleaf)
        config.routes.get("/rundehistorik", ctx -> showRounds(ctx));       // alle runder (Thymeleaf)
        config.routes.post("/opret-forloeb", ctx -> createJourney(ctx));   // "Start dit forløb" på dashboard (US1)
        config.routes.get("/start-runde", ctx -> showStartRound(ctx));     // siden med formularen (Thymeleaf)
        config.routes.post("/start-runde", ctx -> startRound(ctx));        // "Start runde" på start-runde.html (US10a)
        config.routes.get("/afslut-runde", ctx -> showEndRound(ctx));      // bekræft-side – bruges, når JavaScript er slået fra
        config.routes.post("/afslut-runde", ctx -> endRound(ctx));         // bekræft i dialogen på dashboard eller på bekræft-siden (US10a)
        config.routes.get("/afslut-forloeb", ctx -> showEndJourney(ctx));  // bekræft-side for "Afslut forløb"
        config.routes.post("/afslut-forloeb", ctx -> endJourney(ctx));     // afslut forløbet (US1: flere forløb over tid)
    }

    // GET /dashboard – samler data fra alle emner og fylder skabelonen. Tre tilstande: intet forløb, forløb uden runde, runde i gang
    private void showDashboard(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }

        ctx.attribute("patient", profileService.getPatient(patientId));
        ctx.attribute("today", LocalDate.now());

        // påmindelser om dagens medicin oprettes, når forsiden åbnes (US12). Tallet bruges til prikken på klokken
        notificationService.createMedicationReminders(patientId);
        notificationService.createAppointmentReminders(patientId);   // aftaler i dag og i morgen
        ctx.attribute("unread", notificationService.countUnread(patientId));
        ctx.attribute("notifications", notificationService.getNotifications(patientId));   // til pop-op'en ved klokken

        // forløb og runde: null, hvis de ikke findes -> skabelonen viser den rigtige tilstand med th:if
        FertilityJourney journey = null;
        Round round = null;
        try {
            journey = dashboardService.findActiveJourney(patientId);
            round = roundService.findActiveRound(patientId);
        } catch (NoActiveJourneyException | NoActiveRoundException e) {
            // ingen fejl – bare en af de to tilstande
        }
        ctx.attribute("journey", journey);
        ctx.attribute("round", round);
        if (round != null) {
            // dag-nummer i runden: dage siden start + 1
            long dayNumber = ChronoUnit.DAYS.between(round.getStartDate(), LocalDate.now()) + 1;
            ctx.attribute("dayNumber", dayNumber);
            // ringen om dag-tælleren: hvor mange procent af ca. 28 dage er gået? Mellem 0 og 100
            // (0 hvis runden starter i fremtiden, højst 100 så ringen ikke "løber over")
            ctx.attribute("progressPercent", Math.max(0, Math.min(100, dayNumber * 100 / 28)));
        }

        // små kort: dagens medicin, næste aftale, seneste hormonværdi, antal noter
        ctx.attribute("todayMeds", medicationService.getTodayLogs(patientId));
        ctx.attribute("names", medicationService.getMedicationNames());
        List<Appointment> upcoming = appointmentService.getUpcoming(patientId);
        ctx.attribute("nextAppointment", upcoming.isEmpty() ? null : upcoming.get(0));   // første = nærmeste
        List<HormoneLog> logs = hormoneService.getLogs(patientId);
        ctx.attribute("latestHormone", logs.isEmpty() ? null : logs.get(0));             // nyeste først
        ctx.attribute("noteCount", diaryService.getEntries(patientId).size());

        // besked fra sidste POST (?gemt= / ?fejl= i URL'en) -> request scope -> fragmentet besked.html
        ctx.attribute("gemt", ctx.queryParam("gemt"));
        ctx.attribute("fejl", ctx.queryParam("fejl"));
        ctx.render("dashboard");
    }

    // GET /rundehistorik – hent alle runder og fyld skabelonen
    private void showRounds(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        // ALLE forløb (nyeste først) og runderne i hvert af dem – så afsluttede forløb også kan ses
        ctx.attribute("journeys", dashboardService.getJourneys(patientId));             // requestscope -> ${journeys}
        ctx.attribute("roundsPerJourney", roundService.getRoundsPerJourney(patientId)); // requestscope -> ${roundsPerJourney[j.id]}
        // besked fra sidste POST (?gemt= / ?fejl= i URL'en) -> request scope -> fragmentet besked.html
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

        // åbn kuverten: startdatoen fra formularen
        String startDate = ctx.formParam("startDate");

        // bed service oprette forløbet – den kender reglen "kun ét aktivt"
        ServiceResult result = dashboardService.createJourney(patientId, startDate);

        // dashboard viser selv "start dit forløb"-delen, hvis der stadig intet forløb er
        switch (result) {
            case OK, ALREADY_EXISTS -> ctx.redirect("/dashboard?gemt=forloeb");   // der findes nu et aktivt forløb
            case INVALID_INPUT -> ctx.redirect("/dashboard?fejl=dato");
            default -> ctx.redirect("/dashboard?fejl=ukendt");
        }
    }

    // GET /start-runde – vis formularen. ?fejl= fra sidste POST vises med fragmentet besked.html
    private void showStartRound(Context ctx) {
        Integer patientId = ctx.sessionAttribute("patientId");
        if (patientId == null) {
            ctx.redirect("/login");
            return;
        }
        ctx.attribute("today", LocalDate.now());   // startdato er udfyldt med dags dato (kan ændres)
        ctx.attribute("treatmentTypes", TreatmentType.values());   // dropdownen bygges af enum'en (NFR5: ny type = kun ét sted i Java)
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

        // åbn kuverten: behandlingstype ("IVF", "ICSI", "IUI", "FET" = value i dropdownen) og startdato
        String type = ctx.formParam("type");
        String startDate = ctx.formParam("startDate");

        ServiceResult result = roundService.startRound(patientId, type, startDate);

        // vælg side ud fra svaret
        switch (result) {
            case OK -> ctx.redirect("/dashboard?gemt=runde");
            case NO_ACTIVE_JOURNEY -> ctx.redirect("/dashboard");                    // dashboard viser "start dit forløb"
            case ROUND_IN_PROGRESS -> ctx.redirect("/dashboard?fejl=runde-i-gang");
            case INVALID_INPUT -> ctx.redirect("/start-runde?fejl=felter");         // tilbage til formularen med en fejlbesked
            default -> ctx.redirect("/dashboard?fejl=ukendt");
        }
    }

    // GET /afslut-runde – bekræft-side uden JavaScript. Med JavaScript åbner dashboard.js i stedet dialogen på dashboard.
    // Skabelonen bekraeft.html deles med "Afslut forløb" og "Slet konto" – teksterne lægges i request scope her
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
            ctx.redirect("/dashboard?fejl=ingen-runde");   // der er ingen runde at afslutte
            return;
        }
        ctx.attribute("title", "Afslut runde " + round.getRoundNumber() + "?");
        ctx.attribute("text", "Runden markeres som afsluttet, og du kan starte en ny. Dine logs og noter gemmes.");
        ctx.attribute("action", "/afslut-runde");
        ctx.attribute("button", "Afslut runde");
        ctx.attribute("cancel", "/dashboard");
        ctx.attribute("showResult", true);   // kun her: vælg resultat (Positiv/Negativ/Ikke afgjort)
        ctx.render("bekraeft");
    }

    // GET /afslut-forloeb – bekræft-side for "Afslut forløb"
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

        // bed service afslutte forløbet – den kender reglen "afslut runden først"
        ServiceResult result = dashboardService.endJourney(patientId);

        switch (result) {
            case OK -> ctx.redirect("/dashboard?gemt=forloeb-afsluttet");       // dashboard viser nu "Start dit forløb"
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

        // resultatet er valgfrit: "POSITIVE", "NEGATIVE" eller tomt (kan udfyldes senere)
        // try/catch: en ukendt værdi (fx rettet i browseren) giver en fejlbesked i stedet for en 500-fejl
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