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

// Logik for login og opret profil.
public class AuthService {
    public static final int MIN_PASSWORD_LENGTH = 8;  // bruges også af ProfileService ved skift kodeord
    public static final int MAX_PASSWORD_LENGTH = 72; // BCrypt bruger kun de første 72 bytes – resten ville blive ignoreret

    private PatientMapper patientMapper;
    private DashboardService dashboardService;    // til at oprette forløbet ved opret profil

    public AuthService(ConnectionPool connectionPool) {
        this.patientMapper = new PatientMapper(connectionPool);
        this.dashboardService = new DashboardService(connectionPool);
    }

    // Giver patienten tilbage, hvis brugernavn og kodeord passer – ellers UserNotFoundException
    public Patient login(String username, String password) throws UserNotFoundException {
        // begge felter skal være udfyldt (BCrypt kan ikke tjekke null)
        if (InputCheck.isBlank(username) || InputCheck.isBlank(password)) {
            throw new UserNotFoundException("Forkert brugernavn eller adgangskode");
        }

        // trim ligesom ved opret profil, så " anna" og "anna" er samme bruger
        Patient patient = patientMapper.findByUsername(username.trim());

        if (patient == null || !passwordMatches(password, patient.getPasswordHash())) {
            throw new UserNotFoundException("Forkert brugernavn eller adgangskode");
        }

        return patient;
    }

    // Opretter patienten og evt. et forløb, hvis hun allerede er i gang med et.
    // Svar: OK, INVALID_INPUT eller ALREADY_EXISTS (brugernavnet er optaget)
    public ServiceResult createProfile(String firstName, String lastName, String dateOfBirth, String username, String password, String hasJourney, String journeyStart) {

        // ingen tomme felter
        if (InputCheck.isBlank(firstName) || InputCheck.isBlank(lastName) || InputCheck.isBlank(dateOfBirth) || InputCheck.isBlank(username) || InputCheck.isBlank(password)) {
            return ServiceResult.INVALID_INPUT;
        }

        // trim, så man kan logge ind uden at ramme mellemrummene præcist
        username = username.trim();

        // teksten skal kunne være i kolonnerne (VARCHAR(50))
        if (InputCheck.isTooLong(firstName, 50) || InputCheck.isTooLong(lastName, 50) || InputCheck.isTooLong(username, 50)) {
            return ServiceResult.INVALID_INPUT;
        }

        // fødselsdatoen skal være en rigtig dato
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

        // mindst 8 tegn – vi tjekker det her også, fordi man kan komme uden om minlength i HTML
        if (password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            return ServiceResult.INVALID_INPUT;
        }

        // har hun svaret ja til forløb, skal startdatoen tjekkes nu, før vi gemmer noget –
        // ellers kunne kontoen blive oprettet, mens forløbet fejler
        boolean wantsJourney = "yes".equals(hasJourney);
        if (wantsJourney) {
            if (InputCheck.isBlank(journeyStart)) {
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

        // brugernavnet skal være unikt
        if (patientMapper.findByUsername(username) != null) {
            return ServiceResult.ALREADY_EXISTS;
        }

        // ved dobbeltklik kan to forespørgsler nå forbi tjekket ovenfor – så afviser databasens unikke indeks den anden
        int patientId;
        try {
            patientId = patientMapper.save(new Patient(0, username, BCrypt.hashpw(password, BCrypt.gensalt()), firstName.trim(), lastName.trim(), dob));
        } catch (DatabaseException e) {
            if (e.isDuplicate()) {
                return ServiceResult.ALREADY_EXISTS;
            }
            throw e;   // andre databasefejl går videre til ExceptionConfig
        }

        // samme metode som "Start dit forløb" på dashboardet
        if (wantsJourney) {
            dashboardService.createJourney(patientId, journeyStart);
        }

        return ServiceResult.OK;
    }

    // Passer kodeordet med hashet? checkpw kaster en exception, hvis hashet ikke er et rigtigt BCrypt-hash
    // (fx i testdata). Det skal bare give "forkert kodeord" og ikke en 500-fejl
    public static boolean passwordMatches(String password, String hash) {
        try {
            return BCrypt.checkpw(password, hash);
        } catch (IllegalArgumentException | StringIndexOutOfBoundsException e) {
            return false;
        }
    }
}
