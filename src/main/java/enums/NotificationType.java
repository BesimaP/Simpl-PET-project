package enums;

// Typer af påmindelser (US12). NotificationService laver dem, når dashboardet åbnes:
// medicin for dagens doser og aftaler for i dag og i morgen.
public enum NotificationType {
    MEDICATION_REMINDER,
    APPOINTMENT_REMINDER
}
