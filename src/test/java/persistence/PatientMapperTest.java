package persistence;

import entities.Patient;
import exceptions.DatabaseException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class PatientMapperTest {

    private static final String USER = "postgres";
    private static final String PASSWORD = "postgres";
    private static final String URL =
            "jdbc:postgresql://localhost:5432/Simpl?currentSchema=test";

    private static ConnectionPool connectionPool;
    private static PatientMapper patientMapper;

    @BeforeAll
    static void setUpClass() {
        connectionPool = ConnectionPool.getInstance(USER, PASSWORD, URL, "");
        patientMapper = new PatientMapper(connectionPool);

        try (Connection connection = connectionPool.getConnection();
             Statement stmt = connection.createStatement()) {

            // Ryd op efter tidligere kørsler (kun i test!)
            stmt.execute("DROP TABLE IF EXISTS test.patient CASCADE");

            // Kopiér tabelstrukturen fra public (uden data).
            // LIKE ... INCLUDING ALL tager også primærnøgle, UNIQUE og GENERATED ... AS IDENTITY med,
            // så vi ikke selv skal lave en sekvens til id
            stmt.execute("CREATE TABLE test.patient (LIKE public.patient INCLUDING ALL)");

        } catch (SQLException e) {
            fail("Database setup failed: " + e.getMessage());
        }
    }

    @BeforeEach
    void setUp() {
        try (Connection connection = connectionPool.getConnection();
             Statement stmt = connection.createStatement()) {

            // Tøm test-tabellen
            stmt.execute("DELETE FROM test.patient");

            // Nulstil id'er, så første patient altid får id 1
            stmt.execute("ALTER TABLE test.patient ALTER COLUMN id RESTART WITH 1");

            // Patienter
            stmt.execute("""
                INSERT INTO test.patient (username, password_hash, first_name, last_name, date_of_birth) VALUES
                ('mette1990', 'hash1', 'Mette', 'Jensen', '1990-05-12'),
                ('sara1988', 'hash2', 'Sara', 'Hansen', '1988-11-03')
                """);

        } catch (SQLException e) {
            fail("Database setup failed: " + e.getMessage());
        }
    }

    @Test
    void testConnection() throws SQLException {
        assertNotNull(connectionPool.getConnection());
    }

    @Test
    void findByUsername() {
        Patient patient = patientMapper.findByUsername("mette1990");

        assertNotNull(patient);
        assertEquals(1, patient.getId());
        assertEquals("Mette", patient.getFirstName());
        assertEquals(LocalDate.of(1990, 5, 12), patient.getDateOfBirth());
    }

    @Test
    void findByUsernameNotFound() {
        assertNull(patientMapper.findByUsername("findesikke"));
    }

    @Test
    void findById() {
        Patient patient = patientMapper.findById(2);

        assertNotNull(patient);
        assertEquals("sara1988", patient.getUsername());
    }

    @Test
    void save() {
        Patient newPatient = new Patient(0, "lise1995", "hash3", "Lise", "Nielsen", LocalDate.of(1995, 1, 20));

        int id = patientMapper.save(newPatient);

        assertEquals(3, id);
        assertEquals("Lise", patientMapper.findById(3).getFirstName());
    }

    @Test
    void saveDuplicateUsername() {
        Patient duplicate = new Patient(0, "mette1990", "hash3", "Mette", "Andersen", LocalDate.of(1992, 2, 2));

        // username er UNIQUE, så databasen afviser den, og mapperen kaster DatabaseException
        assertThrows(DatabaseException.class, () -> patientMapper.save(duplicate));
    }

    @Test
    void updateProfile() {
        patientMapper.updateProfile(1, "Mette", "Larsen", LocalDate.of(1990, 6, 1));

        Patient patient = patientMapper.findById(1);
        assertEquals("Larsen", patient.getLastName());
        assertEquals(LocalDate.of(1990, 6, 1), patient.getDateOfBirth());
    }

    @Test
    void updatePassword() {
        patientMapper.updatePassword(1, "nytHash");

        assertEquals("nytHash", patientMapper.findById(1).getPasswordHash());
    }

    @Test
    void delete() {
        patientMapper.delete(1);

        assertNull(patientMapper.findById(1));
        assertNotNull(patientMapper.findById(2));
    }
}
