package services;

import dao.DatabaseConnection;
import dao.FertilityJourneyDAO;
import entities.FertilityJourney;
import enums.JourneyStatus;
import enums.ServiceResult;
import exceptions.NoActiveJourneyException;
import persistence.ConnectionPool;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

// Forretningslogik for forløb (FertilityJourney, US1). Kender IKKE Javalin.
public class DashboardService {
    private ConnectionPool connectionPool; // nøgleringen (bruges, når forløb laves om til PostgreSQL)

    // ny konstruktør: med nøgleringen
    public DashboardService(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    // MIDLERTIDIG: tom konstruktør, så de andre filer stadig virker, indtil de også får nøgleringen
    public DashboardService() {
    }

    // Opretter et forløb til patienten. Regel: kun ét aktivt forløb ad gangen.
    // OK = oprettet, ALREADY_EXISTS = der var allerede et aktivt forløb, INVALID_INPUT = ingen dato
    public ServiceResult createJourney(int patientId, String startDate) {
        // 1. regel: startdatoen skal være udfyldt og være en rigtig dato (try/catch: ugyldig dato = INVALID_INPUT, ikke crash)
        if (startDate == null || startDate.isBlank()) {
            return ServiceResult.INVALID_INPUT;
        }
        LocalDate start;
        try {
            start = LocalDate.parse(startDate);
        } catch (DateTimeParseException e) {
            return ServiceResult.INVALID_INPUT;
        }

        // arkivaren til fertility_journey-skuffen
        FertilityJourneyDAO journeyDao = new FertilityJourneyDAO(DatabaseConnection.getConnection());

        // 2. regel: har patienten allerede et aktivt forløb, må der ikke oprettes et nyt
        if (journeyDao.findActiveByPatient(patientId) != null) {
            return ServiceResult.ALREADY_EXISTS;
        }

        // 3. byg kortet (id 0 = databasen finder på et) og læg det i skuffen. Status er ACTIVE fra start
        journeyDao.save(new FertilityJourney(0, patientId, start, JourneyStatus.ACTIVE));
        return ServiceResult.OK;
    }

    // Finder patientens aktive forløb. Findes der ikke et, KASTES NoActiveJourneyException (i stedet for at returnere null,
    // som ikke siger hvorfor). Bruges af de andre services (runde, hormoner, dagbog, aftaler, tidslinje) og senere af dashboard-visningen
    public FertilityJourney findActiveJourney(int patientId) throws NoActiveJourneyException {
        FertilityJourneyDAO journeyDao = new FertilityJourneyDAO(DatabaseConnection.getConnection());
        FertilityJourney journey = journeyDao.findActiveByPatient(patientId);
        if (journey == null) {
            throw new NoActiveJourneyException("Du har ikke et aktivt forløb");
        }
        return journey;
    }
}
