package entities;

import enums.NotificationType;
import java.time.LocalDateTime;

// En påmindelse til patienten, genereret af systemet (tabel notification, US12).
public class Notification {

    private int id;
    private int patientId;            // FK til patient.id
    private LocalDateTime dateTime;
    private NotificationType notificationType;
    private String title;
    private String message;
    private boolean read;

    // konstruktør: id = 0 når kortet er nyt (databasen giver det rigtige id ved save)
    public Notification(int id, int patientId, LocalDateTime dateTime, NotificationType notificationType,
                        String title, String message, boolean read) {
        this.id = id;
        this.patientId = patientId;
        this.dateTime = dateTime;
        this.notificationType = notificationType;
        this.title = title;
        this.message = message;
        this.read = read;
    }

    // gettere: læs felterne. setId bruges af DAO'en efter save; resten kan ikke ændres udefra
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPatientId() {
        return patientId;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    // Markér som læst (UC7)
    public void markRead() {
        this.read = true;
    }
}
