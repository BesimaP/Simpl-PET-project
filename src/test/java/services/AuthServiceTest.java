package services;

import enums.ServiceResult;
import exceptions.UserNotFoundException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Integrationstest af AuthService uden Javalin. Kører mod schema test (se TestData).
class AuthServiceTest {

    @BeforeAll
    static void setup() {
        TestData.freshDatabase();
    }

    @Test
    void loginWithUnknownUserThrowsException() {
        AuthService authService = new AuthService(TestData.pool());
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
        assertTrue(authService.login("auth2", "hemmelig1").getId() > 0);
    }

    @Test
    void createProfileWithBlankNameReturnsInvalidInput() {
        ServiceResult result = new AuthService(TestData.pool()).createProfile("", "Bruger", "1996-01-01", "auth3", "hemmelig1", "no", null);
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
        authService.createProfile("Første", "Bruger", "1996-01-01", "auth5", "hemmelig1", "no", null);
        ServiceResult result = authService.createProfile("Anden", "Bruger", "1996-01-01", "auth5", "hemmelig1", "no", null);
        assertEquals(ServiceResult.ALREADY_EXISTS, result);
    }

    @Test
    void createProfileWithJourneyButNoDateReturnsInvalidInput() {
        // "ja" til forløb uden startdato skal afvises, før der gemmes noget
        ServiceResult result = new AuthService(TestData.pool()).createProfile("Test", "Bruger", "1996-01-01", "auth6", "hemmelig1", "yes", "");
        assertEquals(ServiceResult.INVALID_INPUT, result);
    }

    @Test
    void usernameIsTrimmedWhenCreatingAndLoggingIn() throws Exception {
        AuthService authService = new AuthService(TestData.pool());
        assertEquals(ServiceResult.OK, authService.createProfile("Test", "Bruger", "1996-01-01", "  auth9  ", "hemmelig1", "no", ""));
        // brugernavnet gemmes uden mellemrum, og login virker både med og uden
        assertEquals("auth9", authService.login("auth9", "hemmelig1").getUsername());
        assertEquals("auth9", authService.login(" auth9 ", "hemmelig1").getUsername());
    }

    @Test
    void createProfileWithTooLongPasswordReturnsInvalidInput() {
        // BCrypt bruger kun de første 72 bytes, så vi afviser længere kodeord i stedet for at skære dem af
        String longPassword = "a".repeat(73);
        ServiceResult result = new AuthService(TestData.pool()).createProfile("Test", "Bruger", "1996-01-01", "auth8", longPassword, "no", "");
        assertEquals(ServiceResult.INVALID_INPUT, result);
    }

    @Test
    void createProfileWithJourneyCreatesActiveJourney() throws Exception {
        AuthService authService = new AuthService(TestData.pool());
        assertEquals(ServiceResult.OK, authService.createProfile("Test", "Bruger", "1996-01-01", "auth7", "hemmelig1", "yes", "2026-09-01"));
        int patientId = authService.login("auth7", "hemmelig1").getId();
        assertNotNull(new DashboardService(TestData.pool()).findActiveJourney(patientId));
    }

    @Test
    void createProfileWithShortPasswordReturnsInvalidInput() {
        // serveren skal også afvise under 8 tegn, ikke kun minlength i HTML
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
        // en pladsholder i stedet for en rigtig BCrypt-hash (som i de gamle testdata) må ikke give en 500-fejl
        try (var connection = TestData.pool().getConnection()) {
            connection.createStatement().executeUpdate(
                    "INSERT INTO patient (username, password_hash, first_name, last_name, date_of_birth) "
                    + "VALUES ('brokenhash', '$2a$10$demo_hash', 'Test', 'Bruger', '1996-01-01')");
        }
        assertThrows(UserNotFoundException.class, () -> new AuthService(TestData.pool()).login("brokenhash", "test1234"));
    }
}
