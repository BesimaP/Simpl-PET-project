package services;

import entities.FertilityJourney;
import entities.Round;
import enums.Result;
import enums.RoundStatus;
import enums.ServiceResult;
import enums.TreatmentType;
import enums.EventType;
import exceptions.NoActiveJourneyException;
import exceptions.NoActiveRoundException;
import persistence.ConnectionPool;
import persistence.RoundMapper;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Forretningslogik for runder (Round, US10a/10b). Kender IKKE Javalin.
// Metoderne svarer med ServiceResult. Controlleren vælger side ud fra svaret.
public class RoundService {

    private RoundMapper roundMapper;
    private DashboardService dashboardService;
    private TimelineService timelineService;

    public RoundService(ConnectionPool connectionPool) {
        this.roundMapper = new RoundMapper(connectionPool);
        this.dashboardService = new DashboardService(connectionPool);
        this.timelineService = new TimelineService(connectionPool);
    }

    // Starter en ny runde i patientens aktive forløb.
    // Svar: OK · INVALID_INPUT = tomme felter · NO_ACTIVE_JOURNEY = intet aktivt forløb · ROUND_IN_PROGRESS = der er allerede en runde
    public ServiceResult startRound(int patientId, String type, String startDate) {
        // 0. regel: begge felter skal være udfyldt
        if (type == null || type.isBlank() || startDate == null || startDate.isBlank()) {
            return ServiceResult.INVALID_INPUT;
        }


        // 1. find det aktive forløb – uden forløb er der ingen skuffe at lægge runden i.
        //    findActiveJourney kaster, hvis der ikke er et – vi fanger og oversætter til et ServiceResult
        FertilityJourney journey;
        try {
            journey = dashboardService.findActiveJourney(patientId);
        } catch (NoActiveJourneyException e) {
            return ServiceResult.NO_ACTIVE_JOURNEY;
        }

        // 2. regel: kun én runde i gang ad gangen
        if (roundMapper.findActiveByJourney(journey.getId()) != null) {
            return ServiceResult.ROUND_IN_PROGRESS;
        }

        // 3. rundenummer = antal runder i forløbet + 1 (første runde bliver nr. 1)
        int roundNumber = roundMapper.findByJourney(journey.getId()).size() + 1;

        // 4. tekst fra formularen -> enum og dato. try/catch: ukendt type eller ugyldig dato giver INVALID_INPUT i stedet for et crash
        TreatmentType treatmentType;
        LocalDate start;
        try {
            treatmentType = TreatmentType.valueOf(type);
            start = LocalDate.parse(startDate);
        } catch (IllegalArgumentException | DateTimeParseException e) {
            return ServiceResult.INVALID_INPUT;
        }

        // 5. byg kortet og gem. end_date og result er null, til runden afsluttes
        Round round = new Round(0, journey.getId(), roundNumber, treatmentType, start, null, RoundStatus.IN_PROGRESS, null);
        roundMapper.save(round); // save sætter round.id til det id, databasen gav

        // 6. første trin på tidslinjen: runden er startet (US2). Startdato kl. 00:00, fordi Event bruger LocalDateTime
        timelineService.addEvent(round.getId(), start.atStartOfDay(), EventType.STIMULATION_START, treatmentType.name());

        return ServiceResult.OK; // ok
    }


    // Afslutter den runde, der er i gang. result må være null (kan udfyldes senere).
    // Svar: OK · NO_ACTIVE_ROUND = der var ikke nogen runde at afslutte
    public ServiceResult endRound(int patientId, Result result) {

        // 1. find den runde, der er i gang – findActiveRound kaster, hvis der ikke er forløb eller runde
        Round round;
        try {
            round = findActiveRound(patientId);
        } catch (NoActiveJourneyException | NoActiveRoundException e) {
            return ServiceResult.NO_ACTIVE_ROUND; // intet forløb = heller ingen runde at afslutte
        }

        // 2. afslut den: slutdato = i dag, status COMPLETED (UPDATE i databasen)
        roundMapper.endRound(round.getId(), LocalDate.now(), result);

        return ServiceResult.OK; // ok
    }

    // Finder den runde, der er i gang i patientens aktive forløb. KASTER, hvis der ikke er et forløb (NoActiveJourneyException)
    // eller ingen runde i gang (NoActiveRoundException). Bruges af hormoner, medicin, tidslinje og endRound
    public Round findActiveRound(int patientId) throws NoActiveJourneyException, NoActiveRoundException {
        FertilityJourney journey = dashboardService.findActiveJourney(patientId); // kaster videre, hvis intet forløb
        Round round = roundMapper.findActiveByJourney(journey.getId());
        if (round == null) {
            throw new NoActiveRoundException("Der er ingen runde i gang");
        }
        return round;
    }

    // Runderne for ALLE patientens forløb: forløbets id -> dets runder (ældste først).
    // LinkedHashMap husker rækkefølgen, så forløbene står nyeste først, ligesom getJourneys
    public Map<Integer, List<Round>> getRoundsPerJourney(int patientId) {
        Map<Integer, List<Round>> rounds = new LinkedHashMap<>();
        for (FertilityJourney journey : dashboardService.getJourneys(patientId)) {
            rounds.put(journey.getId(), roundMapper.findByJourney(journey.getId()));
        }
        return rounds;
    }

    // Finder én af patientens EGNE runder ud fra id (i alle hendes forløb) – null, hvis den ikke findes eller er en andens.
    // Bruges af tidslinjen, så man kan se en gammel rundes tidslinje (?runde= i adressen)
    public Round findRound(int patientId, int roundId) {
        for (List<Round> list : getRoundsPerJourney(patientId).values()) {
            for (Round r : list) {
                if (r.getId() == roundId) {
                    return r;
                }
            }
        }
        return null;
    }

    // Henter alle runder i patientens aktive forløb, ældste først – til rundehistorik (GET). Intet forløb -> tom liste
    public List<Round> getRounds(int patientId) {
        try {
            FertilityJourney journey = dashboardService.findActiveJourney(patientId);
            return roundMapper.findByJourney(journey.getId());
        } catch (NoActiveJourneyException e) {
            return new ArrayList<>();
        }
    }
}
