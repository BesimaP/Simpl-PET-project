package services;

import dao.DatabaseConnection;
import dao.NotificationDAO;
import entities.Notification;
import enums.NotificationType;
import enums.ServiceResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

// Unit tests af NotificationService (notifikationer, US12). Der er ingen service-metode, der OPRETTER notifikationer endnu,
// så testene lægger dem ind via DAO'en
class NotificationServiceTest {

    @BeforeAll
    static void setup() {
        TestData.freshDatabase();
    }

    @Test
    void newPatientHasNoNotifications() {
        int patientId = TestData.newPatient();
        NotificationService service = new NotificationService();
        assertTrue(service.getNotifications(patientId).isEmpty());
        assertEquals(0, service.countUnread(patientId));
    }

    @Test
    void markReadLowersUnreadCount() {
        int patientId = TestData.newPatient();
        int id = new NotificationDAO(DatabaseConnection.getConnection()).save(
                new Notification(0, patientId, LocalDateTime.now(), NotificationType.MEDICATION_REMINDER, "Gonal-F", "Husk 150 IU kl. 08", false));
        NotificationService service = new NotificationService();
        assertEquals(1, service.countUnread(patientId));
        assertEquals(ServiceResult.OK, service.markRead(id));
        assertEquals(0, service.countUnread(patientId));
        assertTrue(service.getNotifications(patientId).get(0).isRead());
    }
}
