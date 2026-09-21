package service;

import enums.ServiceResult;

    // Forretningslogik for min-profil (US6b). Kender IKKE Javalin.
    public class ProfileService {

        // Retter patientens navn
        public ServiceResult updateName(int patientId, String name) {
            // 1. isBlank(name) -> INVALID_INPUT
            // 2. new PatientDAO(...).updateName(patientId, name)
            // 3. return OK
        }

        // Skifter kodeord. Alle tre tjek giver INVALID_INPUT, så man ikke afslører hvad der var galt
        public ServiceResult changePassword(int accountId, String current, String newPassword, String repeat) {
            // 1. isBlank på alle tre -> INVALID_INPUT
            // 2. !newPassword.equals(repeat) -> INVALID_INPUT
            // 3. UserAccount account = new UserAccountDAO(...).findById(accountId); account == null -> INVALID_INPUT
            // 4. !current.equals(account.getPasswordHash()) -> INVALID_INPUT   (senere: BCrypt.checkpw)
            // 5. new UserAccountDAO(...).updatePassword(accountId, newPassword)   (senere: hash først)
            // 6. return OK
        }

        // Sletter kontoen – patient og alt under den ryger med (CASCADE i schema.sql)
        public ServiceResult deleteAccount(int accountId) {
            // 1. new UserAccountDAO(...).delete(accountId)
            // 2. return OK
        }

        // isBlank-hjælper som i de andre services
    }