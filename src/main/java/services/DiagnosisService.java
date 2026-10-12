package services;
import entities.Diagnosis;
import enums.ServiceResult;
import persistence.ConnectionPool;
import persistence.DiagnosisMapper;

import java.util.List;

// Logik for diagnoser (US7). Diagnoser hænger direkte på patienten, ikke på et forløb.
public class DiagnosisService {

    private final DiagnosisMapper diagnosisMapper;

    public DiagnosisService(ConnectionPool connectionPool){
        this.diagnosisMapper = new DiagnosisMapper(connectionPool);
    }

    public ServiceResult addDiagnosis(int patientId, String name, String description) {
        // en diagnose skal have et navn
        if (InputCheck.isBlank(name)) {
            return ServiceResult.INVALID_INPUT;
        }
        // navnet skal kunne være i kolonnen (VARCHAR(100))
        if (InputCheck.isTooLong(name, 100)) {
            return ServiceResult.INVALID_INPUT;
        }

        // beskrivelsen er valgfri – tom tekst gemmes som null
        String desc = InputCheck.isBlank(description) ? null : description.trim();

        Diagnosis diagnosis = new Diagnosis(0, patientId, name.trim(), desc);
        diagnosisMapper.save(diagnosis);

        return ServiceResult.OK;
    }

    public List<Diagnosis> getDiagnoses(int patientId) {
        return diagnosisMapper.findByPatient(patientId);
    }

    // Sletter kun, hvis diagnosen er patientens egen (WHERE id = ? AND patient_id = ?). Ellers NOT_FOUND
    public ServiceResult deleteDiagnosis(int patientId, int diagnosisId) {
        return diagnosisMapper.delete(diagnosisId, patientId) ? ServiceResult.OK : ServiceResult.NOT_FOUND;
    }
}
