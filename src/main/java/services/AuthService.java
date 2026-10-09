package services;

import persistence.PatientMapper;
import entities.Patient;
import enums.ServiceResult;
import exceptions.DatabaseException;
import exceptions.UserNotFoundException;
import org.mindrot.jbcrypt.BCrypt;
import persistence.ConnectionPool;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

// Forretningslogik for login og opret profil. Kender IKKE Javalin – controlleren læser formularen og kalder én metode her.
public class AuthService {
    public static final int MIN_PASSWORD_LENGTH = 8;  // bruges også af ProfileService ved "skift kodeord"
    public static final int MAX_PASSWORD_LENGTH = 72; // BCrypt bruger kun de første 72 bytes – resten ville blive ignoreret uden at sige det

    private PatientMapper patientMapper;          // attribut 1: arkivaren (laves én gang i konstruktøren)
    private DashboardService dashboardService;    // attribut 2: bruges til at oprette forløbet ved "opret profil"

    // Konstruktøren: den, der laver en AuthService, SKAL give nøgleringen med.
    // Kaldes fra LoginController: new AuthService(connectionPool)
    public AuthService(ConnectionPool connectionPool) {
        this.patientMapper = new PatientMapper(connectionPool);       // lav arkivaren, giv ham ringen, gem ham i attributten
        this.dashboardService = new DashboardService(connectionPool); // ringen gives videre til DashboardService
    }

    // Login: giver patientens id tilbage (det er det, sessionen skal huske) – ellers kastes UserNotFoundException
    public Patient login(String username, String password) throws UserNotFoundException {
        // regel: begge felter skal være udfyldt (BCrypt kan ikke tjekke et kodeord, der er null)
        if (isBlank(username) || isBlank(password)) {
            throw new UserNotFoundException("Forkert brugernavn eller adgangskode");
        }

        // mellemrum før/efter fjernes – samme som ved opret profil, så " anna" og "anna" er samme bruger
        Patient patient = patientMapper.findByUsername(username.trim());

        if (patient == null || !passwordMatches(password, patient.getPasswordHash())) {
            throw new UserNotFoundException("Forkert brugernavn eller adgangskode");
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

        // mellemrum før/efter fjernes (ligesom fornavn og efternavn), så man kan logge ind uden at ramme dem præcist
        username = username.trim();

        // 0a. regel: teksten skal kunne være i kolonnerne (VARCHAR(50) – se schema_postgres.sql)
        if (isTooLong(firstName, 50) || isTooLong(lastName, 50) || isTooLong(username, 50)) {
            return ServiceResult.INVALID_INPUT;
        }

        // 0b. regel: fødselsdatoen skal være en rigtig dato (ellers crasher LocalDate.parse længere nede)
        LocalDate dob;
        try {
            dob = LocalDate.parse(dateOfBirth);
        } catch (DateTimeParseException e) {
            return ServiceResult.INVALID_INPUT;
        }
        // man kan ikke være født i fremtiden
        if (dob.isAfter(LocalDate.now())) {
            return ServiceResult.INVALID_INPUT;
        }

        // regel: kodeordet skal være mindst 8 tegn (samme som minlength="8" i HTML – serveren stoler ikke på HTML)
        if (password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            return ServiceResult.INVALID_INPUT;
        }

        // 0c. regel: har brugeren svaret "ja" til forløb, skal startdatoen være en rigtig dato
        //     (tjekkes HER, før vi gemmer noget – ellers ville kontoen være oprettet, men forløbet fejle)
        boolean wantsJourney = "yes".equals(hasJourney);
        if (wantsJourney) {
            if (isBlank(journeyStart)) {
                return ServiceResult.INVALID_INPUT;
            }
            LocalDate start;
            try {
                start = LocalDate.parse(journeyStart);
            } catch (DateTimeParseException e) {
                return ServiceResult.INVALID_INPUT;
            }
            // forløbet kan ikke starte i fremtiden (samme regel som i DashboardService)
            if (start.isAfter(LocalDate.now())) {
                return ServiceResult.INVALID_INPUT;
            }
        }

        // 1. regel: brugernavn skal være unikt
        if (patientMapper.findByUsername(username) != null) {
            return ServiceResult.ALREADY_EXISTS; // brugernavnet er optaget
        }

        // 2. gem patienten (login + persondata i én række). id'et fra databasen skal bruges til forløbet
        //    try/catch: ved dobbeltklik kan to forespørgsler komme forbi tjekket i trin 1 – databasens UNIQUE afviser den anden
        int patientId;
        try {
            patientId = patientMapper.save(new Patient(0, username, BCrypt.hashpw(password, BCrypt.gensalt()), firstName.trim(), lastName.trim(), dob));
        } catch (DatabaseException e) {
            if (e.isDuplicate()) {
                return ServiceResult.ALREADY_EXISTS;
            }
            throw e;   // en anden databasefejl – lad den samlede handler vise fejlsiden
        }

        // 3. valgfrit: opret forløbet med det samme – samme regel/metode som "Start dit forløb" på dashboardet
        if (wantsJourney) {
            dashboardService.createJourney(patientId, journeyStart);
        }

        return ServiceResult.OK;
    }

    // hjælper: passer kodeordet med hashet fra databasen?
    // BCrypt.checkpw KASTER en IllegalArgumentException eller StringIndexOutOfBoundsException, hvis hashet i databasen
    // ikke er et rigtigt BCrypt-hash (fx en pladsholder i testdata). Det skal give "forkert kodeord" – ikke en 500-fejl
    public static boolean passwordMatches(String password, String hash) {
        try {
            return BCrypt.checkpw(password, hash);
        } catch (IllegalArgumentException | StringIndexOutOfBoundsException e) {
            return false;
        }
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
