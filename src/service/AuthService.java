package service;

import dao.DatabaseConnection;
import dao.PatientDAO;
import dao.UserAccountDAO;
import entities.Patient;
import entities.UserAccount;
import enums.ServiceResult;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

// Forretningslogik for login og opret profil. Kender IKKE Javalin – controlleren læser formularen og kalder én metode her.
public class AuthService {

    // Login: giver kortet (UserAccount) tilbage, hvis brugernavn og kodeord passer – ellers null
    public UserAccount login(String username, String password) {
        UserAccountDAO dao = new UserAccountDAO(DatabaseConnection.getConnection());
        UserAccount user = dao.findByUsername(username);

        if (user == null || !password.equals(user.getPasswordHash())) {
            return null;   // ukendt bruger eller forkert kodeord (senere: BCrypt.checkpw i stedet for equals)
        }
        return user;       // login ok
    }

    // Opret profil: konto + patient. OK = oprettet · INVALID_INPUT = tomt felt/ugyldig dato · ALREADY_EXISTS = brugernavnet er optaget
    public ServiceResult createProfile(String name, String dateOfBirth, String username, String password) {

        // 0. regel: ingen tomme felter
        if (isBlank(name) || isBlank(dateOfBirth) || isBlank(username) || isBlank(password)) {
            return ServiceResult.INVALID_INPUT;
        }

        // 0b. regel: fødselsdatoen skal være en rigtig dato (ellers crasher LocalDate.parse længere nede)
        LocalDate dob;
        try {
            dob = LocalDate.parse(dateOfBirth);
        } catch (DateTimeParseException e) {
            return ServiceResult.INVALID_INPUT;
        }

        UserAccountDAO accountDao = new UserAccountDAO(DatabaseConnection.getConnection());


        // 1. regel: brugernavn skal være unikt
        if (accountDao.findByUsername(username) != null) {
            return ServiceResult.ALREADY_EXISTS; // brugernavnet er optaget
        }

        // 2. gem kontoen – id'et fra databasen skal bruges til patienten lige efter
        //    (TODO senere: hash kodeordet med BCrypt før det gemmes)
        int accountId = accountDao.save(new UserAccount(0, username, password));

        // 3. gem patienten, knyttet til kontoen via accountId
        PatientDAO patientDao = new PatientDAO(DatabaseConnection.getConnection());
        patientDao.save(new Patient(0, accountId, name, dob));

        return ServiceResult.OK;
    }

    // hjælper: null eller kun mellemrum tæller som tomt (samme som i de andre services)
    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}