package services;

import persistence.ConnectionPool;
import persistence.NotificationMapper;
import entities.MedicationLog;
import entities.Notification;
import enums.NotificationType;
import enums.ServiceResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

// Forretningslogik for påmindelser (US12). Kender IKKE Javalin.
// Påmindelser hænger direkte på patienten – ingen forløb/runde at slå op.
public class NotificationService {
    private NotificationMapper notificationMapper; // arkivaren for påmindelser
    private MedicationService medicationService;   // til at finde dagens doser (påmindelser om medicin)

    public NotificationService(ConnectionPool connectionPool) {
        this.notificationMapper = new NotificationMapper(connectionPool);
        this.medicationService = new MedicationService(connectionPool);
    }

    // Henter alle patientens påmindelser, nyeste først – til notifikationer.html
    public List<Notification> getNotifications(int patientId) {
        return notificationMapper.findByPatient(patientId);
    }

    // Tæller ulæste – til den røde prik på klokken i topbaren
    public int countUnread(int patientId) {
        return notificationMapper.countUnread(patientId);
    }

    // Opretter én MEDICATION_REMINDER per planlagt (ikke taget) dosis i dag (US12). Kaldes når dashboard åbnes.
    // Samme påmindelse oprettes ikke to gange: hvis der allerede findes en fra i dag med samme besked, springes den over.
    // Returnerer antal nye påmindelser – praktisk i tests
    public int createMedicationReminders(int patientId) {
        Map<Integer, String> names = medicationService.getMedicationNames();   // id -> "Gonal-F"
        List<Notification> existing = getNotifications(patientId);

        int created = 0;

        for (MedicationLog dose : medicationService.getTodayLogs(patientId)) {
            if (dose.isTaken()) {
                continue; // allerede taget = ingen grund til at minde om den
            }
            // beskeden er også "nøglen" til at undgå dubletter, fx "Gonal-F · 150.0 IU · kl. 20:00"
            String message = names.get(dose.getMedicationId()) + " · " + dose.getDose() + " " + dose.getUnit()
                    + " · kl. " + dose.getScheduledDateTime().format(DateTimeFormatter.ofPattern("HH:mm"));

            if (alreadyExistsToday(existing, message)) {
                continue;
            }
            notificationMapper.save(new Notification(0, patientId, LocalDateTime.now(), NotificationType.MEDICATION_REMINDER,
                    "Husk din medicin", message, false));

            created++;
        }
        return created;
    }

    // hjælper: findes der allerede en påmindelse fra i dag med præcis denne besked?
    private boolean alreadyExistsToday(List<Notification> existing, String message) {
        for (Notification n : existing) {
            if (n.getDateTime().toLocalDate().equals(LocalDate.now()) && message.equals(n.getMessage())) {
                return true;
            }
        }
        return false;
    }

    // Markér alle patientens påmindelser som læst – knappen øverst på notifikationer-siden
    public ServiceResult markAllRead(int patientId) {
        notificationMapper.markAllRead(patientId);
        return ServiceResult.OK;
    }

    // Markér én påmindelse som læst (UC7) – kun patientens egen (id'et skal findes i hendes liste),
    // så man ikke kan ændre andres ved at rette id'et i det skjulte felt. Svar: OK · NOT_FOUND
    public ServiceResult markRead(int patientId, int notificationId) {
        for (Notification n : getNotifications(patientId)) {
            if (n.getId() == notificationId) {
                notificationMapper.markRead(notificationId);
                return ServiceResult.OK;
            }
        }
        return ServiceResult.NOT_FOUND;
    }
}
