package persistence;

import entities.Diagnosis;
import exceptions.DatabaseException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Integrationstest af DiagnosisMapper mod schemaet "test" i databasen Simpl (se TestDatabase).
// En diagnose hænger direkte på patienten (patient_id) – intet forløb eller runde.
class DiagnosisMapperTest {

    private static ConnectionPool connectionPool;
    private static DiagnosisMapper diagnosisMapper;

    // Køres ÉN gang før alle testene: laver alle 20 tabeller i schemaet test
    @BeforeAll
    static void setUpClass() {
        connectionPool = TestDatabase.pool();
        diagnosisMapper = new DiagnosisMapper(connectionPool);
        TestDatabase.createTables();
    }

    // Køres før HVER test: 2 patienter og 2 diagnoser til Anna
    @BeforeEach
    void setUp() {
        TestDatabase.clearData();   // tom + id starter forfra på 1

        try (Connection connection = connectionPool.getConnection();
             Statement stmt = connection.createStatement()) {

            // Patienter (id 1 = Anna, id 2 = Bo)
            stmt.execute("""
            INSERT INTO test.patient (username, password_hash, first_name, last_name, date_of_birth) VALUES
            ('anna', 'hash1', 'Anna', 'Jensen', '1990-05-01'),
            ('bo', 'hash2', 'Bo', 'Hansen', '1988-11-20')
            """);

            // Diagnoser til Anna: id 1 = PCOS (med beskrivelse), id 2 = Endometriose (uden beskrivelse)
            stmt.execute("""
            INSERT INTO test.diagnosis (patient_id, name, description) VALUES
            (1, 'PCOS', 'Polycystisk ovariesyndrom'),
            (1, 'Endometriose', NULL)
            """);

        } catch (SQLException e) {
            fail("Test data setup failed: " + e.getMessage());
        }
    }

    @Test
    void testConnection() throws SQLException {
        // try ( … ): forbindelsen afleveres igen bagefter, så testen ikke "stjæler" en fra nøgleringen
        try (Connection connection = connectionPool.getConnection()) {
            assertNotNull(connection);
        }
    }

    @Test
    void findByPatient() {
        List<Diagnosis> diagnoses = diagnosisMapper.findByPatient(1);

        assertEquals(2, diagnoses.size());
        assertEquals("Endometriose", diagnoses.get(0).getName());   // alfabetisk (ORDER BY name)
        assertNull(diagnoses.get(0).getDescription());               // NULL i databasen -> null i Java
        assertEquals("PCOS", diagnoses.get(1).getName());
    }

    @Test
    void findByPatientWithoutDiagnosesReturnsEmptyList() {
        assertTrue(diagnosisMapper.findByPatient(2).isEmpty());      // Bo har ingen diagnoser
    }

    @Test
    void save() {
        Diagnosis diagnosis = new Diagnosis(0, 2, "Lav AMH", null);
        int id = diagnosisMapper.save(diagnosis);

        assertEquals(3, id);                                         // der ligger 2 i forvejen
        assertEquals(id, diagnosis.getId());                         // mapperen sætter id'et på objektet
        assertEquals("Lav AMH", diagnosisMapper.findByPatient(2).get(0).getName());
    }

    @Test
    void deleteOwnDiagnosis() {
        assertTrue(diagnosisMapper.delete(1, 1));                    // Anna sletter sin egen PCOS
        assertEquals(1, diagnosisMapper.findByPatient(1).size());
    }

    @Test
    void deleteOtherPatientsDiagnosisDoesNothing() {
        // Bo (id 2) prøver at slette Annas diagnose (id 1) -> WHERE id = ? AND patient_id = ? rammer 0 rækker
        assertFalse(diagnosisMapper.delete(1, 2));
        assertEquals(2, diagnosisMapper.findByPatient(1).size());    // Annas diagnoser er urørte
    }

    @Test
    void deleteUnknownDiagnosisReturnsFalse() {
        assertFalse(diagnosisMapper.delete(99, 1));
    }

    // ---------- regler, der ligger i DATABASEN ----------

    @Test
    void saveOnUnknownPatientThrowsDatabaseException() {
        // fremmednøgle: patient 99 findes ikke
        Diagnosis diagnosis = new Diagnosis(0, 99, "PCOS", null);
        assertThrows(DatabaseException.class, () -> diagnosisMapper.save(diagnosis));
    }

    @Test
    void saveTooLongNameThrowsDatabaseException() {
        // name er VARCHAR(100) – 101 tegn kan ikke være i kolonnen
        Diagnosis diagnosis = new Diagnosis(0, 1, "x".repeat(101), null);
        assertThrows(DatabaseException.class, () -> diagnosisMapper.save(diagnosis));
    }

    @Test
    void deletingPatientDeletesDiagnoses() {
        // ON DELETE CASCADE: slettes patienten (kontoen), forsvinder diagnoserne også (NFR2)
        new PatientMapper(connectionPool).delete(1);
        assertTrue(diagnosisMapper.findByPatient(1).isEmpty());
    }
}
