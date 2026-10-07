package services;

import dao.PatientDAO;
import entities.Patient;
import enums.ServiceResult;
import exceptions.UserNotFoundException;
import org.mindrot.jbcrypt.BCrypt;
import persistence.ConnectionPool;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

// Forretningslogik for login og opret profil. Kender IKKE Javalin – controlleren læser formularen og kalder én metode her.
public class AuthService {
    private PatientDAO patientDao;          // lomme 1: arkivaren (laves én gang i konstruktøren)
    private ConnectionPool connectionPool;  // lomme 2: nøgleringen (gives videre til DashboardService)

    // Konstruktøren: den, der laver en AuthService, SKAL give nøgleringen med.
    // Kaldes fra LoginController: new AuthService(connectionPool)
    public AuthService(ConnectionPool connectionPool) {
        this.patientDao = new PatientDAO(connectionPool); // lav arkivaren, giv ham ringen, læg ham i lommen
        this.connectionPool = connectionPool;             // læg også selve ringen i lommen
    }

    // Login: giver patientens id tilbage (det er det, sessionen skal huske) – ellers kastes UserNotFoundException
    public Patient login(String username, String password) throws UserNotFoundException {
        Patient patient = patientDao.findByUsername(username);

        if (patient == null || !BCrypt.checkpw(password, patient.getPasswordHash())) {
            throw new UserNotFoundException("Forkert brugernavn eller kodeord");
        }

        // brugernavn og kodeord passer – giv patientens kort tilbage (id'et skal i sessionen)
        return patient;
    }

    // Opret profil: patient (+ forløb, hvis brugeren allerede er i gang med et).
    // OK = oprettet · INVALID_INPUT = tomt felt/ugyldig dato · ALREADY_EXISTS = brugernavnet er optaget
    public ServiceResult createProfile(String firstName, String lastName, String dateOfBirth, String username, String password, String hasJourney, String journeyStart) {

        // 0. regel: ingen tomme felter
        if (isBlank(firstName) || isBlank(lastName) || isBlank(dateOfBirth) || isBlank(username) || isBlank(password)) {
            return ServiceResult.INVALID_INPUT;
        }

        // 0b. regel: fødselsdatoen skal være en rigtig dato (ellers crasher LocalDate.parse længere nede)
        LocalDate dob;
        try {
            dob = LocalDate.parse(dateOfBirth);
        } catch (DateTimeParseException e) {
            return ServiceResult.INVALID_INPUT;
        }

        // 0c. regel: har brugeren svaret "ja" til forløb, skal startdatoen være en rigtig dato
        //     (tjekkes HER, før vi gemmer noget – ellers ville kontoen være oprettet, men forløbet fejle)
        boolean wantsJourney = "yes".equals(hasJourney);
        if (wantsJourney) {
            if (isBlank(journeyStart)) {
                return ServiceResult.INVALID_INPUT;
            }
            try {
                LocalDate.parse(journeyStart);
            } catch (DateTimeParseException e) {
                return ServiceResult.INVALID_INPUT;
            }
        }

        // 1. regel: brugernavn skal være unikt
        if (patientDao.findByUsername(username) != null) {
            return ServiceResult.ALREADY_EXISTS; // brugernavnet er optaget
        }

        // 2. gem patienten (login + persondata i én række). id'et fra databasen skal bruges til forløbet
        int patientId = patientDao.save(new Patient(0, username, BCrypt.hashpw(password, BCrypt.gensalt()), firstName.trim(), lastName.trim(), dob));

        // 3. valgfrit: opret forløbet med det samme – samme regel/metode som "Start dit forløb" på dashboardtom.html
        if (wantsJourney) {
            new DashboardService(connectionPool).createJourney(patientId, journeyStart); // ringen gives videre
        }

        return ServiceResult.OK;
    }

    // hjælper: null eller kun mellemrum tæller som tomt (samme som i de andre services)
    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}