package service;

import dao.DatabaseConnection;
import dao.DiagnosisDAO;
import entities.Diagnosis;
import enums.ServiceResult;

// Forretningslogik for diagnoser (US7). Kender IKKE Javalin. Diagnoser hænger på patienten – ingen forløb/runde.
public class DiagnosisService {

    public ServiceResult addDiagnosis(int patientId, String name, String description) {
        // 1. byg kortet af felterne – diagnosen hænger direkte på patienten (patient_id), ingen forløb/runde
        Diagnosis diagnosis = new Diagnosis(0, patientId, name, description);

        // 2. læg kortet i skuffen diagnosis
        new DiagnosisDAO(DatabaseConnection.getConnection()).save(diagnosis);

        return ServiceResult.OK;
    }
}
