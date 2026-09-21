package service;

import dao.*;
import entities.FertilityJourney;
import entities.Medication;
import entities.MedicationLog;
import entities.Round;
import enums.ServiceResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

// Forretningslogik for medicin (US8). Kender IKKE Javalin – controlleren læser formularen og kalder én metode her.
public class MedicationService {

    // Log en dosis i patientens aktive runde
    public ServiceResult logDose(int patientId, String medicationName, String dose, String unit, String date, String time, boolean taken){

        // 1. regel: alle felter skal være udfyldt
        if (isBlank(medicationName) || isBlank(dose) || isBlank(unit) || isBlank(date) || isBlank(time)) {
            return ServiceResult.INVALID_INPUT;
        }

        // 2. tekst fra formularen -> rigtige typer. Ugyldigt input giver INVALID_INPUT i stedet for et crash
        double doseValue;
        LocalDateTime scheduled;
        try{
            doseValue = Double.parseDouble(dose);
            scheduled = LocalDateTime.of(LocalDate.parse(date), LocalTime.parse(time));
        }catch(NumberFormatException | DateTimeParseException e){
            return ServiceResult.INVALID_INPUT;
        }

        // 3. regel: dosis skal være større end 0
        if(doseValue<=0){
            return ServiceResult.INVALID_INPUT;
        }

        // 4. find lægemidlet ud fra navnet (dropdownens value)
        Medication medication = new MedicationDAO(DatabaseConnection.getConnection()).findByName(medicationName);
        if(medication == null){
            return ServiceResult.INVALID_INPUT;
        }

        // 5. find den aktive runde – en dosis hører til en runde
        FertilityJourney journey = new FertilityJourneyDAO(DatabaseConnection.getConnection()).findActiveByPatient(patientId);
        if(journey == null){
            return ServiceResult.NO_ACTIVE_JOURNEY;
        }
        Round round = new RoundDAO(DatabaseConnection.getConnection()).findActiveByJourney(journey.getId());
        if(round == null){
            return ServiceResult.NO_ACTIVE_ROUND;
        }

        // 6. gem – taken = true betyder "allerede taget", false betyder "planlagt"
        new MedicationLogDAO(DatabaseConnection.getConnection()).
                save(new MedicationLog(0, round.getId(), medication.getId(), scheduled, doseValue, unit, taken));

        return ServiceResult.OK;
    }
    private boolean isBlank(String s){
        return s == null || s.isBlank();
    }
}
