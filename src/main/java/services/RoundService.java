package services;

import entities.FertilityJourney;
import entities.Round;
import enums.Result;
import enums.RoundStatus;
import enums.ServiceResult;
import enums.TreatmentType;
import enums.EventType;
import exceptions.DatabaseException;
import exceptions.NoActiveJourneyException;
import exceptions.NoActiveRoundException;
import persistence.ConnectionPool;
import persistence.RoundMapper;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Logik for runder (US10a/10b): start og afslut en runde og find den, der er i gang.
public class RoundService {

    private RoundMapper roundMapper;
    private DashboardService dashboardService;
    private TimelineService timelineService;

    public RoundService(ConnectionPool connectionPool) {
        this.roundMapper = new RoundMapper(connectionPool);
        this.dashboardService = new DashboardService(connectionPool);
        this.timelineService = new TimelineService(connectionPool);
    }

    // Starter en ny runde i det aktive forløb.
    // Svar: OK, INVALID_INPUT, NO_ACTIVE_JOURNEY eller ROUND_IN_PROGRESS
    public ServiceResult startRound(int patientId, String type, String startDate) {
        // begge felter skal være udfyldt
        if (type == null || type.isBlank() || startDate == null || startDate.isBlank()) {
            return ServiceResult.INVALID_INPUT;
        }


        // en runde skal ligge i et forløb
        FertilityJourney journey;
        try {
            journey = dashboardService.findActiveJourney(patientId);
        } catch (NoActiveJourneyException e) {
            return ServiceResult.NO_ACTIVE_JOURNEY;
        }

        // kun én runde i gang ad gangen
        if (roundMapper.findActiveByJourney(journey.getId()) != null) {
            return ServiceResult.ROUND_IN_PROGRESS;
        }

        // første runde bliver nr. 1
        int roundNumber = roundMapper.findByJourney(journey.getId()).size() + 1;

        // ukendt type eller ugyldig dato giver INVALID_INPUT i stedet for et crash
        TreatmentType treatmentType;
        LocalDate start;
        try {
            treatmentType = TreatmentType.valueOf(type);
            start = LocalDate.parse(startDate);
        } catch (IllegalArgumentException | DateTimeParseException e) {
            return ServiceResult.INVALID_INPUT;
        }

        // runden må ikke starte før forløbet eller i fremtiden. Slutdatoen bliver dags dato,
        // og databasen afviser en slutdato før startdatoen
        if (start.isBefore(journey.getStartDate()) || start.isAfter(LocalDate.now())) {
            return ServiceResult.INVALID_INPUT;
        }

        // end_date og result er null, til runden afsluttes
        Round round = new Round(0, journey.getId(), roundNumber, treatmentType, start, null, RoundStatus.IN_PROGRESS, null);
        // ved dobbeltklik afviser databasens unikke indeks (one_active_round_per_journey) den anden runde
        try {
            roundMapper.save(round); // save sætter round.id, som vi bruger nedenfor
        } catch (DatabaseException e) {
            if (e.isDuplicate()) {
                return ServiceResult.ROUND_IN_PROGRESS;
            }
            throw e;
        }

        // første trin på tidslinjen (US2). Kl. 00:00, fordi Event bruger LocalDateTime
        timelineService.addEvent(round.getId(), start.atStartOfDay(), EventType.STIMULATION_START, treatmentType.name());

        return ServiceResult.OK;
    }


    // Afslutter runden, der er i gang. result må være null, hvis svaret ikke er kommet endnu.
    // Svar: OK eller NO_ACTIVE_ROUND
    public ServiceResult endRound(int patientId, Result result) {

        Round round;
        try {
            round = findActiveRound(patientId);
        } catch (NoActiveJourneyException | NoActiveRoundException e) {
            return ServiceResult.NO_ACTIVE_ROUND; // intet forløb betyder heller ingen runde
        }

        // slutdato = i dag. Der er ingen status-kolonne – med en slutdato er runden afsluttet
        roundMapper.endRound(round.getId(), LocalDate.now(), result);

        return ServiceResult.OK;
    }

    // Finder runden, der er i gang. Kaster en exception, hvis der ikke er et forløb eller en runde.
    // Bruges af hormoner, medicin, aftaler og endRound
    public Round findActiveRound(int patientId) throws NoActiveJourneyException, NoActiveRoundException {
        FertilityJourney journey = dashboardService.findActiveJourney(patientId);
        Round round = roundMapper.findActiveByJourney(journey.getId());
        if (round == null) {
            throw new NoActiveRoundException("Der er ingen runde i gang");
        }
        return round;
    }

    // Startdatoen på runden, der er i gang, eller null. Bruges som min på datofelterne i hormoner.html og medicin.html
    public LocalDate getActiveRoundStart(int patientId) {
        try {
            return findActiveRound(patientId).getStartDate();
        } catch (NoActiveJourneyException | NoActiveRoundException e) {
            return null;
        }
    }

    // forløbets id -> dets runder. LinkedHashMap holder rækkefølgen, så forløbene står nyeste først
    public Map<Integer, List<Round>> getRoundsPerJourney(int patientId) {
        Map<Integer, List<Round>> rounds = new LinkedHashMap<>();
        for (FertilityJourney journey : dashboardService.getJourneys(patientId)) {
            rounds.put(journey.getId(), roundMapper.findByJourney(journey.getId()));
        }
        return rounds;
    }

    // Finder en af patientens egne runder – null, hvis den ikke findes eller er en andens.
    // Bruges af tidslinjen, så man kan se en gammel runde (?runde= i adressen)
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
}
