package services;

import dao.DatabaseConnection;
import dao.DiagnosisDAO;
import entities.Diagnosis;
import enums.ServiceResult;

import java.util.List;

// Forretningslogik for diagnoser (US7). Kender IKKE Javalin. Diagnoser hænger på patienten – ingen forløb/runde.
public class DiagnosisService {

    public ServiceResult addDiagnosis(int patientId, String name, String description) {
        // 1. regel: en diagnose skal have et navn (serveren stoler ikke på required i HTML)
        if (isBlank(name)) {
            return ServiceResult.INVALID_INPUT;
        }

        // 2. beskrivelsen er valgfri – tom tekst gemmes som null, så databasen ikke fyldes med ""
        String desc = isBlank(description) ? null : description;

        // 3. byg kortet og gem – diagnosen hænger direkte på patienten (patient_id)
        Diagnosis diagnosis = new Diagnosis(0, patientId, name, desc);
        new DiagnosisDAO(DatabaseConnection.getConnection()).save(diagnosis);

        return ServiceResult.OK;
    }

    // Henter alle patientens diagnoser – til listen på diagnoser-siden (GET)
    public List<Diagnosis> getDiagnoses(int patientId) {
        return new DiagnosisDAO(DatabaseConnection.getConnection()).findByPatient(patientId);
    }

    // lille hjælper: null eller kun mellemrum tæller som tomt
    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
