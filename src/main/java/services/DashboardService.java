package services;

import entities.FertilityJourney;
import enums.JourneyStatus;
import enums.ServiceResult;
import exceptions.NoActiveJourneyException;
import persistence.ConnectionPool;
import persistence.FertilityJourneyMapper;
import persistence.RoundMapper;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

// Forretningslogik for forløb (FertilityJourney, US1). Kender IKKE Javalin.
public class DashboardService {
    private FertilityJourneyMapper journeyMapper;
    private RoundMapper roundMapper;   // til reglen "et forløb kan ikke afsluttes, mens en runde er i gang"

    // ny konstruktør: med nøgleringen
    public DashboardService(ConnectionPool connectionPool) {
        this.journeyMapper = new FertilityJourneyMapper(connectionPool);
        this.roundMapper = new RoundMapper(connectionPool);
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


        // 2. regel: har patienten allerede et aktivt forløb, må der ikke oprettes et nyt
        if (journeyMapper.findActiveByPatient(patientId) != null) {
            return ServiceResult.ALREADY_EXISTS;
        }

        // 3. byg kortet (id 0 = databasen finder på et) og læg det i skuffen. Status er ACTIVE fra start
        journeyMapper.save(new FertilityJourney(0, patientId, start, JourneyStatus.ACTIVE));
        return ServiceResult.OK;
    }

    // Afslutter patientens aktive forløb (status COMPLETED). Bagefter viser dashboard "Start dit forløb" igen,
    // så patienten kan oprette et nyt forløb (US1: "en patient kan godt have flere forløb over tid").
    // Svar: OK · NO_ACTIVE_JOURNEY = der er intet forløb at afslutte · ROUND_IN_PROGRESS = afslut runden først
    public ServiceResult endJourney(int patientId) {
        // 1. find det aktive forløb
        FertilityJourney journey = journeyMapper.findActiveByPatient(patientId);
        if (journey == null) {
            return ServiceResult.NO_ACTIVE_JOURNEY;
        }

        // 2. regel: en runde i gang skal afsluttes først (ellers ville den hænge "i gang" i et afsluttet forløb)
        if (roundMapper.findActiveByJourney(journey.getId()) != null) {
            return ServiceResult.ROUND_IN_PROGRESS;
        }

        // 3. sæt status til COMPLETED
        journeyMapper.endJourney(journey.getId());
        return ServiceResult.OK;
    }

    // Finder patientens aktive forløb. Findes der ikke et, KASTES NoActiveJourneyException (i stedet for at returnere null,
    // som ikke siger hvorfor). Bruges af de andre services (runde, hormoner, dagbog, aftaler, tidslinje) og senere af dashboard-visningen
    public FertilityJourney findActiveJourney(int patientId) throws NoActiveJourneyException {
        FertilityJourney journey = journeyMapper.findActiveByPatient(patientId);
        if (journey == null) {
            throw new NoActiveJourneyException("Du har ikke et aktivt forløb");
        }
        return journey;
    }
}
