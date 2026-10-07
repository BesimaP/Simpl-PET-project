package services;
import entities.Diagnosis;
import enums.ServiceResult;
import persistence.ConnectionPool;
import persistence.DiagnosisMapper;

import java.util.List;

// Forretningslogik for diagnoser (US7). Kender IKKE Javalin. Diagnoser hænger på patienten – ingen forløb/runde.
public class DiagnosisService {

    private final DiagnosisMapper diagnosisMapper;

    public DiagnosisService(ConnectionPool connectionPool){
        this.diagnosisMapper = new DiagnosisMapper(connectionPool);
    }

    public ServiceResult addDiagnosis(int patientId, String name, String description) {
        // 1. regel: en diagnose skal have et navn (serveren stoler ikke på required i HTML)
        if (isBlank(name)) {
            return ServiceResult.INVALID_INPUT;
        }
        // navnet skal kunne være i kolonnen (VARCHAR(100))
        if (isTooLong(name, 100)) {
            return ServiceResult.INVALID_INPUT;
        }

        // 2. beskrivelsen er valgfri – tom tekst gemmes som null, så databasen ikke fyldes med ""
        String desc = isBlank(description) ? null : description;

        // 3. byg kortet og gem – diagnosen hænger direkte på patienten (patient_id)
        Diagnosis diagnosis = new Diagnosis(0, patientId, name, desc);
        diagnosisMapper.save(diagnosis);

        return ServiceResult.OK;
    }

    // Henter alle patientens diagnoser – til listen på diagnoser-siden (GET)
    public List<Diagnosis> getDiagnoses(int patientId) {
        return diagnosisMapper.findByPatient(patientId);
    }

    // Sletter én diagnose – men kun hvis den er patientens egen: mapperen sletter med "WHERE id = ? AND patient_id = ?".
    // Svar: OK · NOT_FOUND = findes ikke / ikke hendes. Samme mønster som DiaryService.deleteEntry
    public ServiceResult deleteDiagnosis(int patientId, int diagnosisId) {
        return diagnosisMapper.delete(diagnosisId, patientId) ? ServiceResult.OK : ServiceResult.NOT_FOUND;
    }

    // lille hjælper: null eller kun mellemrum tæller som tomt
    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    // lille hjælper: længere end kolonnen i databasen (VARCHAR(max))? Så ville INSERT fejle med en 500-fejl
    private boolean isTooLong(String s, int max) {
        return s != null && s.length() > max;
    }
}
