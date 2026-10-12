package persistence;

import entities.HormoneLog;
import enums.HormoneType;
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

// Integrationstest af HormoneLogMapper mod schema test (se TestDatabase).
class HormoneLogMapperTest {

    private static ConnectionPool connectionPool;
    private static HormoneLogMapper hormoneLogMapper;

    @BeforeAll
    static void setUpClass() {
        connectionPool = TestDatabase.pool();
        hormoneLogMapper = new HormoneLogMapper(connectionPool);
        TestDatabase.createTables();
    }

    // før hver test: patient -> forløb -> 2 runder -> 3 målinger. Rækkefølgen skyldes fremmednøglerne
    @BeforeEach
    void setUp() {
        TestDatabase.clearData();   // id'erne starter forfra på 1

        try (Connection connection = connectionPool.getConnection();
             Statement stmt = connection.createStatement()) {

            // Patient (id 1 = Anna)
            stmt.execute("""
            INSERT INTO test.patient (username, password_hash, first_name, last_name, date_of_birth) VALUES
            ('anna', 'hash1', 'Anna', 'Jensen', '1990-05-01')
            """);

            // Forløb (id 1) til Anna. journey_status_id 1 = ACTIVE
            stmt.execute("""
            INSERT INTO test.fertility_journey (patient_id, start_date, journey_status_id) VALUES
            (1, '2026-01-10', 1)
            """);

            // Runder i forløb 1: runde 1 (id 1) er afsluttet, runde 2 (id 2) er i gang. treatment_type_id 1 = IVF
            stmt.execute("""
            INSERT INTO test.round (fertility_journey_id, round_number, treatment_type_id, start_date, end_date, result_id) VALUES
            (1, 1, 1, '2026-02-01', '2026-03-01', 2),
            (1, 2, 1, '2026-04-01', NULL, NULL)
            """);

            // Målinger. hormone_type_id 1 = FSH, 2 = LH, 3 = E2_OESTRADIOL
            // id 1 og 2 ligger i runde 2, id 3 ligger i runde 1
            stmt.execute("""
            INSERT INTO test.hormone_log (round_id, date_time, hormone_type_id, value, unit) VALUES
            (2, '2026-04-03 08:00', 1, 8.5, 'IU/L'),
            (2, '2026-04-05 08:00', 3, 1200, 'pmol/L'),
            (1, '2026-02-05 08:00', 2, 5.0, 'IU/L')
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
        List<HormoneLog> logs = hormoneLogMapper.findByRound(2);
        assertEquals(2, logs.size());                                    // målingen i runde 1 må ikke komme med
        assertEquals(HormoneType.E2_OESTRADIOL, logs.get(0).getHormoneType()); // nyeste først (ORDER BY date_time DESC)
        assertEquals(1200, logs.get(0).getValue());
        assertEquals("pmol/L", logs.get(0).getUnit());
        assertEquals(LocalDateTime.of(2026, 4, 3, 8, 0), logs.get(1).getDateTime());
    }

    @Test
    void findByRoundWithoutLogsReturnsEmptyList() {
        assertTrue(hormoneLogMapper.findByRound(99).isEmpty());
    }

    @Test
    void save() {
        HormoneLog log = new HormoneLog(0, 2, LocalDateTime.of(2026, 4, 7, 8, 0), HormoneType.PROGESTERONE, 3.2, "nmol/L");
        int id = hormoneLogMapper.save(log);

        assertEquals(4, id);                                             // der ligger 3 i forvejen
        List<HormoneLog> logs = hormoneLogMapper.findByRound(2);
        assertEquals(3, logs.size());
        assertEquals(HormoneType.PROGESTERONE, logs.get(0).getHormoneType());
    }

    @Test
    void delete() {
        assertTrue(hormoneLogMapper.delete(1, 1));

        assertEquals(1, hormoneLogMapper.findByRound(2).size());
        assertEquals(1, hormoneLogMapper.findByRound(1).size());        // runde 1 er urørt
    }

    @Test
    void deleteOtherPatientsLogDoesNothing() {
        // patient 2 ejer ikke runden, så subqueryen finder ingen runder og intet slettes
        assertFalse(hormoneLogMapper.delete(1, 2));
        assertEquals(2, hormoneLogMapper.findByRound(2).size());
    }

    // ---------- regler, som databasen selv håndhæver ----------

    @Test
    void saveNegativeValueThrowsDatabaseException() {
        // CHECK (value >= 0)
        HormoneLog log = new HormoneLog(0, 2, LocalDateTime.of(2026, 4, 7, 8, 0), HormoneType.FSH, -1, "IU/L");
        assertThrows(DatabaseException.class, () -> hormoneLogMapper.save(log));
    }

    @Test
    void saveOnUnknownRoundThrowsDatabaseException() {
        // runde 99 findes ikke, så fremmednøglen afviser
        HormoneLog log = new HormoneLog(0, 99, LocalDateTime.of(2026, 4, 7, 8, 0), HormoneType.FSH, 8.0, "IU/L");
        assertThrows(DatabaseException.class, () -> hormoneLogMapper.save(log));
    }

    @Test
    void saveZeroValueIsAllowed() {
        // grænseværdi: 0 skal accepteres her, modsat dosis hvor 0 afvises
        HormoneLog log = new HormoneLog(0, 2, LocalDateTime.of(2026, 4, 7, 8, 0), HormoneType.LH, 0, "IU/L");
        int id = hormoneLogMapper.save(log);

        assertEquals(4, id);
        assertEquals(0, hormoneLogMapper.findByRound(2).get(0).getValue());
    }
}
