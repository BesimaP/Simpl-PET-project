package persistence;

import entities.Appointment;
import enums.AppointmentType;
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

// Integrationstest af AppointmentMapper mod schemaet "test" i databasen Simpl (se TestDatabase).
// En aftale hænger på FORLØBET (ikke runden) – den første konsultation sker, før der er nogen runde.
class AppointmentMapperTest {

    private static ConnectionPool connectionPool;
    private static AppointmentMapper appointmentMapper;

    // Køres ÉN gang før alle testene: laver alle 20 tabeller i schemaet test
    @BeforeAll
    static void setUpClass() {
        connectionPool = TestDatabase.pool();
        appointmentMapper = new AppointmentMapper(connectionPool);
        TestDatabase.createTables();
    }

    // Køres før HVER test: kæden patient -> forløb -> 2 aftaler
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

            // Aftaler i forløb 1. appointment_type_id 1 = CONSULTATION, 2 = SCANNING
            stmt.execute("""
            INSERT INTO test.appointment (fertility_journey_id, date_time, appointment_type_id, location) VALUES
            (1, '2026-09-10 10:00', 2, 'Vitanova'),
            (1, '2026-08-20 09:00', 1, 'Rigshospitalet')
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
    void findByJourney() {
        List<Appointment> appointments = appointmentMapper.findByJourney(1);

        assertEquals(2, appointments.size());
        assertEquals(AppointmentType.CONSULTATION, appointments.get(0).getAppointmentType()); // ældste først (ORDER BY date_time)
        assertEquals("Rigshospitalet", appointments.get(0).getLocation());
        assertEquals(LocalDateTime.of(2026, 9, 10, 10, 0), appointments.get(1).getDateTime());
    }

    @Test
    void findByJourneyWithoutAppointmentsReturnsEmptyList() {
        assertTrue(appointmentMapper.findByJourney(99).isEmpty());
    }

    @Test
    void save() {
        Appointment appointment = new Appointment(0, 1, LocalDateTime.of(2026, 9, 20, 8, 30), AppointmentType.EGG_RETRIEVAL, "Vitanova");
        int id = appointmentMapper.save(appointment);

        assertEquals(3, id);
        List<Appointment> appointments = appointmentMapper.findByJourney(1);
        assertEquals(3, appointments.size());
        assertEquals(AppointmentType.EGG_RETRIEVAL, appointments.get(2).getAppointmentType()); // den seneste dato står sidst
    }

    // ---------- regler, der ligger i DATABASEN ----------

    @Test
    void saveOnUnknownJourneyThrowsDatabaseException() {
        // fremmednøgle: forløb 99 findes ikke
        Appointment appointment = new Appointment(0, 99, LocalDateTime.of(2026, 9, 20, 8, 30), AppointmentType.SCANNING, "Vitanova");
        assertThrows(DatabaseException.class, () -> appointmentMapper.save(appointment));
    }

    @Test
    void saveWithoutLocationThrowsDatabaseException() {
        // location er NOT NULL
        Appointment appointment = new Appointment(0, 1, LocalDateTime.of(2026, 9, 20, 8, 30), AppointmentType.SCANNING, null);
        assertThrows(DatabaseException.class, () -> appointmentMapper.save(appointment));
    }
}
