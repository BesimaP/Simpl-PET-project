package services;

import entities.FertilityJourney;
import enums.JourneyStatus;
import enums.ServiceResult;
import exceptions.DatabaseException;
import exceptions.NoActiveJourneyException;
import persistence.ConnectionPool;
import persistence.FertilityJourneyMapper;
import persistence.RoundMapper;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

// Logik for forløb (FertilityJourney, US1).
public class DashboardService {
    private FertilityJourneyMapper journeyMapper;
    private RoundMapper roundMapper;   // et forløb må ikke afsluttes, mens en runde er i gang

    public DashboardService(ConnectionPool connectionPool) {
        this.journeyMapper = new FertilityJourneyMapper(connectionPool);
        this.roundMapper = new RoundMapper(connectionPool);
    }

    // Opretter et forløb. Patienten må kun have ét aktivt forløb ad gangen.
    // Svar: OK, INVALID_INPUT eller ALREADY_EXISTS
    public ServiceResult createJourney(int patientId, String startDate) {
        // startdatoen skal være udfyldt og være en rigtig dato
        if (startDate == null || startDate.isBlank()) {
            return ServiceResult.INVALID_INPUT;
        }
        LocalDate start;
        try {
            start = LocalDate.parse(startDate);
        } catch (DateTimeParseException e) {
            return ServiceResult.INVALID_INPUT;
        }
        // et forløb må ikke starte i fremtiden – så kunne man ikke starte en runde før den dato
        if (start.isAfter(LocalDate.now())) {
            return ServiceResult.INVALID_INPUT;
        }

        if (journeyMapper.findActiveByPatient(patientId) != null) {
            return ServiceResult.ALREADY_EXISTS;
        }

        // ved dobbeltklik afviser databasens unikke indeks (one_active_journey_per_patient) det andet forløb
        try {
            journeyMapper.save(new FertilityJourney(0, patientId, start, JourneyStatus.ACTIVE));
        } catch (DatabaseException e) {
            if (e.isDuplicate()) {
                return ServiceResult.ALREADY_EXISTS;
            }
            throw e;
        }
        return ServiceResult.OK;
    }

    // Afslutter det aktive forløb, så patienten kan starte et nyt senere (US1).
    // Svar: OK, NO_ACTIVE_JOURNEY eller ROUND_IN_PROGRESS (runden skal afsluttes først)
    public ServiceResult endJourney(int patientId) {
        FertilityJourney journey = journeyMapper.findActiveByPatient(patientId);
        if (journey == null) {
            return ServiceResult.NO_ACTIVE_JOURNEY;
        }

        // ellers ville runden hænge "i gang" i et afsluttet forløb
        if (roundMapper.findActiveByJourney(journey.getId()) != null) {
            return ServiceResult.ROUND_IN_PROGRESS;
        }

        journeyMapper.endJourney(journey.getId());
        return ServiceResult.OK;
    }

    // Alle patientens forløb, nyeste først – til rundehistorik og valg af forløb på aftaler
    public List<FertilityJourney> getJourneys(int patientId) {
        return journeyMapper.findByPatient(patientId);
    }

    // Finder et af patientens egne forløb – null, hvis det ikke findes eller tilhører en anden
    // (så man ikke kan se andres aftaler ved at rette ?forloeb= i adressen)
    public FertilityJourney findJourney(int patientId, int journeyId) {
        for (FertilityJourney j : getJourneys(patientId)) {
            if (j.getId() == journeyId) {
                return j;
            }
        }
        return null;
    }

    // Finder det aktive forløb. Vi kaster en exception i stedet for at give null, så den, der kalder, ved hvorfor
    public FertilityJourney findActiveJourney(int patientId) throws NoActiveJourneyException {
        FertilityJourney journey = journeyMapper.findActiveByPatient(patientId);
        if (journey == null) {
            throw new NoActiveJourneyException("Du har ikke et aktivt forløb");
        }
        return journey;
    }
}
