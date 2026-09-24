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
        AuthService authService = new AuthService();
        // assertThrows = "vi FORVENTER, at denne kode kaster netop den exception" – testen er grøn, hvis den gør
        assertThrows(UserNotFoundException.class, () -> authService.login("findesikke", "1234"));
    }

    @Test
    void loginWithWrongPasswordThrowsException() {
        AuthService authService = new AuthService();
        authService.createProfile("Test", "1996-01-01", "auth1", "rigtigt1", "no", null);
        assertThrows(UserNotFoundException.class, () -> authService.login("auth1", "forkert1"));
    }

    @Test
    void loginWithCorrectPasswordReturnsUser() throws UserNotFoundException {
        AuthService authService = new AuthService();
        authService.createProfile("Test", "1996-01-01", "auth2", "hemmelig1", "no", null);
        assertEquals("auth2", authService.login("auth2", "hemmelig1").getUsername());
    }

    @Test
    void createProfileWithBlankNameReturnsInvalidInput() {
        ServiceResult result = new AuthService().createProfile("", "1996-01-01", "auth3", "hemmelig1", "no", null);
        // assertEquals(forventet, faktisk)
        assertEquals(ServiceResult.INVALID_INPUT, result);
    }

    @Test
    void createProfileWithInvalidDateReturnsInvalidInput() {
        ServiceResult result = new AuthService().createProfile("Test", "ikke-en-dato", "auth4", "hemmelig1", "no", null);
        assertEquals(ServiceResult.INVALID_INPUT, result);
    }

    @Test
    void createProfileWithTakenUsernameReturnsAlreadyExists() {
        AuthService authService = new AuthService();
        authService.createProfile("Første", "1996-01-01", "auth5", "hemmelig1", "no", null);            // første gang: OK
        ServiceResult result = authService.createProfile("Anden", "1996-01-01", "auth5", "hemmelig1", "no", null); // samme brugernavn igen
        assertEquals(ServiceResult.ALREADY_EXISTS, result);
    }

    @Test
    void createProfileWithJourneyButNoDateReturnsInvalidInput() {
        // "ja" til forløb, men ingen startdato -> afvises FØR noget gemmes
        ServiceResult result = new AuthService().createProfile("Test", "1996-01-01", "auth6", "hemmelig1", "yes", "");
        assertEquals(ServiceResult.INVALID_INPUT, result);
    }

    @Test
    void createProfileWithJourneyCreatesActiveJourney() throws Exception {
        AuthService authService = new AuthService();
        assertEquals(ServiceResult.OK, authService.createProfile("Test", "1996-01-01", "auth7", "hemmelig1", "yes", "2026-09-01"));
        // forløbet findes: findActiveJourney kaster IKKE
        int accountId = authService.login("auth7", "hemmelig1").getId();
        int patientId = new dao.PatientDAO(dao.DatabaseConnection.getConnection()).findByUserAccount(accountId).getId();
        assertNotNull(new DashboardService().findActiveJourney(patientId));
    }
}
