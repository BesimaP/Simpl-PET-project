package services;

import dao.DatabaseConnection;
import dao.RoundDAO;
import entities.FertilityJourney;
import entities.Round;
import enums.Result;
import enums.RoundStatus;
import enums.ServiceResult;
import enums.TreatmentType;

import java.time.LocalDate;

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

        // 1. find det aktive forløb – uden forløb er der ingen skuffe at lægge runden i
        FertilityJourney journey = new DashboardService().findActiveJourney(patientId);
        if (journey == null) {
            return ServiceResult.NO_ACTIVE_JOURNEY;
        }

        // 2. regel: kun én runde i gang ad gangen
        if (roundDao.findActiveByJourney(journey.getId()) != null) {
            return ServiceResult.ROUND_IN_PROGRESS;
        }

        // 3. rundenummer = antal runder i forløbet + 1 (første runde bliver nr. 1)
        int roundNumber = roundDao.findByJourney(journey.getId()).size() + 1;

        // 4. byg kortet og gem. end_date og result er null, til runden afsluttes
        Round round = new Round(0, journey.getId(), roundNumber, TreatmentType.valueOf(type),
                LocalDate.parse(startDate), null, RoundStatus.IN_PROGRESS, null);
        roundDao.save(round);

        return ServiceResult.OK; // ok
    }


    // Afslutter den runde, der er i gang. result må være null (kan udfyldes senere).
    // Svar: OK · NO_ACTIVE_ROUND = der var ikke nogen runde at afslutte
    public ServiceResult endRound(int patientId, Result result) {
        RoundDAO roundDao = new RoundDAO(DatabaseConnection.getConnection());

        // 1. find forløbet og den runde, der er i gang
        FertilityJourney journey = new DashboardService().findActiveJourney(patientId);
        Round round = (journey == null) ? null : roundDao.findActiveByJourney(journey.getId());
        if (round == null) {
            return ServiceResult.NO_ACTIVE_ROUND;
        }

        // 2. afslut den: slutdato = i dag, status COMPLETED (UPDATE i databasen)
        roundDao.endRound(round.getId(), LocalDate.now(), result);

        return ServiceResult.OK; // ok
    }
}
