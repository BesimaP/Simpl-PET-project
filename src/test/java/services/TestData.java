package services;

import persistence.ConnectionPool;
import persistence.PatientMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

// Fælles hjælper til service-testene: sætter testdatabasen op og laver patient, forløb og runde.
// Kører mod schema test ligesom TestDatabase i persistence, så vores rigtige data i public ikke bliver rørt.
class TestData {

    // samme som i Main, bare med currentSchema=test
    private static final String USER = "postgres";
    private static final String PASSWORD = "postgres";
    private static final String URL = "jdbc:postgresql://localhost:5432/%s?currentSchema=test";
    private static final String DB = "Simpl";

    // stien er relativ til projektets rod, hvor Maven og IntelliJ kører testene fra
    private static final Path SCHEMA = Path.of("doc", "database", "schema_postgres.sql");

    private static int counter = 0; // så hver testpatient får sit eget brugernavn

    static ConnectionPool pool() {
        return ConnectionPool.getInstance(USER, PASSWORD, URL, DB);
    }

    // kører schema_postgres.sql, så alle tabeller laves forfra. Kaldes fra @BeforeAll i hver testklasse
    static void freshDatabase() {
        try (Connection connection = pool().getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS test");
            statement.execute("SET search_path TO test");   // så scriptet ikke kommer til at ramme public
            String sql = Files.readString(SCHEMA);
            statement.execute(sql);   // PostgreSQL kan køre hele filen på én gang
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Kunne ikke lave testtabellerne i schemaet test i databasen Simpl", e);
        }
    }

    // opretter en patient via AuthService og returnerer patientens id
    static int newPatient() {
        String username = "testuser" + (++counter);
        new AuthService(pool()).createProfile("Test", "Bruger", "1996-01-01", username, "hemmelig1", "no", null);
        return new PatientMapper(pool()).findByUsername(username).getId();
    }

    // patient med et aktivt forløb
    static int newPatientWithJourney() {
        int patientId = newPatient();
        new DashboardService(pool()).createJourney(patientId, "2026-09-01");
        return patientId;
    }

    // patient med aktivt forløb og en runde i gang
    static int newPatientWithRound() {
        int patientId = newPatientWithJourney();
        new RoundService(pool()).startRound(patientId, "IVF", "2026-09-10");
        return patientId;
    }

    // tidslinjen for runden i gang – samme to kald som TimelineController laver
    static java.util.List<entities.Event> eventsInActiveRound(int patientId) throws Exception {
        entities.Round round = new RoundService(pool()).findActiveRound(patientId);
        return new TimelineService(pool()).getEvents(patientId, round.getId());
    }

    // til tests, der kalder getPast(patientId, journeyId) ligesom AppointmentController
    static int activeJourneyId(int patientId) throws Exception {
        return new DashboardService(pool()).findActiveJourney(patientId).getId();
    }
}
