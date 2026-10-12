package services;

import persistence.PatientMapper;
import entities.Patient;
import enums.ServiceResult;
import org.mindrot.jbcrypt.BCrypt;
import persistence.ConnectionPool;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

// Logik for min-profil (US6b): ret oplysninger, skift kodeord og slet konto.
public class ProfileService {
    private PatientMapper patientMapper;
    private DocumentService documentService;   // til at slette filerne på disken, når kontoen slettes

    public ProfileService(ConnectionPool connectionPool) {
        this.patientMapper = new PatientMapper(connectionPool);
        this.documentService = new DocumentService(connectionPool);
    }

    // til at udfylde felterne på min-profil
    public Patient getPatient(int patientId) {
        return patientMapper.findById(patientId);
    }

    // Retter patientens navn og fødselsdato (US6b AC1)
    public ServiceResult updateProfile(int patientId, String firstName, String lastName, String dateOfBirth) {
        // ingen tomme felter
        if (InputCheck.isBlank(firstName) || InputCheck.isBlank(lastName) || InputCheck.isBlank(dateOfBirth)) {
            return ServiceResult.INVALID_INPUT;
        }
        // navnene skal kunne være i kolonnerne (VARCHAR(50))
        if (InputCheck.isTooLong(firstName, 50) || InputCheck.isTooLong(lastName, 50)) {
            return ServiceResult.INVALID_INPUT;
        }

        // fødselsdatoen skal være en rigtig dato og ikke i fremtiden
        LocalDate dob;
        try {
            dob = LocalDate.parse(dateOfBirth);
        } catch (DateTimeParseException e) {
            return ServiceResult.INVALID_INPUT;
        }
        if (dob.isAfter(LocalDate.now())) {
            return ServiceResult.INVALID_INPUT;
        }

        patientMapper.updateProfile(patientId, firstName.trim(), lastName.trim(), dob);

        return ServiceResult.OK;
    }

    // Skifter kodeord. Alle fejl giver INVALID_INPUT, så vi ikke afslører, hvad der var galt
    public ServiceResult changePassword(int patientId, String currentPassword, String newPassword, String repeat) {
        // ingen tomme felter
        if (InputCheck.isBlank(currentPassword) || InputCheck.isBlank(newPassword) || InputCheck.isBlank(repeat)) {
            return ServiceResult.INVALID_INPUT;
        }

        // det nye kodeord skal være skrevet ens to gange, så vi ikke gemmer en stavefejl
        if (!newPassword.equals(repeat)) {
            return ServiceResult.INVALID_INPUT;
        }

        // samme længdekrav som ved opret profil
        if (newPassword.length() < AuthService.MIN_PASSWORD_LENGTH || newPassword.length() > AuthService.MAX_PASSWORD_LENGTH) {
            return ServiceResult.INVALID_INPUT;
        }

        Patient patient = patientMapper.findById(patientId);
        if (patient == null) {
            return ServiceResult.INVALID_INPUT;
        }

        // det nuværende kodeord skal passe – samme tjek som ved login
        if (!AuthService.passwordMatches(currentPassword, patient.getPasswordHash())) {
            return ServiceResult.INVALID_INPUT;
        }

        patientMapper.updatePassword(patientId, BCrypt.hashpw(newPassword, BCrypt.gensalt()));

        return ServiceResult.OK;
    }

    // Sletter patienten. Alt under hende slettes med i databasen (ON DELETE CASCADE)
    public ServiceResult deleteAccount(int patientId) {
        // CASCADE sletter kun rækkerne, ikke filerne på disken – derfor henter vi listen først (US6b AC2)
        java.util.List<entities.Document> documents = documentService.getDocuments(patientId);

        patientMapper.delete(patientId);

        // filerne slettes først nu, så de stadig findes, hvis sletningen i databasen fejler
        documentService.deleteFiles(documents);

        return ServiceResult.OK;
    }
}
