package services;
import entities.Medication;
import entities.MedicationLog;
import entities.Round;
import enums.ServiceResult;
import exceptions.NoActiveJourneyException;
import exceptions.NoActiveRoundException;
import persistence.ConnectionPool;
import persistence.MedicationLogMapper;
import persistence.MedicationMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Logik for medicin og doser (US8).
public class MedicationService {

    private MedicationMapper medicationMapper;
    private MedicationLogMapper medicationLogMapper;
    private RoundService roundService;

    public MedicationService(ConnectionPool connectionPool){
        this.medicationMapper = new MedicationMapper(connectionPool);
        this.medicationLogMapper = new MedicationLogMapper(connectionPool);
        this.roundService = new RoundService(connectionPool);
    }

    // Logger en dosis i den aktive runde. Enheden vælger patienten ikke selv – den følger lægemidlet
    public ServiceResult logDose(int patientId, String medicationName, String dose, String date, String time, boolean taken) {

        // alle felter skal være udfyldt
        if (InputCheck.isBlank(medicationName) || InputCheck.isBlank(dose) || InputCheck.isBlank(date) || InputCheck.isBlank(time)) {
            return ServiceResult.INVALID_INPUT;
        }

        // ugyldigt tal eller dato giver INVALID_INPUT i stedet for et crash
        double doseValue;
        LocalDateTime scheduled;
        try {
            doseValue = Double.parseDouble(dose);
            scheduled = LocalDateTime.of(LocalDate.parse(date), LocalTime.parse(time));
        } catch (NumberFormatException | DateTimeParseException e) {
            return ServiceResult.INVALID_INPUT;
        }

        // dosis skal være over 0 og passe i NUMERIC(10,2). parseDouble accepterer "NaN" og "Infinity", så dem tjekker vi også
        if (!Double.isFinite(doseValue) || doseValue <= 0 || doseValue >= 100_000_000) {
            return ServiceResult.INVALID_INPUT;
        }

        // ukendt lægemiddel = ugyldigt input
        Medication medication = medicationMapper.findByName(medicationName);
        if (medication == null) {
            return ServiceResult.INVALID_INPUT;
        }

        Round round;
        try {
           round = roundService.findActiveRound(patientId);
        } catch (NoActiveJourneyException e) {
            return ServiceResult.NO_ACTIVE_JOURNEY;
        } catch (NoActiveRoundException e) {
            return ServiceResult.NO_ACTIVE_ROUND;
        }

        // en dosis før runden startede hører ikke til denne runde
        if (scheduled.toLocalDate().isBefore(round.getStartDate())) {
            return ServiceResult.INVALID_INPUT;
        }

        // taken = false betyder, at dosen kun er planlagt
        medicationLogMapper.
                save(new MedicationLog(0, round.getId(), medication.getId(), scheduled, doseValue, medication.getUnit(), taken));

        return ServiceResult.OK;
    }

    // Doser i dag i den runde, der er i gang – til listen "I dag"
    public List<MedicationLog> getTodayLogs(int patientId) {
        List<MedicationLog> today = new ArrayList<>();
        for (MedicationLog m : getAll(patientId)) {
            if (m.getScheduledDateTime().toLocalDate().equals(LocalDate.now())) {
                today.add(m);
            }
        }
        return today;
    }

    // Doser før i dag – til listen "Tidligere"
    public List<MedicationLog> getPastLogs(int patientId) {
        List<MedicationLog> past = new ArrayList<>();
        for (MedicationLog m : getAll(patientId)) {
            if (m.getScheduledDateTime().toLocalDate().isBefore(LocalDate.now())) {
                past.add(m);
            }
        }
        return past;
    }

    // Doser fra i morgen og frem – til listen "Kommende"
    public List<MedicationLog> getUpcomingLogs(int patientId) {
        List<MedicationLog> upcoming = new ArrayList<>();
        for (MedicationLog m : getAll(patientId)) {
            if (m.getScheduledDateTime().toLocalDate().isAfter(LocalDate.now())) {
                upcoming.add(m);
            }
        }
        return upcoming;
    }

    // Alle lægemidler til dropdownen på medicin-siden
    public List<Medication> getMedications() {
        return medicationMapper.findAll();
    }

    // id -> navn (fx 1 -> "Gonal-F"), fordi doserne kun har medicationId
    public Map<Integer, String> getMedicationNames() {
        Map<Integer, String> names = new HashMap<>();
        for (Medication med : medicationMapper.findAll()) {
            // description er det pæne navn, name er koden ("GONAL_F"). Mangler description, bruger vi koden,
            // så en påmindelse ikke kommer til at hedde "null · 150 IU"
            String name = med.getDescription() != null ? med.getDescription() : med.getName();
            names.put(med.getId(), name);
        }
        return names;
    }

    // Markér en dosis som taget (US8 AC3). Ejerskabet tjekkes i SQL'en – ellers NOT_FOUND
    public ServiceResult markTaken(int patientId, int logId) {
        return medicationLogMapper.markTaken(logId, patientId) ? ServiceResult.OK : ServiceResult.NOT_FOUND;
    }

    // Fortryd "taget", så dosen er planlagt igen
    public ServiceResult markNotTaken(int patientId, int logId) {
        return medicationLogMapper.markNotTaken(logId, patientId) ? ServiceResult.OK : ServiceResult.NOT_FOUND;
    }

    // Sletter en dosis, fx en fejlindtastning
    public ServiceResult deleteDose(int patientId, int logId) {
        return medicationLogMapper.delete(logId, patientId) ? ServiceResult.OK : ServiceResult.NOT_FOUND;
    }

    // alle doser i den runde, der er i gang. Ingen runde giver en tom liste
    private List<MedicationLog> getAll(int patientId) {
        try {
           Round round =  roundService.findActiveRound(patientId);
            return medicationLogMapper.findByRound(round.getId());
        } catch (NoActiveJourneyException | NoActiveRoundException e) {
            return new ArrayList<>();
        }
    }
}