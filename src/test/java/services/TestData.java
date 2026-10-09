package services;

import persistence.ConnectionPool;
import persistence.PatientMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

// Hjælper til alle testklasser: sætter testdatabasen op og laver de "kort", en test skal bruge (patient, forløb, runde).
// Ligger i src/test – er ikke med i selve programmet.
//
// Testene kører mod schemaet "test" i databasen Simpl (ikke "public"), så testene aldrig rører jeres rigtige data.
// Samme opsætning som mapper-testene (src/test/java/persistence/TestDatabase.java).
// Schemaet og tabellerne laver testene selv (de kører schema_postgres.sql før hver testklasse).
class TestData {

    // samme opsætning som i Main – kun schemaet er anderledes (test i stedet for public)
    private static final String USER = "postgres";
    private static final String PASSWORD = "postgres";
    private static final String URL = "jdbc:postgresql://localhost:5432/%s?currentSchema=test";
    private static final String DB = "Simpl";

    // schemaet, der laver alle 20 tabeller (stien er fra projektets rod – der kører IntelliJ/Maven testene fra)
    private static final Path SCHEMA = Path.of("doc", "database", "schema_postgres.sql");

    private static int counter = 0; // tæller op, så hvert kald får et nyt, unikt brugernavn

    // nøgleringen til TESTdatabasen. Laves første gang, den skal bruges
    static ConnectionPool pool() {
        return ConnectionPool.getInstance(USER, PASSWORD, URL, DB);
    }

    // tomt schema test: kør schema_postgres.sql (DROP + CREATE af alle tabeller). Kaldes fra @BeforeAll i hver testklasse
    static void freshDatabase() {
        try (Connection connection = pool().getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS test");
            statement.execute("SET search_path TO test");   // sikkerhed: så scriptet ALDRIG rammer public
            String sql = Files.readString(SCHEMA);
            statement.execute(sql);   // PostgreSQL kan køre hele filen (mange sætninger adskilt af ;) på én gang
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

    // patient med aktivt forløb OG en runde i gang
    static int newPatientWithRound() {
        int patientId = newPatientWithJourney();
        new RoundService(pool()).startRound(patientId, "IVF", "2026-09-10");
        return patientId;
    }

    // Tidslinjen for runden, der er i gang – samme to kald som TimelineController (findActiveRound + getEvents(patientId, roundId))
    static java.util.List<entities.Event> eventsInActiveRound(int patientId) throws Exception {
        entities.Round round = new RoundService(pool()).findActiveRound(patientId);
        return new TimelineService(pool()).getEvents(patientId, round.getId());
    }

    // id på det aktive forløb – bruges, når en test skal kalde de samme metoder som AppointmentController (getPast(patientId, journeyId))
    static int activeJourneyId(int patientId) throws Exception {
        return new DashboardService(pool()).findActiveJourney(patientId).getId();
    }
}
