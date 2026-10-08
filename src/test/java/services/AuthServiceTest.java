package services;

import enums.ServiceResult;
import exceptions.UserNotFoundException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Unit tests af AuthService – uden Javalin og uden browser. Kører mod en tom database i hukommelsen, ikke simpl.db
class AuthServiceTest {

    // kører ÉN gang, før alle tests i klassen
    @BeforeAll
    static void setup() {
        TestData.freshDatabase();
    }

    @Test
    void loginWithUnknownUserThrowsException() {
        AuthService authService = new AuthService(TestData.pool());
        // assertThrows = "vi FORVENTER, at denne kode kaster netop den exception" – testen er grøn, hvis den gør
        assertThrows(UserNotFoundException.class, () -> authService.login("findesikke", "1234"));
    }

    @Test
    void loginWithWrongPasswordThrowsException() {
        AuthService authService = new AuthService(TestData.pool());
        authService.createProfile("Test", "Bruger", "1996-01-01", "auth1", "rigtigt12", "no", null);
        assertThrows(UserNotFoundException.class, () -> authService.login("auth1", "forkert1"));
    }

    @Test
    void loginWithCorrectPasswordReturnsPatientId() throws UserNotFoundException {
        AuthService authService = new AuthService(TestData.pool());
        authService.createProfile("Test", "Bruger", "1996-01-01", "auth2", "hemmelig1", "no", null);
        assertTrue(authService.login("auth2", "hemmelig1").getId() > 0);   // et rigtigt id er altid > 0
    }

    @Test
    void createProfileWithBlankNameReturnsInvalidInput() {
        ServiceResult result = new AuthService(TestData.pool()).createProfile("", "Bruger", "1996-01-01", "auth3", "hemmelig1", "no", null);
        // assertEquals(forventet, faktisk)
        assertEquals(ServiceResult.INVALID_INPUT, result);
    }

    @Test
    void createProfileWithInvalidDateReturnsInvalidInput() {
        ServiceResult result = new AuthService(TestData.pool()).createProfile("Test", "Bruger", "ikke-en-dato", "auth4", "hemmelig1", "no", null);
        assertEquals(ServiceResult.INVALID_INPUT, result);
    }

    @Test
    void createProfileWithTakenUsernameReturnsAlreadyExists() {
        AuthService authService = new AuthService(TestData.pool());
        authService.createProfile("Første", "Bruger", "1996-01-01", "auth5", "hemmelig1", "no", null);            // første gang: OK
        ServiceResult result = authService.createProfile("Anden", "Bruger", "1996-01-01", "auth5", "hemmelig1", "no", null); // samme brugernavn igen
        assertEquals(ServiceResult.ALREADY_EXISTS, result);
    }

    @Test
    void createProfileWithJourneyButNoDateReturnsInvalidInput() {
        // "ja" til forløb, men ingen startdato -> afvises FØR noget gemmes
        ServiceResult result = new AuthService(TestData.pool()).createProfile("Test", "Bruger", "1996-01-01", "auth6", "hemmelig1", "yes", "");
        assertEquals(ServiceResult.INVALID_INPUT, result);
    }

    @Test
    void createProfileWithJourneyCreatesActiveJourney() throws Exception {
        AuthService authService = new AuthService(TestData.pool());
        assertEquals(ServiceResult.OK, authService.createProfile("Test", "Bruger", "1996-01-01", "auth7", "hemmelig1", "yes", "2026-09-01"));
        // forløbet findes: login giver patientens id, og findActiveJourney kaster IKKE
        int patientId = authService.login("auth7", "hemmelig1").getId();
        assertNotNull(new DashboardService(TestData.pool()).findActiveJourney(patientId));
    }

    @Test
    void createProfileWithShortPasswordReturnsInvalidInput() {
        // under 8 tegn afvises af serveren (ikke kun af minlength i HTML)
        ServiceResult result = new AuthService(TestData.pool()).createProfile("Test", "Bruger", "1996-01-01", "auth8", "kort1", "no", null);
        assertEquals(ServiceResult.INVALID_INPUT, result);
    }

    @Test
    void createProfileWithFutureBirthDateReturnsInvalidInput() {
        ServiceResult result = new AuthService(TestData.pool()).createProfile("Test", "Bruger", "2999-01-01", "auth9", "hemmelig1", "no", null);
        assertEquals(ServiceResult.INVALID_INPUT, result);
    }

    @Test
    void loginWithBrokenHashInDatabaseThrowsExceptionInsteadOfCrashing() throws Exception {
        // en pladsholder i stedet for et rigtigt BCrypt-hash (som de gamle testdata) må ikke give en 500-fejl
        try (var connection = TestData.pool().getConnection()) {
            connection.createStatement().executeUpdate(
                    "INSERT INTO patient (username, password_hash, first_name, last_name, date_of_birth) "
                    + "VALUES ('brokenhash', '$2a$10$demo_hash', 'Test', 'Bruger', '1996-01-01')");
        }
        assertThrows(UserNotFoundException.class, () -> new AuthService(TestData.pool()).login("brokenhash", "test1234"));
    }
}
