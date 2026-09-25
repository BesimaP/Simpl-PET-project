package services;

import dao.DatabaseConnection;
import dao.PatientDAO;
import dao.UserAccountDAO;
import enums.ServiceResult;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Unit tests af ProfileService (min profil, US6b)
class ProfileServiceTest {

    @BeforeAll
    static void setup() {
        TestData.freshDatabase();
    }

    @Test
    void updateNameWithBlankNameReturnsInvalidInput() {
        int patientId = TestData.newPatient();
        assertEquals(ServiceResult.INVALID_INPUT, new ProfileService().updateName(patientId, ""));
    }

    @Test
    void updateNameChangesNameInDatabase() {
        int patientId = TestData.newPatient();
        assertEquals(ServiceResult.OK, new ProfileService().updateName(patientId, "Nyt Navn"));
        // læs navnet direkte fra databasen og se, at det er ændret
        assertEquals("Nyt Navn", findPatientName(patientId));
    }

    @Test
    void changePasswordWithBlankFieldReturnsInvalidInput() {
        int accountId = TestData.newAccount();
        assertEquals(ServiceResult.INVALID_INPUT, new ProfileService().changePassword(accountId, "hemmelig1", "", ""));
    }

    @Test
    void changePasswordWithMismatchReturnsInvalidInput() {
        int accountId = TestData.newAccount();
        // de to nye er ikke ens
        assertEquals(ServiceResult.INVALID_INPUT, new ProfileService().changePassword(accountId, "hemmelig1", "nyt12345", "nyt54321"));
    }

    @Test
    void changePasswordWithWrongCurrentReturnsInvalidInput() {
        int accountId = TestData.newAccount();
        assertEquals(ServiceResult.INVALID_INPUT, new ProfileService().changePassword(accountId, "forkert1", "nyt12345", "nyt12345"));
    }

    @Test
    void changePasswordReturnsOkAndNewPasswordIsSaved() {
        int accountId = TestData.newAccount();
        assertEquals(ServiceResult.OK, new ProfileService().changePassword(accountId, "hemmelig1", "nyt12345", "nyt12345"));
        assertEquals("nyt12345", new UserAccountDAO(DatabaseConnection.getConnection()).findById(accountId).getPasswordHash());
    }

    @Test
    void deleteAccountRemovesAccountAndPatient() {
        int accountId = TestData.newAccount();
        assertEquals(ServiceResult.OK, new ProfileService().deleteAccount(accountId));
        // kontoen er væk – og patienten fulgte med (ON DELETE CASCADE)
        assertNull(new UserAccountDAO(DatabaseConnection.getConnection()).findById(accountId));
        assertNull(new PatientDAO(DatabaseConnection.getConnection()).findByUserAccount(accountId));
    }

    // lille hjælper: patientens navn direkte fra databasen
    private static String findPatientName(int patientId) {
        try {
            var rs = DatabaseConnection.getConnection().createStatement()
                    .executeQuery("SELECT name FROM patient WHERE id = " + patientId);
            return rs.next() ? rs.getString("name") : null;
        } catch (java.sql.SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getPatientReturnsNameAndDateOfBirth() {
        int patientId = TestData.newPatient(); // oprettes med "Test Bruger" og 1996-01-01
        assertEquals("Test Bruger", new ProfileService().getPatient(patientId).getName());
        assertEquals(LocalDate.of(1996, 1, 1), new ProfileService().getPatient(patientId).getDateOfBirth());
    }
}
