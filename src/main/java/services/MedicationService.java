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

// Forretningslogik for medicin (US8). Kender IKKE Javalin – controlleren læser formularen og kalder én metode her.
// Svarer med ServiceResult: OK · INVALID_INPUT = tomt felt/ugyldigt tal, dato eller ukendt medicin · NO_ACTIVE_JOURNEY · NO_ACTIVE_ROUND
public class MedicationService {

    private MedicationMapper medicationMapper;
    private MedicationLogMapper medicationLogMapper;
    private RoundService roundService;

    public MedicationService(ConnectionPool connectionPool){
        this.medicationMapper = new MedicationMapper(connectionPool);
        this.medicationLogMapper = new MedicationLogMapper(connectionPool);
        this.roundService = new RoundService(connectionPool);
    }

    // Logger én dosis i patientens aktive runde (en dosis SKAL ligge på en runde – round_id i databasen)
    // Enheden vælges ikke af patienten – den følger lægemidlet (medication.unit)
    public ServiceResult logDose(int patientId, String medicationName, String dose, String date, String time, boolean taken) {

        // 1. regel: alle felter skal være udfyldt
        if (isBlank(medicationName) || isBlank(dose) || isBlank(date) || isBlank(time)) {
            return ServiceResult.INVALID_INPUT;
        }

        // 2. tekst fra formularen -> rigtige typer. Ugyldigt input giver INVALID_INPUT i stedet for et crash
        double doseValue;
        LocalDateTime scheduled;
        try {
            doseValue = Double.parseDouble(dose);                                       // "150" -> 150.0
            scheduled = LocalDateTime.of(LocalDate.parse(date), LocalTime.parse(time)); // dato + klokkeslæt -> ét tidspunkt
        } catch (NumberFormatException | DateTimeParseException e) {
            return ServiceResult.INVALID_INPUT;
        }

        // 3. regel: dosis skal være et rigtigt tal (ikke "NaN"/"Infinity"), større end 0
        //    og kunne være i kolonnen NUMERIC(10,2) (højst 8 cifre før kommaet)
        if (!Double.isFinite(doseValue) || doseValue <= 0 || doseValue >= 100_000_000) {
            return ServiceResult.INVALID_INPUT;
        }

        // 4. find lægemidlet ud fra navnet (dropdownens value)
        Medication medication = medicationMapper.findByName(medicationName);
        if (medication == null) {
            return ServiceResult.INVALID_INPUT;
        }

        // 5. find den aktive runde – en dosis hører til en runde.
        //    findActiveRound kaster, hvis der ikke er forløb eller runde – vi fanger og oversætter til et ServiceResult
        Round round;
        try {
           round = roundService.findActiveRound(patientId);
        } catch (NoActiveJourneyException e) {
            return ServiceResult.NO_ACTIVE_JOURNEY;
        } catch (NoActiveRoundException e) {
            return ServiceResult.NO_ACTIVE_ROUND;
        }

        // 6. gem – taken = true betyder "allerede taget", false betyder "planlagt"
        medicationLogMapper.
                save(new MedicationLog(0, round.getId(), medication.getId(), scheduled, doseValue, medication.getUnit(), taken));

        return ServiceResult.OK;
    }

    // Doser planlagt i DAG i den runde, der er i gang – til listen "I dag" (GET)
    public List<MedicationLog> getTodayLogs(int patientId) {
        List<MedicationLog> today = new ArrayList<>();
        for (MedicationLog m : getAll(patientId)) {
            if (m.getScheduledDateTime().toLocalDate().equals(LocalDate.now())) {
                today.add(m);
            }
        }
        return today;
    }

    // Doser før i dag – til listen "Tidligere" (GET)
    public List<MedicationLog> getPastLogs(int patientId) {
        List<MedicationLog> past = new ArrayList<>();
        for (MedicationLog m : getAll(patientId)) {
            if (m.getScheduledDateTime().toLocalDate().isBefore(LocalDate.now())) {
                past.add(m);
            }
        }
        return past;
    }

    // Doser planlagt EFTER i dag (fra i morgen og frem) – til listen "Kommende" (GET)
    public List<MedicationLog> getUpcomingLogs(int patientId) {
        List<MedicationLog> upcoming = new ArrayList<>();
        for (MedicationLog m : getAll(patientId)) {
            if (m.getScheduledDateTime().toLocalDate().isAfter(LocalDate.now())) {
                upcoming.add(m);
            }
        }
        return upcoming;
    }

    // Alle lægemidler (navn + enhed) – til dropdownen på medicin-siden, så listen kommer fra databasen
    public List<Medication> getMedications() {
        return medicationMapper.findAll();
    }

    // Opslag id -> navn (fx 1 -> "Gonal-F"), så skabelonen kan vise navnet. Loggen har kun medicationId
    public Map<Integer, String> getMedicationNames() {
        Map<Integer, String> names = new HashMap<>();
        for (Medication med : medicationMapper.findAll()) {
            // description = det pæne navn ("Gonal-F"), name = koden ("GONAL_F"). description må være NULL – så bruges koden,
            // så en påmindelse aldrig hedder "null · 150 IU"
            String name = med.getDescription() != null ? med.getDescription() : med.getName();
            names.put(med.getId(), name);
        }
        return names;
    }

    // Markér én dosis som taget (US8 AC3) – kun patientens egen dosis. Svar: OK · NOT_FOUND
    public ServiceResult markTaken(int patientId, int logId) {
        if (!isOwnDose(patientId, logId)) {
            return ServiceResult.NOT_FOUND;
        }
        medicationLogMapper.markTaken(logId);
        return ServiceResult.OK;
    }

    // Fortryd "markér som taget" (sæt dosen tilbage til planlagt) – kun patientens egen dosis. Svar: OK · NOT_FOUND
    public ServiceResult markNotTaken(int patientId, int logId) {
        if (!isOwnDose(patientId, logId)) {
            return ServiceResult.NOT_FOUND;
        }
        medicationLogMapper.markNotTaken(logId);
        return ServiceResult.OK;
    }

    // Sletter én dosis (fx en fejlindtastning) – kun patientens egen dosis. Svar: OK · NOT_FOUND
    public ServiceResult deleteDose(int patientId, int logId) {
        if (!isOwnDose(patientId, logId)) {
            return ServiceResult.NOT_FOUND;
        }
        medicationLogMapper.delete(logId);
        return ServiceResult.OK;
    }

    // hjælper: findes dosen i patientens egen runde? Så man ikke kan ændre andres ved at rette id'et i det skjulte felt
    private boolean isOwnDose(int patientId, int logId) {
        for (MedicationLog log : getAll(patientId)) {
            if (log.getId() == logId) {
                return true;
            }
        }
        return false;
    }

    // hjælper: alle doser i den runde, der er i gang. Intet forløb eller ingen runde er ikke en fejl her -> tom liste
    private List<MedicationLog> getAll(int patientId) {
        try {
           Round round =  roundService.findActiveRound(patientId);
            return medicationLogMapper.findByRound(round.getId());
        } catch (NoActiveJourneyException | NoActiveRoundException e) {
            return new ArrayList<>();
        }
    }

    // lille hjælper: null eller kun mellemrum tæller som tomt
    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }


}