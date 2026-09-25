package services;

import dao.DatabaseConnection;
import dao.RoundDAO;
import entities.FertilityJourney;
import entities.Round;
import enums.Result;
import enums.RoundStatus;
import enums.ServiceResult;
import enums.TreatmentType;
import enums.EventType;
import exceptions.NoActiveJourneyException;
import exceptions.NoActiveRoundException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

// Forretningslogik for runder (Round, US10a/10b). Kender IKKE Javalin.
// Metoderne svarer med ServiceResult. Controlleren vælger side ud fra svaret.
public class RoundService {

    // Starter en ny runde i patientens aktive forløb.
    // Svar: OK · INVALID_INPUT = tomme felter · NO_ACTIVE_JOURNEY = intet aktivt forløb · ROUND_IN_PROGRESS = der er allerede en runde
    public ServiceResult startRound(int patientId, String type, String startDate) {
        // 0. regel: begge felter skal være udfyldt
        if (type == null || type.isBlank() || startDate == null || startDate.isBlank()) {
            return ServiceResult.INVALID_INPUT;
        }

        // arkivaren til round-skuffen
        RoundDAO roundDao = new RoundDAO(DatabaseConnection.getConnection());

        // 1. find det aktive forløb – uden forløb er der ingen skuffe at lægge runden i.
        //    findActiveJourney kaster, hvis der ikke er et – vi fanger og oversætter til et ServiceResult
        FertilityJourney journey;
        try {
            journey = new DashboardService().findActiveJourney(patientId);
        } catch (NoActiveJourneyException e) {
            return ServiceResult.NO_ACTIVE_JOURNEY;
        }

        // 2. regel: kun én runde i gang ad gangen
        if (roundDao.findActiveByJourney(journey.getId()) != null) {
            return ServiceResult.ROUND_IN_PROGRESS;
        }

        // 3. rundenummer = antal runder i forløbet + 1 (første runde bliver nr. 1)
        int roundNumber = roundDao.findByJourney(journey.getId()).size() + 1;

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
        roundDao.save(round); // save sætter round.id til det id, databasen gav

        // 6. første trin på tidslinjen: runden er startet (US2). Startdato kl. 00:00, fordi Event bruger LocalDateTime
        new TimelineService().addEvent(round.getId(), start.atStartOfDay(), EventType.STIMULATION_START, treatmentType.name());

        return ServiceResult.OK; // ok
    }


    // Afslutter den runde, der er i gang. result må være null (kan udfyldes senere).
    // Svar: OK · NO_ACTIVE_ROUND = der var ikke nogen runde at afslutte
    public ServiceResult endRound(int patientId, Result result) {
        RoundDAO roundDao = new RoundDAO(DatabaseConnection.getConnection());

        // 1. find den runde, der er i gang – findActiveRound kaster, hvis der ikke er forløb eller runde
        Round round;
        try {
            round = findActiveRound(patientId);
        } catch (NoActiveJourneyException | NoActiveRoundException e) {
            return ServiceResult.NO_ACTIVE_ROUND; // intet forløb = heller ingen runde at afslutte
        }

        // 2. afslut den: slutdato = i dag, status COMPLETED (UPDATE i databasen)
        roundDao.endRound(round.getId(), LocalDate.now(), result);

        return ServiceResult.OK; // ok
    }

    // Finder den runde, der er i gang i patientens aktive forløb. KASTER, hvis der ikke er et forløb (NoActiveJourneyException)
    // eller ingen runde i gang (NoActiveRoundException). Bruges af hormoner, medicin, tidslinje og endRound
    public Round findActiveRound(int patientId) throws NoActiveJourneyException, NoActiveRoundException {
        FertilityJourney journey = new DashboardService().findActiveJourney(patientId); // kaster videre, hvis intet forløb
        Round round = new RoundDAO(DatabaseConnection.getConnection()).findActiveByJourney(journey.getId());
        if (round == null) {
            throw new NoActiveRoundException("Der er ingen runde i gang");
        }
        return round;
    }

    // Henter alle runder i patientens aktive forløb, ældste først – til rundehistorik (GET). Intet forløb -> tom liste
    public List<Round> getRounds(int patientId) {
        try {
            FertilityJourney journey = new DashboardService().findActiveJourney(patientId);
            return new RoundDAO(DatabaseConnection.getConnection()).findByJourney(journey.getId());
        } catch (NoActiveJourneyException e) {
            return new ArrayList<>();
        }
    }
}
