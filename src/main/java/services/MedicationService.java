package services;

import dao.*;
import entities.Medication;
import entities.MedicationLog;
import entities.Round;
import enums.ServiceResult;
import exceptions.NoActiveJourneyException;
import exceptions.NoActiveRoundException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

// Forretningslogik for medicin (US8). Kender IKKE Javalin – controlleren læser formularen og kalder én metode her.
// Svarer med ServiceResult: OK · INVALID_INPUT = tomt felt/ugyldigt tal, dato eller ukendt medicin · NO_ACTIVE_JOURNEY · NO_ACTIVE_ROUND
public class MedicationService {

    // Logger én dosis i patientens aktive runde (en dosis SKAL ligge på en runde – round_id i databasen)
    public ServiceResult logDose(int patientId, String medicationName, String dose, String unit, String date, String time, boolean taken) {

        // 1. regel: alle felter skal være udfyldt
        if (isBlank(medicationName) || isBlank(dose) || isBlank(unit) || isBlank(date) || isBlank(time)) {
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

        // 3. regel: dosis skal være større end 0
        if (doseValue <= 0) {
            return ServiceResult.INVALID_INPUT;
        }

        // 4. find lægemidlet ud fra navnet (dropdownens value)
        Medication medication = new MedicationDAO(DatabaseConnection.getConnection()).findByName(medicationName);
        if (medication == null) {
            return ServiceResult.INVALID_INPUT;
        }

        // 5. find den aktive runde – en dosis hører til en runde.
        //    findActiveRound kaster, hvis der ikke er forløb eller runde – vi fanger og oversætter til et ServiceResult
        Round round;
        try {
            round = new RoundService().findActiveRound(patientId);
        } catch (NoActiveJourneyException e) {
            return ServiceResult.NO_ACTIVE_JOURNEY;
        } catch (NoActiveRoundException e) {
            return ServiceResult.NO_ACTIVE_ROUND;
        }

        // 6. gem – taken = true betyder "allerede taget", false betyder "planlagt"
        new MedicationLogDAO(DatabaseConnection.getConnection()).
                save(new MedicationLog(0, round.getId(), medication.getId(), scheduled, doseValue, unit, taken));

        return ServiceResult.OK;
    }

    // lille hjælper: null eller kun mellemrum tæller som tomt
    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
