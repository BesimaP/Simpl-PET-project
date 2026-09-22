package service;

import dao.DatabaseConnection;
import dao.FertilityJourneyDAO;
import entities.FertilityJourney;
import enums.JourneyStatus;
import enums.ServiceResult;

import java.time.LocalDate;

// Forretningslogik for forløb (FertilityJourney, US1). Kender IKKE Javalin.
public class DashboardService {

    // Opretter et forløb til patienten. Regel: kun ét aktivt forløb ad gangen.
    // true = oprettet, false = der var allerede et aktivt forløb (så blev intet gemt)
    public boolean createJourney(int patientId, String startDate) {
        // arkivaren til fertility_journey-skuffen
        FertilityJourneyDAO journeyDao = new FertilityJourneyDAO(DatabaseConnection.getConnection());

        // har patienten allerede et aktivt forløb? så må der ikke oprettes et nyt
        if (journeyDao.findActiveByPatient(patientId) != null) {
            return ServiceResult.ALREADY_EXISTS;
        }

        // byg kortet (id 0 = databasen finder på et) og læg det i skuffen
        journeyDao.save(new FertilityJourney(0, patientId, LocalDate.parse(startDate), JourneyStatus.ACTIVE));
        return ServiceResult.OK;
    }

    // Finder patientens aktive forløb – null, hvis der ikke er noget. Bruges af RoundService og senere af dashboard-visningen
    public FertilityJourney findActiveJourney(int patientId) {
        FertilityJourneyDAO journeyDao = new FertilityJourneyDAO(DatabaseConnection.getConnection());
        return journeyDao.findActiveByPatient(patientId);
    }
}
