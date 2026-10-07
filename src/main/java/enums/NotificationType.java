package enums;

// Notifikationstyper (ordlisten). Begge genereres af NotificationService, når dashboard åbnes (US12):
// MEDICATION_REMINDER for dagens doser, APPOINTMENT_REMINDER for aftaler i dag og i morgen.
public enum NotificationType {
    MEDICATION_REMINDER,
    APPOINTMENT_REMINDER
}
