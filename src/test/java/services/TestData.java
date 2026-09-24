package services;

import dao.DatabaseConnection;
import dao.DatabaseInitializer;
import dao.PatientDAO;
import dao.UserAccountDAO;

// Hjælper til alle testklasser: sætter testdatabasen op og laver de "kort", en test skal bruge (patient, forløb, runde).
// Ligger i src/test – er ikke med i selve programmet.
class TestData {

    private static int counter = 0; // tæller op, så hvert kald får et nyt, unikt brugernavn

    // tom database i hukommelsen + tabellerne fra schema.sql. Kaldes fra @BeforeAll i hver testklasse
    static void freshDatabase() {
        DatabaseConnection.useTestDatabase();
        DatabaseInitializer.initialize();
    }

    // opretter konto + patient via AuthService og returnerer patientens id
    static int newPatient() {
        String username = "testuser" + (++counter);
        new AuthService().createProfile("Test Bruger", "1996-01-01", username, "hemmelig1", "no", null);
        int accountId = new UserAccountDAO(DatabaseConnection.getConnection()).findByUsername(username).getId();
        return new PatientDAO(DatabaseConnection.getConnection()).findByUserAccount(accountId).getId();
    }

    // kontoens id for en patient (bruges af ProfileService-tests)
    static int newAccount() {
        String username = "testaccount" + (++counter);
        new AuthService().createProfile("Test Bruger", "1996-01-01", username, "hemmelig1", "no", null);
        return new UserAccountDAO(DatabaseConnection.getConnection()).findByUsername(username).getId();
    }

    // patient med et aktivt forløb
    static int newPatientWithJourney() {
        int patientId = newPatient();
        new DashboardService().createJourney(patientId, "2026-09-01");
        return patientId;
    }

    // patient med aktivt forløb OG en runde i gang
    static int newPatientWithRound() {
        int patientId = newPatientWithJourney();
        new RoundService().startRound(patientId, "IVF", "2026-09-10");
        return patientId;
    }
}
