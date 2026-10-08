package services;

import persistence.NotificationMapper;
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
        NotificationService service = new NotificationService(TestData.pool());
        assertTrue(service.getNotifications(patientId).isEmpty());
        assertEquals(0, service.countUnread(patientId));
    }

    @Test
    void markReadLowersUnreadCount() {
        int patientId = TestData.newPatient();
        int id = new NotificationMapper(TestData.pool()).save(
                new Notification(0, patientId, LocalDateTime.now(), NotificationType.MEDICATION_REMINDER, "Gonal-F", "Husk 150 IU kl. 08", false));
        NotificationService service = new NotificationService(TestData.pool());
        assertEquals(1, service.countUnread(patientId));
        assertEquals(ServiceResult.OK, service.markRead(patientId, id));
        assertEquals(0, service.countUnread(patientId));
        assertTrue(service.getNotifications(patientId).get(0).isRead());
    }

    @Test
    void reminderIsCreatedForPlannedDoseToday() {
        int patientId = TestData.newPatientWithRound();
        new MedicationService(TestData.pool()).logDose(patientId, "GONAL_F", "150", LocalDate.now().toString(), "20:00", false);
        assertEquals(1, new NotificationService(TestData.pool()).createMedicationReminders(patientId));
        assertEquals("Gonal-F · 150 IU · kl. 20:00", new NotificationService(TestData.pool()).getNotifications(patientId).get(0).getMessage());
        assertEquals(NotificationType.MEDICATION_REMINDER, new NotificationService(TestData.pool()).getNotifications(patientId).get(0).getNotificationType());
    }

    @Test
    void noReminderForDoseAlreadyTaken() {
        int patientId = TestData.newPatientWithRound();
        new MedicationService(TestData.pool()).logDose(patientId, "GONAL_F", "150", LocalDate.now().toString(), "08:00", true);
        assertEquals(0, new NotificationService(TestData.pool()).createMedicationReminders(patientId));
    }

    @Test
    void noReminderForDoseTomorrow() {
        int patientId = TestData.newPatientWithRound();
        new MedicationService(TestData.pool()).logDose(patientId, "GONAL_F", "150", LocalDate.now().plusDays(1).toString(), "08:00", false);
        assertEquals(0, new NotificationService(TestData.pool()).createMedicationReminders(patientId));
    }

    @Test
    void sameReminderIsNotCreatedTwice() {
        int patientId = TestData.newPatientWithRound();
        new MedicationService(TestData.pool()).logDose(patientId, "MENOPUR", "75", LocalDate.now().toString(), "20:00", false);
        // dashboard åbnes to gange -> stadig kun én påmindelse
        assertEquals(1, new NotificationService(TestData.pool()).createMedicationReminders(patientId));
        assertEquals(0, new NotificationService(TestData.pool()).createMedicationReminders(patientId));
        assertEquals(1, new NotificationService(TestData.pool()).getNotifications(patientId).size());
    }

    @Test
    void reminderWithoutRoundCreatesNothing() {
        int patientId = TestData.newPatientWithJourney();
        assertEquals(0, new NotificationService(TestData.pool()).createMedicationReminders(patientId));
    }

    @Test
    void markAllReadClearsUnreadCount() {
        int patientId = TestData.newPatientWithRound();
        new MedicationService(TestData.pool()).logDose(patientId, "GONAL_F", "150", LocalDate.now().toString(), "08:00", false);
        new MedicationService(TestData.pool()).logDose(patientId, "MENOPUR", "75", LocalDate.now().toString(), "20:00", false);
        assertEquals(2, new NotificationService(TestData.pool()).createMedicationReminders(patientId));
        assertEquals(ServiceResult.OK, new NotificationService(TestData.pool()).markAllRead(patientId));
        assertEquals(0, new NotificationService(TestData.pool()).countUnread(patientId));
    }

    @Test
    void markAllReadDoesNotTouchAnotherPatient() {
        int anna = TestData.newPatientWithRound();
        int maria = TestData.newPatientWithRound();
        new MedicationService(TestData.pool()).logDose(anna, "GONAL_F", "150", LocalDate.now().toString(), "08:00", false);
        new MedicationService(TestData.pool()).logDose(maria, "GONAL_F", "150", LocalDate.now().toString(), "08:00", false);
        new NotificationService(TestData.pool()).createMedicationReminders(anna);
        new NotificationService(TestData.pool()).createMedicationReminders(maria);
        new NotificationService(TestData.pool()).markAllRead(anna);
        assertEquals(1, new NotificationService(TestData.pool()).countUnread(maria)); // Marias er stadig ulæst
    }

    @Test
    void appointmentReminderIsCreatedForAppointmentTomorrow() {
        int patientId = TestData.newPatientWithJourney();
        String tomorrow = LocalDate.now().plusDays(1).toString();
        new AppointmentService(TestData.pool()).addAppointment(patientId, "SCANNING", "Vitanova", tomorrow, "10:30");
        NotificationService service = new NotificationService(TestData.pool());
        assertEquals(1, service.createAppointmentReminders(patientId));
        assertEquals(NotificationType.APPOINTMENT_REMINDER, service.getNotifications(patientId).get(0).getNotificationType());
        // samme aftale giver ikke en ny påmindelse, når dashboard åbnes igen
        assertEquals(0, service.createAppointmentReminders(patientId));
    }

    @Test
    void noAppointmentReminderForAppointmentNextWeek() {
        int patientId = TestData.newPatientWithJourney();
        String nextWeek = LocalDate.now().plusDays(7).toString();
        new AppointmentService(TestData.pool()).addAppointment(patientId, "SCANNING", "Vitanova", nextWeek, "10:30");
        assertEquals(0, new NotificationService(TestData.pool()).createAppointmentReminders(patientId));
    }
}
