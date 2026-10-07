package services;

import persistence.PatientMapper;
import entities.Patient;
import enums.ServiceResult;
import org.mindrot.jbcrypt.BCrypt;
import persistence.ConnectionPool;

// Forretningslogik for min-profil (US6b). Kender IKKE Javalin – ProfileController læser formularen og kalder én metode her.
// Alle tre metoder gemmer/ændrer noget, så de svarer med ServiceResult. Controlleren vælger side ud fra svaret.
public class ProfileService {
    private PatientMapper patientMapper;

    public ProfileService(ConnectionPool connectionPool) {
        this.patientMapper = new PatientMapper(connectionPool);
    }

    // Henter patientens kort – til at forudfylde felterne på min-profil (GET)
    public Patient getPatient(int patientId) {
        return patientMapper.findById(patientId);
    }

    // Retter patientens navn
    public ServiceResult updateName(int patientId, String firstName, String lastName) {
        // 1. regel: hverken for- eller efternavn må være tomt
        if (isBlank(firstName) || isBlank(lastName)) {
            return ServiceResult.INVALID_INPUT;
        }

        // 2. bed arkivaren rette navnet på patientens kort (UPDATE patient SET first_name = ?, last_name = ? WHERE id = ?)
        patientMapper.updateName(patientId, firstName.trim(), lastName.trim());

        // 3. gik godt
        return ServiceResult.OK;
    }

    // Skifter kodeord. Alle tjek giver INVALID_INPUT, så man ikke afslører hvad der var galt
    public ServiceResult changePassword(int patientId, String currentPassword, String newPassword, String repeat) {
        // 1. regel: ingen tomme felter
        if (isBlank(currentPassword) || isBlank(newPassword) || isBlank(repeat)) {
            return ServiceResult.INVALID_INPUT;
        }

        // 2. regel: det nye kodeord skal være skrevet ens to gange (ellers gemmer vi en stavefejl)
        if (!newPassword.equals(repeat)) {
            return ServiceResult.INVALID_INPUT;
        }

        // 3. hent patientens kort fra databasen – null = findes ikke
        Patient patient = patientMapper.findById(patientId);
        if (patient == null) {
            return ServiceResult.INVALID_INPUT;
        }

        // 4. regel: det gamle kodeord skal passe med det, der ligger i databasen
        if (!BCrypt.checkpw(currentPassword, patient.getPasswordHash())) {
            return ServiceResult.INVALID_INPUT;
        }

        // 5. bed arkivaren gemme det nye kodeord
        patientMapper.updatePassword(patientId, BCrypt.hashpw(newPassword, BCrypt.gensalt()));

        // 6. gik godt
        return ServiceResult.OK;
    }

    // Sletter kontoen = patienten – alt under den ryger med (ON DELETE CASCADE i schema_postgres.sql)
    public ServiceResult deleteAccount(int patientId) {
        // 1. bed arkivaren slette kortet (DELETE FROM patient WHERE id = ?)
        patientMapper.delete(patientId);

        // 2. gik godt
        return ServiceResult.OK;
    }

    // hjælper: null eller kun mellemrum tæller som tomt (samme som i de andre services)
    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
