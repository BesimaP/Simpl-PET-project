package service;

import dao.DatabaseConnection;
import dao.PatientDAO;
import dao.UserAccountDAO;
import entities.UserAccount;
import enums.ServiceResult;

// Forretningslogik for min-profil (US6b). Kender IKKE Javalin – ProfileController læser formularen og kalder én metode her.
// Alle tre metoder gemmer/ændrer noget, så de svarer med ServiceResult. Controlleren vælger side ud fra svaret.
public class ProfileService {

    // Retter patientens navn
    public ServiceResult updateName(int patientId, String name) {
        // 1. regel: navnet må ikke være tomt
        if (isBlank(name)) {
            return ServiceResult.INVALID_INPUT;
        }

        // 2. bed arkivaren rette navnet på patientens kort (UPDATE patient SET name = ? WHERE id = ?)
        new PatientDAO(DatabaseConnection.getConnection()).updateName(patientId, name);

        // 3. gik godt
        return ServiceResult.OK;
    }

    // Skifter kodeord. Alle tjek giver INVALID_INPUT, så man ikke afslører hvad der var galt
    public ServiceResult changePassword(int accountId, String currentPassword, String newPassword, String repeat) {
        // 1. regel: ingen tomme felter
        if (isBlank(currentPassword) || isBlank(newPassword) || isBlank(repeat)) {
            return ServiceResult.INVALID_INPUT;
        }

        // 2. regel: det nye kodeord skal være skrevet ens to gange (ellers gemmer vi en stavefejl)
        if (!newPassword.equals(repeat)) {
            return ServiceResult.INVALID_INPUT;
        }

        // 3. hent kontoens kort fra databasen – null = findes ikke
        UserAccount account = new UserAccountDAO(DatabaseConnection.getConnection()).findById(accountId);
        if (account == null) {
            return ServiceResult.INVALID_INPUT;
        }

        // 4. regel: det gamle kodeord skal passe med det, der ligger i databasen (senere: BCrypt.checkpw)
        if (!currentPassword.equals(account.getPasswordHash())) {
            return ServiceResult.INVALID_INPUT;
        }

        // 5. bed arkivaren gemme det nye kodeord (senere: hash det først)
        new UserAccountDAO(DatabaseConnection.getConnection()).updatePassword(accountId, newPassword);

        // 6. gik godt
        return ServiceResult.OK;
    }

    // Sletter kontoen – patient og alt under den ryger med (ON DELETE CASCADE i schema.sql)
    public ServiceResult deleteAccount(int accountId) {
        // 1. bed arkivaren slette kortet (DELETE FROM user_account WHERE id = ?)
        new UserAccountDAO(DatabaseConnection.getConnection()).delete(accountId);

        // 2. gik godt
        return ServiceResult.OK;
    }

    // hjælper: null eller kun mellemrum tæller som tomt (samme som i de andre services)
    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
