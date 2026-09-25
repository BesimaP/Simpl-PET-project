package services;

import dao.DatabaseConnection;
import dao.NotificationDAO;
import entities.Notification;
import enums.NotificationType;
import enums.ServiceResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

// Unit tests af NotificationService (notifikationer, US12)
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

    @Test
    void reminderIsCreatedForPlannedDoseToday() {
        int patientId = TestData.newPatientWithRound();
        new MedicationService().logDose(patientId, "GONAL_F", "150", "IU", LocalDate.now().toString(), "20:00", false);
        assertEquals(1, new NotificationService().createMedicationReminders(patientId));
        assertEquals("Gonal-F · 150.0 IU · kl. 20:00", new NotificationService().getNotifications(patientId).get(0).getMessage());
        assertEquals(NotificationType.MEDICATION_REMINDER, new NotificationService().getNotifications(patientId).get(0).getNotificationType());
    }

    @Test
    void noReminderForDoseAlreadyTaken() {
        int patientId = TestData.newPatientWithRound();
        new MedicationService().logDose(patientId, "GONAL_F", "150", "IU", LocalDate.now().toString(), "08:00", true);
        assertEquals(0, new NotificationService().createMedicationReminders(patientId));
    }

    @Test
    void noReminderForDoseTomorrow() {
        int patientId = TestData.newPatientWithRound();
        new MedicationService().logDose(patientId, "GONAL_F", "150", "IU", LocalDate.now().plusDays(1).toString(), "08:00", false);
        assertEquals(0, new NotificationService().createMedicationReminders(patientId));
    }

    @Test
    void sameReminderIsNotCreatedTwice() {
        int patientId = TestData.newPatientWithRound();
        new MedicationService().logDose(patientId, "MENOPUR", "75", "IU", LocalDate.now().toString(), "20:00", false);
        // dashboard åbnes to gange -> stadig kun én påmindelse
        assertEquals(1, new NotificationService().createMedicationReminders(patientId));
        assertEquals(0, new NotificationService().createMedicationReminders(patientId));
        assertEquals(1, new NotificationService().getNotifications(patientId).size());
    }

    @Test
    void reminderWithoutRoundCreatesNothing() {
        int patientId = TestData.newPatientWithJourney();
        assertEquals(0, new NotificationService().createMedicationReminders(patientId));
    }

    @Test
    void markAllReadClearsUnreadCount() {
        int patientId = TestData.newPatientWithRound();
        new MedicationService().logDose(patientId, "GONAL_F", "150", "IU", LocalDate.now().toString(), "08:00", false);
        new MedicationService().logDose(patientId, "MENOPUR", "75", "IU", LocalDate.now().toString(), "20:00", false);
        assertEquals(2, new NotificationService().createMedicationReminders(patientId));
        assertEquals(ServiceResult.OK, new NotificationService().markAllRead(patientId));
        assertEquals(0, new NotificationService().countUnread(patientId));
    }

    @Test
    void markAllReadDoesNotTouchAnotherPatient() {
        int anna = TestData.newPatientWithRound();
        int maria = TestData.newPatientWithRound();
        new MedicationService().logDose(anna, "GONAL_F", "150", "IU", LocalDate.now().toString(), "08:00", false);
        new MedicationService().logDose(maria, "GONAL_F", "150", "IU", LocalDate.now().toString(), "08:00", false);
        new NotificationService().createMedicationReminders(anna);
        new NotificationService().createMedicationReminders(maria);
        new NotificationService().markAllRead(anna);
        assertEquals(1, new NotificationService().countUnread(maria)); // Marias er stadig ulæst
    }
}
