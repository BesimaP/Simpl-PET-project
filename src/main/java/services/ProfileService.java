package services;

import persistence.PatientMapper;
import entities.Patient;
import enums.ServiceResult;
import org.mindrot.jbcrypt.BCrypt;
import persistence.ConnectionPool;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

// Forretningslogik for min-profil (US6b). Kender IKKE Javalin – ProfileController læser formularen og kalder én metode her.
// Alle tre metoder gemmer/ændrer noget, så de svarer med ServiceResult. Controlleren vælger side ud fra svaret.
public class ProfileService {
    private PatientMapper patientMapper;
    private DocumentService documentService;   // til at slette patientens filer på disken, når kontoen slettes

    public ProfileService(ConnectionPool connectionPool) {
        this.patientMapper = new PatientMapper(connectionPool);
        this.documentService = new DocumentService(connectionPool);
    }

    // Henter patientens kort – til at forudfylde felterne på min-profil (GET)
    public Patient getPatient(int patientId) {
        return patientMapper.findById(patientId);
    }

    // Retter patientens navn og fødselsdato (US6b AC1)
    public ServiceResult updateProfile(int patientId, String firstName, String lastName, String dateOfBirth) {
        // 1. regel: ingen tomme felter
        if (isBlank(firstName) || isBlank(lastName) || isBlank(dateOfBirth)) {
            return ServiceResult.INVALID_INPUT;
        }
        // navnene skal kunne være i kolonnerne (VARCHAR(50))
        if (isTooLong(firstName, 50) || isTooLong(lastName, 50)) {
            return ServiceResult.INVALID_INPUT;
        }

        // 2. fødselsdatoen skal være en rigtig dato – og ikke i fremtiden
        LocalDate dob;
        try {
            dob = LocalDate.parse(dateOfBirth);
        } catch (DateTimeParseException e) {
            return ServiceResult.INVALID_INPUT;
        }
        if (dob.isAfter(LocalDate.now())) {
            return ServiceResult.INVALID_INPUT;
        }

        // 3. bed arkivaren rette kortet (UPDATE patient SET first_name = ?, last_name = ?, date_of_birth = ? WHERE id = ?)
        patientMapper.updateProfile(patientId, firstName.trim(), lastName.trim(), dob);

        // 4. gik godt
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

        // 2b. regel: mindst 8 tegn (samme regel som ved opret profil)
        if (newPassword.length() < AuthService.MIN_PASSWORD_LENGTH) {
            return ServiceResult.INVALID_INPUT;
        }

        // 3. hent patientens kort fra databasen – null = findes ikke
        Patient patient = patientMapper.findById(patientId);
        if (patient == null) {
            return ServiceResult.INVALID_INPUT;
        }

        // 4. regel: det gamle kodeord skal passe med det, der ligger i databasen
        //    samme tjek som ved login (AuthService.passwordMatches) – et ødelagt hash i databasen tæller som "passer ikke"
        if (!AuthService.passwordMatches(currentPassword, patient.getPasswordHash())) {
            return ServiceResult.INVALID_INPUT;
        }

        // 5. bed arkivaren gemme det nye kodeord
        patientMapper.updatePassword(patientId, BCrypt.hashpw(newPassword, BCrypt.gensalt()));

        // 6. gik godt
        return ServiceResult.OK;
    }

    // Sletter kontoen = patienten – alt under den ryger med (ON DELETE CASCADE i schema_postgres.sql)
    public ServiceResult deleteAccount(int patientId) {
        // 1. slet patientens uploadede filer på disken. CASCADE sletter kun RÆKKERNE i databasen – ikke selve filerne (US6b AC2)
        documentService.deleteAllFiles(patientId);

        // 2. bed arkivaren slette kortet (DELETE FROM patient WHERE id = ?)
        patientMapper.delete(patientId);

        // 3. gik godt
        return ServiceResult.OK;
    }

    // hjælper: null eller kun mellemrum tæller som tomt (samme som i de andre services)
    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    // lille hjælper: længere end kolonnen i databasen (VARCHAR(max))? Så ville INSERT fejle med en 500-fejl
    private boolean isTooLong(String s, int max) {
        return s != null && s.length() > max;
    }
}
