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

// Integrationstest af PatientMapper: mapperen taler med en RIGTIG PostgreSQL-database.
// Testene bruger schemaet "test" i databasen Simpl (se TestDatabase), så de aldrig rører de rigtige data i "public".
// Opskrift fra undervisningen: @BeforeAll laver tabellerne, @BeforeEach lægger frisk, kendt testdata i.
class PatientMapperTest {

    private static ConnectionPool connectionPool;
    private static PatientMapper patientMapper;

    // Køres ÉN gang før alle testene: laver alle 20 tabeller i schemaet test
    @BeforeAll
    static void setUpClass() {
        connectionPool = TestDatabase.pool();
        patientMapper = new PatientMapper(connectionPool);
        TestDatabase.createTables();
    }

    // Køres før HVER test: tøm tabellerne og indsæt de samme 2 patienter, så vi altid ved, hvad der ligger der
    @BeforeEach
    void setUp() {
        TestDatabase.clearData();   // tom + id starter forfra på 1

        try (Connection connection = connectionPool.getConnection();
             Statement stmt = connection.createStatement()) {

            // Patienter (id 1 = Anna, id 2 = Bo). password_hash er bare tekst her – mapperen hasher ikke selv
            stmt.execute("""
            INSERT INTO test.patient (username, password_hash, first_name, last_name, date_of_birth) VALUES
            ('anna', 'hash1', 'Anna', 'Jensen', '1990-05-01'),
            ('bo', 'hash2', 'Bo', 'Hansen', '1988-11-20')
            """);

        } catch (SQLException e) {
            fail("Test data setup failed: " + e.getMessage());
        }
    }

    @Test
    void testConnection() throws SQLException {
        assertNotNull(connectionPool.getConnection());
    }

    @Test
    void findByUsername() {
        Patient patient = patientMapper.findByUsername("anna");
        assertNotNull(patient);
        assertEquals(1, patient.getId());
        assertEquals("Anna", patient.getFirstName());
        assertEquals("Jensen", patient.getLastName());
        assertEquals(LocalDate.of(1990, 5, 1), patient.getDateOfBirth());
    }

    @Test
    void findByUsernameUnknownReturnsNull() {
        assertNull(patientMapper.findByUsername("findesikke"));
    }

    @Test
    void findById() {
        Patient patient = patientMapper.findById(2);
        assertNotNull(patient);
        assertEquals("bo", patient.getUsername());
    }

    @Test
    void findByIdUnknownReturnsNull() {
        assertNull(patientMapper.findById(99));
    }

    @Test
    void save() {
        Patient patient = new Patient(0, "carl", "hash3", "Carl", "Nielsen", LocalDate.of(1995, 1, 15));
        int id = patientMapper.save(patient);

        assertEquals(3, id); // der ligger 2 i forvejen, så den nye får id 3
        assertEquals("Carl", patientMapper.findById(3).getFirstName());
    }

    @Test
    void saveDuplicateUsernameThrowsDatabaseException() {
        // "anna" findes allerede, og username er UNIQUE
        Patient patient = new Patient(0, "anna", "hash3", "Anden", "Anna", LocalDate.of(2000, 1, 1));
        assertThrows(DatabaseException.class, () -> patientMapper.save(patient));
    }

    @Test
    void updateProfile() {
        patientMapper.updateProfile(1, "Annette", "Larsen", LocalDate.of(1991, 6, 2));

        Patient patient = patientMapper.findById(1);
        assertEquals("Annette", patient.getFirstName());
        assertEquals("Larsen", patient.getLastName());
        assertEquals(LocalDate.of(1991, 6, 2), patient.getDateOfBirth());
        assertEquals("anna", patient.getUsername()); // brugernavnet må ikke ændre sig
    }

    @Test
    void updatePassword() {
        patientMapper.updatePassword(2, "nythash");
        assertEquals("nythash", patientMapper.findById(2).getPasswordHash());
        assertEquals("hash1", patientMapper.findById(1).getPasswordHash()); // de andre er urørte (WHERE virker)
    }

    @Test
    void delete() {
        patientMapper.delete(1);
        assertNull(patientMapper.findById(1));
        assertNotNull(patientMapper.findById(2)); // kun den ene er væk
    }
}
