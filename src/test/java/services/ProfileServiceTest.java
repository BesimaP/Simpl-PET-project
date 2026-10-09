package services;

import persistence.PatientMapper;
import enums.ServiceResult;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import static org.junit.jupiter.api.Assertions.*;

// Integrationstests af ProfileService (min profil, US6b)
class ProfileServiceTest {

    @BeforeAll
    static void setup() {
        TestData.freshDatabase();
    }

    @Test
    void updateProfileWithBlankNameReturnsInvalidInput() {
        int patientId = TestData.newPatient();
        assertEquals(ServiceResult.INVALID_INPUT, new ProfileService(TestData.pool()).updateProfile(patientId, "", "Navn", "1996-01-01"));
    }

    @Test
    void updateProfileWithBlankLastNameReturnsInvalidInput() {
        int patientId = TestData.newPatient();
        assertEquals(ServiceResult.INVALID_INPUT, new ProfileService(TestData.pool()).updateProfile(patientId, "Nyt", "", "1996-01-01"));
    }

    @Test
    void updateProfileChangesNameInDatabase() {
        int patientId = TestData.newPatient();
        assertEquals(ServiceResult.OK, new ProfileService(TestData.pool()).updateProfile(patientId, "Nyt", "Navn", "1996-01-01"));
        // læs navnet direkte fra databasen og se, at det er ændret
        assertEquals("Nyt Navn", findPatientName(patientId));
    }

    @Test
    void updateProfileChangesDateOfBirth() {
        int patientId = TestData.newPatient();
        assertEquals(ServiceResult.OK, new ProfileService(TestData.pool()).updateProfile(patientId, "Test", "Bruger", "1990-05-17"));
        assertEquals(LocalDate.of(1990, 5, 17), new ProfileService(TestData.pool()).getPatient(patientId).getDateOfBirth());
    }

    @Test
    void updateProfileWithFutureDateOfBirthReturnsInvalidInput() {
        int patientId = TestData.newPatient();
        assertEquals(ServiceResult.INVALID_INPUT, new ProfileService(TestData.pool()).updateProfile(patientId, "Test", "Bruger", "2999-01-01"));
    }

    @Test
    void changePasswordTooShortReturnsInvalidInput() {
        int patientId = TestData.newPatient();
        // under 8 tegn afvises – samme regel som ved opret profil
        assertEquals(ServiceResult.INVALID_INPUT, new ProfileService(TestData.pool()).changePassword(patientId, "hemmelig1", "kort1", "kort1"));
    }

    @Test
    void changePasswordWithBlankFieldReturnsInvalidInput() {
        int patientId = TestData.newPatient();
        assertEquals(ServiceResult.INVALID_INPUT, new ProfileService(TestData.pool()).changePassword(patientId, "hemmelig1", "", ""));
    }

    @Test
    void changePasswordWithMismatchReturnsInvalidInput() {
        int patientId = TestData.newPatient();
        // de to nye er ikke ens
        assertEquals(ServiceResult.INVALID_INPUT, new ProfileService(TestData.pool()).changePassword(patientId, "hemmelig1", "nyt12345", "nyt54321"));
    }

    @Test
    void changePasswordWithWrongCurrentReturnsInvalidInput() {
        int patientId = TestData.newPatient();
        assertEquals(ServiceResult.INVALID_INPUT, new ProfileService(TestData.pool()).changePassword(patientId, "forkert1", "nyt12345", "nyt12345"));
    }

    @Test
    void changePasswordReturnsOkAndNewPasswordIsSaved() {
        int patientId = TestData.newPatient();
        assertEquals(ServiceResult.OK, new ProfileService(TestData.pool()).changePassword(patientId, "hemmelig1", "nyt12345", "nyt12345"));
        String hash = new PatientMapper(TestData.pool()).findById(patientId).getPasswordHash();
        assertNotEquals("nyt12345", hash);               // kodeordet er IKKE gemt i klartekst
        assertTrue(BCrypt.checkpw("nyt12345", hash));     // men hashen passer med det nye kodeord
    }

    @Test
    void deleteAccountRemovesPatient() {
        int patientId = TestData.newPatient();
        assertEquals(ServiceResult.OK, new ProfileService(TestData.pool()).deleteAccount(patientId));
        // patienten (= kontoen) er væk
        assertNull(new PatientMapper(TestData.pool()).findById(patientId));
    }

    // lille hjælper: patientens navn direkte fra databasen
    private static String findPatientName(int patientId) {
        try {
            try (var connection = TestData.pool().getConnection()) {
                var rs = connection.createStatement()
                        .executeQuery("SELECT first_name, last_name FROM patient WHERE id = " + patientId);
                return rs.next() ? rs.getString("first_name") + " " + rs.getString("last_name") : null;
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getPatientReturnsNameAndDateOfBirth() {
        int patientId = TestData.newPatient(); // oprettes med "Test Bruger" og 1996-01-01
        assertEquals("Test Bruger", new ProfileService(TestData.pool()).getPatient(patientId).getName());
        assertEquals("Test", new ProfileService(TestData.pool()).getPatient(patientId).getFirstName());
        assertEquals("Bruger", new ProfileService(TestData.pool()).getPatient(patientId).getLastName());
        assertEquals(LocalDate.of(1996, 1, 1), new ProfileService(TestData.pool()).getPatient(patientId).getDateOfBirth());
    }

    @Test
    void deleteAccountAlsoDeletesUploadedFiles() throws Exception {
        int patientId = TestData.newPatient();
        DocumentService documents = new DocumentService(TestData.pool());
        documents.uploadDocument(patientId, "Henvisning", "REFERRAL", "henvisning.pdf",
                new java.io.ByteArrayInputStream("indhold".getBytes()), 100);
        java.nio.file.Path file = java.nio.file.Path.of(documents.getDocuments(patientId).get(0).getFilePath());
        assertTrue(java.nio.file.Files.exists(file));
        new ProfileService(TestData.pool()).deleteAccount(patientId);
        assertFalse(java.nio.file.Files.exists(file));   // CASCADE sletter rækken – ProfileService sletter filen
    }
}
