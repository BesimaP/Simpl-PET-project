package persistence;

import entities.Event;
import enums.EventType;
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

// Integrationstest af EventMapper mod schemaet "test" i databasen Simpl (se TestDatabase).
// En hændelse (fx ægudtagning) hænger på RUNDEN. Bruges til tidslinjen.
class EventMapperTest {

    private static ConnectionPool connectionPool;
    private static EventMapper eventMapper;

    // Køres ÉN gang før alle testene: laver alle 20 tabeller i schemaet test
    @BeforeAll
    static void setUpClass() {
        connectionPool = TestDatabase.pool();
        eventMapper = new EventMapper(connectionPool);
        TestDatabase.createTables();
    }

    // Køres før HVER test: kæden patient -> forløb -> runde -> 2 hændelser
    @BeforeEach
    void setUp() {
        TestDatabase.clearData();   // tom + id starter forfra på 1

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

            // Runde 1 i gang (IVF)
            stmt.execute("""
            INSERT INTO test.round (fertility_journey_id, round_number, treatment_type_id, start_date) VALUES
            (1, 1, 1, '2026-09-01')
            """);

            // Hændelser i runde 1. event_type_id 1 = STIMULATION_START, 2 = EGG_RETRIEVAL
            stmt.execute("""
            INSERT INTO test.event (round_id, date_time, event_type_id, description) VALUES
            (1, '2026-09-14 08:00', 2, '8 æg'),
            (1, '2026-09-01 20:00', 1, NULL)
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
        List<Event> events = eventMapper.findByRound(1);

        assertEquals(2, events.size());
        assertEquals(EventType.STIMULATION_START, events.get(0).getEventType());   // ældste først (ORDER BY date_time)
        assertNull(events.get(0).getDescription());                                 // description må være NULL
        assertEquals("8 æg", events.get(1).getDescription());
    }

    @Test
    void findByRoundWithoutEventsReturnsEmptyList() {
        assertTrue(eventMapper.findByRound(99).isEmpty());
    }

    @Test
    void save() {
        Event event = new Event(0, 1, LocalDateTime.of(2026, 9, 17, 11, 0), EventType.EMBRYO_TRANSFER, "1 blastocyst");
        int id = eventMapper.save(event);

        assertEquals(3, id);
        assertEquals(EventType.EMBRYO_TRANSFER, eventMapper.findByRound(1).get(2).getEventType());
    }

    // ---------- regler, der ligger i DATABASEN ----------

    @Test
    void saveOnUnknownRoundThrowsDatabaseException() {
        // fremmednøgle: runde 99 findes ikke
        Event event = new Event(0, 99, LocalDateTime.of(2026, 9, 17, 11, 0), EventType.EMBRYO_TRANSFER, null);
        assertThrows(DatabaseException.class, () -> eventMapper.save(event));
    }

    @Test
    void deletingPatientDeletesEvents() {
        // ON DELETE CASCADE: slettes patienten, forsvinder forløb -> runde -> hændelser også (NFR2)
        new PatientMapper(connectionPool).delete(1);
        assertTrue(eventMapper.findByRound(1).isEmpty());
    }
}
