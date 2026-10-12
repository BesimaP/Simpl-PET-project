package entities;

import enums.NotificationType;
import java.time.LocalDateTime;

// En påmindelse til patienten, som systemet laver (tabel notification, US12).
public class Notification {

    private int id;
    private int patientId;            // FK til patient.id
    private LocalDateTime dateTime;
    private NotificationType notificationType;
    private String title;
    private String message;
    private boolean read;

    // id = 0 for en ny række – databasen giver det rigtige id ved save
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

    // setId bruges af mapperen efter save. Resten kan kun læses.
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

}
