package persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.fail;

// Fælles opsætning til mapper-testene. De kører i schema test, så de ikke rører vores rigtige data i public.
// Vi kører vores eget schema_postgres.sql ind i test, så tabellerne er helt magen til appens.
class TestDatabase {

    private static final String USER = "postgres";
    private static final String PASSWORD = "postgres";
    private static final String URL = "jdbc:postgresql://localhost:5432/%s?currentSchema=test";
    private static final String DB = "Simpl";

    // stien er relativ til projektets rod, hvor Maven og IntelliJ kører testene fra
    private static final Path SCHEMA = Path.of("doc", "database", "schema_postgres.sql");

    // kun tabeller med patientdata – typetabellerne og medication er faste lister og skal ikke tømmes
    private static final String DATA_TABLES = """
            medication_log, hormone_log, event, appointment, round, fertility_journey,
            notification, document, diary_entry, diagnosis, patient""";

    static ConnectionPool pool() {
        return ConnectionPool.getInstance(USER, PASSWORD, URL, DB);
    }

    // kaldes fra @BeforeAll
    static void createTables() {
        try (Connection connection = pool().getConnection();
             Statement stmt = connection.createStatement()) {

            stmt.execute("CREATE SCHEMA IF NOT EXISTS test");

            // scriptet skriver ikke "public." foran tabelnavnene, så med search_path = test havner alt i test
            stmt.execute("SET search_path TO test");
            stmt.execute(Files.readString(SCHEMA));   // PostgreSQL kan køre hele filen på én gang

        } catch (IOException | SQLException e) {
            fail("Database setup failed: " + e.getMessage());
        }
    }

    // kaldes fra @BeforeEach. RESTART IDENTITY gør, at id'erne starter forfra på 1 i hver test
    static void clearData() {
        try (Connection connection = pool().getConnection();
             Statement stmt = connection.createStatement()) {

            stmt.execute("TRUNCATE " + DATA_TABLES + " RESTART IDENTITY CASCADE");

        } catch (SQLException e) {
            fail("Test data setup failed: " + e.getMessage());
        }
    }
}
