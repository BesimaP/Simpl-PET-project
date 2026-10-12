package persistence;

import entities.MedicationLog;
import exceptions.DatabaseException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Integrationstest af MedicationLogMapper mod schema test (se TestDatabase).
// Medicinlisten (Gonal-F = 1, Orgalutran = 2 osv.) kommer fra schemaet og bliver ikke tømt af clearData().
class MedicationLogMapperTest {

    private static ConnectionPool connectionPool;
    private static MedicationLogMapper medicationLogMapper;

    @BeforeAll
    static void setUpClass() {
        connectionPool = TestDatabase.pool();
        medicationLogMapper = new MedicationLogMapper(connectionPool);
        TestDatabase.createTables();
    }

    // før hver test: patient -> forløb -> runde -> 2 doser
    @BeforeEach
    void setUp() {
        TestDatabase.clearData();   // id'erne starter forfra på 1

        try (Connection connection = connectionPool.getConnection();
             Statement stmt = connection.createStatement()) {

            stmt.execute("""
            INSERT INTO test.patient (username, password_hash, first_name, last_name, date_of_birth) VALUES
            ('anna', 'hash1', 'Anna', 'Jensen', '1990-05-01')
            """);
            stmt.execute("""
            INSERT INTO test.fertility_journey (patient_id, start_date, journey_status_id) VALUES
            (1, '2026-01-10', 1)
            """);
            // runde 1 er i gang
            stmt.execute("""
            INSERT INTO test.round (fertility_journey_id, round_number, treatment_type_id, start_date) VALUES
            (1, 1, 1, '2026-09-01')
            """);

            // id 1 = Gonal-F 150 IU (taget), id 2 = Orgalutran 0,25 mg (ikke taget)
            stmt.execute("""
            INSERT INTO test.medication_log (round_id, medication_id, scheduled_date_time, dose, taken) VALUES
            (1, 1, '2026-09-02 20:00', 150, TRUE),
            (1, 2, '2026-09-03 08:00', 0.25, FALSE)
            """);

        } catch (SQLException e) {
            fail("Test data setup failed: " + e.getMessage());
        }
    }

    @Test
    void testConnection() throws SQLException {
        try (Connection connection = connectionPool.getConnection()) {
            assertNotNull(connection);
        }
    }

    @Test
    void findByRound() {
        List<MedicationLog> logs = medicationLogMapper.findByRound(1);

        assertEquals(2, logs.size());
        assertEquals(1, logs.get(0).getMedicationId());   // ældste først
        assertEquals(150.0, logs.get(0).getDose());
        assertEquals("IU", logs.get(0).getUnit());          // enheden kommer fra medication via JOIN
        assertEquals("mg", logs.get(1).getUnit());
        assertTrue(logs.get(0).isTaken());
        assertFalse(logs.get(1).isTaken());
    }

    @Test
    void findByRoundWithoutDosesReturnsEmptyList() {
        assertTrue(medicationLogMapper.findByRound(99).isEmpty());
    }

    @Test
    void save() {
        // Menopur (id 3) 75 IU
        MedicationLog log = new MedicationLog(0, 1, 3, LocalDateTime.of(2026, 9, 4, 20, 0), 75, null, false);
        int id = medicationLogMapper.save(log);

        assertEquals(3, id);                                         // der ligger 2 i forvejen
        List<MedicationLog> logs = medicationLogMapper.findByRound(1);
        assertEquals(3, logs.size());
        assertEquals("IU", logs.get(2).getUnit());                   // enheden gemmes ikke i medication_log, den hentes fra medication
    }

    @Test
    void markTaken() {
        assertTrue(medicationLogMapper.markTaken(2, 1));
        assertTrue(medicationLogMapper.findByRound(1).get(1).isTaken());
    }

    @Test
    void markNotTaken() {
        assertTrue(medicationLogMapper.markNotTaken(1, 1));
        assertFalse(medicationLogMapper.findByRound(1).get(0).isTaken());
    }

    @Test
    void delete() {
        assertTrue(medicationLogMapper.delete(1, 1));

        List<MedicationLog> logs = medicationLogMapper.findByRound(1);
        assertEquals(1, logs.size());
        assertEquals(2, logs.get(0).getId());
    }

    @Test
    void otherPatientCannotChangeDose() {
        // patient 2 ejer ikke runden, så dosen skal være urørt
        assertFalse(medicationLogMapper.markTaken(2, 2));
        assertFalse(medicationLogMapper.delete(1, 2));
        assertEquals(2, medicationLogMapper.findByRound(1).size());
        assertFalse(medicationLogMapper.findByRound(1).get(1).isTaken());
    }

    // ---------- regler, som databasen selv håndhæver ----------

    @Test
    void saveZeroDoseThrowsDatabaseException() {
        // CHECK (dose > 0)
        MedicationLog log = new MedicationLog(0, 1, 1, LocalDateTime.of(2026, 9, 4, 20, 0), 0, null, false);
        assertThrows(DatabaseException.class, () -> medicationLogMapper.save(log));
    }

    @Test
    void saveUnknownMedicationThrowsDatabaseException() {
        // medication_id 999 findes ikke i medicinlisten
        MedicationLog log = new MedicationLog(0, 1, 999, LocalDateTime.of(2026, 9, 4, 20, 0), 50, null, false);
        assertThrows(DatabaseException.class, () -> medicationLogMapper.save(log));
    }

    @Test
    void saveOnUnknownRoundThrowsDatabaseException() {
        // runde 99 findes ikke
        MedicationLog log = new MedicationLog(0, 99, 1, LocalDateTime.of(2026, 9, 4, 20, 0), 50, null, false);
        assertThrows(DatabaseException.class, () -> medicationLogMapper.save(log));
    }
}
