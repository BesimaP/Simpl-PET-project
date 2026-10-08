package persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.fail;

// Fælles hjælper til alle mapper-testene (integrationstest).
// Testene bruger schemaet "test" i databasen Simpl, så de aldrig rører de rigtige data i "public".
// I stedet for at skrive CREATE TABLE for alle 20 tabeller i hver testklasse, kører vi jeres eget
// schema_postgres.sql ind i schemaet test. Så har testtabellerne præcis de samme kolonner, nøgler og CHECK-regler som appen.
class TestDatabase {

    private static final String USER = "postgres";
    private static final String PASSWORD = "postgres";
    private static final String URL = "jdbc:postgresql://localhost:5432/%s?currentSchema=test";
    private static final String DB = "Simpl";

    // schemaet, der laver alle 20 tabeller + typerne og medicinlisten (stien er fra projektets rod – der kører IntelliJ/Maven testene fra)
    private static final Path SCHEMA = Path.of("doc", "database", "schema_postgres.sql");

    // tabellerne med patientdata. Typerne (treatment_type …) og medication er faste lister og bliver liggende
    private static final String DATA_TABLES = """
            medication_log, hormone_log, event, appointment, round, fertility_journey,
            notification, document, diary_entry, diagnosis, patient""";

    static ConnectionPool pool() {
        return ConnectionPool.getInstance(USER, PASSWORD, URL, DB);
    }

    // Kaldes fra @BeforeAll: laver schemaet test og alle 20 tabeller (DROP + CREATE) inde i det
    static void createTables() {
        try (Connection connection = pool().getConnection();
             Statement stmt = connection.createStatement()) {

            stmt.execute("CREATE SCHEMA IF NOT EXISTS test");

            // sikkerhed: scriptet skriver ikke "public." foran tabellerne, så med search_path = test havner alt i test
            stmt.execute("SET search_path TO test");
            stmt.execute(Files.readString(SCHEMA));   // PostgreSQL kan køre hele filen (mange sætninger adskilt af ;) på én gang

        } catch (IOException | SQLException e) {
            fail("Database setup failed: " + e.getMessage());
        }
    }

    // Kaldes fra @BeforeEach: tømmer alle tabeller med patientdata og starter id'erne forfra på 1
    // TRUNCATE … RESTART IDENTITY = DELETE + nulstil id-tælleren i ét hug. CASCADE = også tabeller, der peger på dem
    static void clearData() {
        try (Connection connection = pool().getConnection();
             Statement stmt = connection.createStatement()) {

            stmt.execute("TRUNCATE " + DATA_TABLES + " RESTART IDENTITY CASCADE");

        } catch (SQLException e) {
            fail("Test data setup failed: " + e.getMessage());
        }
    }
}
