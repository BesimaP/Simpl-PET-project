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

// Integrationstest af MedicationLogMapper mod schemaet "test" i databasen Simpl (se TestDatabase).
// En dosis hænger på en runde (round_id) OG på et præparat i medicinlisten (medication_id).
// Medicinlisten (Gonal-F = 1, Orgalutran = 2 …) fyldes af schema_postgres.sql og tømmes IKKE af clearData().
class MedicationLogMapperTest {

    private static ConnectionPool connectionPool;
    private static MedicationLogMapper medicationLogMapper;

    // Køres ÉN gang før alle testene: laver alle 20 tabeller i schemaet test
    @BeforeAll
    static void setUpClass() {
        connectionPool = TestDatabase.pool();
        medicationLogMapper = new MedicationLogMapper(connectionPool);
        TestDatabase.createTables();
    }

    // Køres før HVER test: kæden patient -> forløb -> runde -> 2 doser
    @BeforeEach
    void setUp() {
        TestDatabase.clearData();   // tom + id starter forfra på 1

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
            // runde 1 i gang (IVF)
            stmt.execute("""
            INSERT INTO test.round (fertility_journey_id, round_number, treatment_type_id, start_date) VALUES
            (1, 1, 1, '2026-09-01')
            """);

            // Doser i runde 1: id 1 = Gonal-F 150 IU (taget), id 2 = Orgalutran 0,25 mg (ikke taget endnu)
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
        // try ( … ): forbindelsen afleveres igen bagefter, så testen ikke "stjæler" en fra nøgleringen
        try (Connection connection = connectionPool.getConnection()) {
            assertNotNull(connection);
        }
    }

    @Test
    void findByRound() {
        List<MedicationLog> logs = medicationLogMapper.findByRound(1);

        assertEquals(2, logs.size());
        assertEquals(1, logs.get(0).getMedicationId());   // ældste først (ORDER BY scheduled_date_time)
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
        assertEquals("IU", logs.get(2).getUnit());                   // enheden blev ikke gemt i medication_log, men hentes fra medication
    }

    @Test
    void markTaken() {
        medicationLogMapper.markTaken(2);
        assertTrue(medicationLogMapper.findByRound(1).get(1).isTaken());
    }

    @Test
    void markNotTaken() {
        medicationLogMapper.markNotTaken(1);
        assertFalse(medicationLogMapper.findByRound(1).get(0).isTaken());
    }

    @Test
    void delete() {
        medicationLogMapper.delete(1);

        List<MedicationLog> logs = medicationLogMapper.findByRound(1);
        assertEquals(1, logs.size());
        assertEquals(2, logs.get(0).getId());                        // kun den anden dosis er tilbage
    }

    // ---------- regler, der ligger i DATABASEN ----------

    @Test
    void saveZeroDoseThrowsDatabaseException() {
        // CHECK (dose > 0)
        MedicationLog log = new MedicationLog(0, 1, 1, LocalDateTime.of(2026, 9, 4, 20, 0), 0, null, false);
        assertThrows(DatabaseException.class, () -> medicationLogMapper.save(log));
    }

    @Test
    void saveUnknownMedicationThrowsDatabaseException() {
        // fremmednøgle: medication_id 999 findes ikke i medicinlisten
        MedicationLog log = new MedicationLog(0, 1, 999, LocalDateTime.of(2026, 9, 4, 20, 0), 50, null, false);
        assertThrows(DatabaseException.class, () -> medicationLogMapper.save(log));
    }

    @Test
    void saveOnUnknownRoundThrowsDatabaseException() {
        // fremmednøgle: round_id 99 findes ikke
        MedicationLog log = new MedicationLog(0, 99, 1, LocalDateTime.of(2026, 9, 4, 20, 0), 50, null, false);
        assertThrows(DatabaseException.class, () -> medicationLogMapper.save(log));
    }
}
