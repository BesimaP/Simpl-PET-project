package persistence;

import entities.Notification;
import enums.NotificationType;
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

// Integrationstest af NotificationMapper mod schema test (se TestDatabase).
class NotificationMapperTest {

    private static ConnectionPool connectionPool;
    private static NotificationMapper notificationMapper;

    @BeforeAll
    static void setUpClass() {
        connectionPool = TestDatabase.pool();
        notificationMapper = new NotificationMapper(connectionPool);
        TestDatabase.createTables();
    }

    // før hver test: 2 patienter, 3 påmindelser til Anna (2 ulæste) og 1 ulæst til Bo
    @BeforeEach
    void setUp() {
        TestDatabase.clearData();   // id'erne starter forfra på 1

        try (Connection connection = connectionPool.getConnection();
             Statement stmt = connection.createStatement()) {

            // Patienter (id 1 = Anna, id 2 = Bo)
            stmt.execute("""
            INSERT INTO test.patient (username, password_hash, first_name, last_name, date_of_birth) VALUES
            ('anna', 'hash1', 'Anna', 'Jensen', '1990-05-01'),
            ('bo', 'hash2', 'Bo', 'Hansen', '1988-11-20')
            """);

            // Påmindelser. notification_type_id 1 = MEDICATION_REMINDER, 2 = APPOINTMENT_REMINDER
            stmt.execute("""
            INSERT INTO test.notification (patient_id, date_time, notification_type_id, title, message, is_read) VALUES
            (1, '2026-09-01 08:00', 1, 'Medicin', 'Tag Gonal-F', TRUE),
            (1, '2026-09-02 08:00', 1, 'Medicin', 'Tag Gonal-F', FALSE),
            (1, '2026-09-03 09:00', 2, 'Aftale', 'Scanning kl. 10', FALSE),
            (2, '2026-09-03 09:00', 2, 'Aftale', 'Konsultation', FALSE)
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
    void findByPatient() {
        List<Notification> notifications = notificationMapper.findByPatient(1);

        assertEquals(3, notifications.size());                                     // Bos må ikke komme med
        assertEquals(NotificationType.APPOINTMENT_REMINDER, notifications.get(0).getNotificationType()); // nyeste først
        assertEquals("Scanning kl. 10", notifications.get(0).getMessage());
        assertTrue(notifications.get(2).isRead());
    }

    @Test
    void countUnread() {
        assertEquals(2, notificationMapper.countUnread(1));
        assertEquals(1, notificationMapper.countUnread(2));
    }

    @Test
    void countUnreadWithoutNotificationsIsZero() {
        assertEquals(0, notificationMapper.countUnread(99));
    }

    @Test
    void save() {
        Notification notification = new Notification(0, 2, LocalDateTime.of(2026, 9, 4, 8, 0),
                NotificationType.MEDICATION_REMINDER, "Medicin", "Tag Menopur", false);
        int id = notificationMapper.save(notification);

        assertEquals(5, id);                                                       // der ligger 4 i forvejen
        assertEquals(2, notificationMapper.countUnread(2));
    }

    @Test
    void markRead() {
        assertTrue(notificationMapper.markRead(2, 1));
        assertEquals(1, notificationMapper.countUnread(1));
    }

    @Test
    void markReadOtherPatientsNotificationDoesNothing() {
        // Anna prøver at markere Bos påmindelse (id 4) som læst
        assertFalse(notificationMapper.markRead(4, 1));
        assertEquals(1, notificationMapper.countUnread(2));
    }

    @Test
    void markAllRead() {
        notificationMapper.markAllRead(1);

        assertEquals(0, notificationMapper.countUnread(1));
        assertEquals(1, notificationMapper.countUnread(2));                        // Bos påmindelse må ikke røres
    }

    // ---------- regler, som databasen selv håndhæver ----------

    @Test
    void saveWithoutTitleThrowsDatabaseException() {
        // title er NOT NULL
        Notification notification = new Notification(0, 1, LocalDateTime.of(2026, 9, 4, 8, 0),
                NotificationType.MEDICATION_REMINDER, null, "Tekst", false);
        assertThrows(DatabaseException.class, () -> notificationMapper.save(notification));
    }
}
