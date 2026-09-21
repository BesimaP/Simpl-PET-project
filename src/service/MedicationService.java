package service;

// Forretningslogik for medicin (US8). Kender IKKE Javalin – controlleren læser formularen og kalder én metode her.
public class MedicationService {

    public enum LogResult{SAVED, INVALID_INPUT, NO_ACTIVE_ROUND}

    // Log en dosis i patientens aktive runde
    public LogResult logDose(int patientId, String medicationName, String dose, String unit, String date, String time, boolean taken){

        // 1. regel: alle felter skal være udfyldt
        if(medicationName.isBlank() || dose.isBlank() || unit.isBlank() || date.isBlank()|| time.isBlank()){
        return LogResult.INVALID_INPUT;
        }

        double doseValue



    }
}
