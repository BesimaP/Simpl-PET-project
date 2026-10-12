package services;

import persistence.ConnectionPool;
import persistence.NotificationMapper;
import entities.Appointment;
import entities.MedicationLog;
import entities.Notification;
import enums.NotificationType;
import enums.ServiceResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Logik for påmindelser (US12). Påmindelser hænger direkte på patienten.
public class NotificationService {
    private NotificationMapper notificationMapper;
    private MedicationService medicationService;
    private AppointmentService appointmentService;

    public NotificationService(ConnectionPool connectionPool) {
        this.notificationMapper = new NotificationMapper(connectionPool);
        this.medicationService = new MedicationService(connectionPool);
        this.appointmentService = new AppointmentService(connectionPool);
    }

    // nyeste først
    public List<Notification> getNotifications(int patientId) {
        return notificationMapper.findByPatient(patientId);
    }

    // til den røde prik på klokken i topbaren
    public int countUnread(int patientId) {
        return notificationMapper.countUnread(patientId);
    }

    // Laver en påmindelse for hver dosis i dag, der ikke er taget. Kaldes, når dashboardet åbnes,
    // så vi springer over, hvis samme besked allerede er lavet i dag. Returnerer antal nye (bruges i tests)
    public int createMedicationReminders(int patientId) {
        Map<Integer, String> names = medicationService.getMedicationNames();
        List<Notification> existing = getNotifications(patientId);

        int created = 0;

        for (MedicationLog dose : medicationService.getTodayLogs(patientId)) {
            if (dose.isTaken()) {
                continue;
            }
            // beskeden bruges også til at finde dubletter, fx "Gonal-F · 150 IU · kl. 20:00"
            String message = names.get(dose.getMedicationId()) + " · " + dose.getDoseText() + " " + dose.getUnit()
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

    // Laver en påmindelse for hver aftale i dag eller i morgen. Beskeden indeholder aftalens dato,
    // så samme aftale kun giver én påmindelse, selvom dashboardet åbnes flere gange. Returnerer antal nye (bruges i tests)
    public int createAppointmentReminders(int patientId) {
        List<Notification> existing = getNotifications(patientId);
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        DateTimeFormatter format = DateTimeFormatter.ofPattern("d. MMM yyyy 'kl.' HH:mm", Locale.forLanguageTag("da"));

        int created = 0;

        // getUpcoming er sorteret med nærmeste først
        for (Appointment a : appointmentService.getUpcoming(patientId)) {
            if (a.getDateTime().toLocalDate().isAfter(tomorrow)) {
                break; // resten ligger endnu længere ude
            }
            // fx "Scanning · Vitanova · 8. okt. 2026 kl. 10:30" – årstallet er med, så en aftale næste år ikke ligner en dublet
            String message = a.getAppointmentType().getLabel() + " · " + a.getLocation() + " · " + a.getDateTime().format(format);

            if (alreadyExists(existing, message)) {
                continue;
            }
            notificationMapper.save(new Notification(0, patientId, LocalDateTime.now(), NotificationType.APPOINTMENT_REMINDER,
                    "Husk din aftale", message, false));
            created++;
        }
        return created;
    }

    // findes der allerede en påmindelse med denne besked (uanset dato)?
    private boolean alreadyExists(List<Notification> existing, String message) {
        for (Notification n : existing) {
            if (message.equals(n.getMessage())) {
                return true;
            }
        }
        return false;
    }

    // findes der allerede en påmindelse fra i dag med denne besked?
    private boolean alreadyExistsToday(List<Notification> existing, String message) {
        for (Notification n : existing) {
            if (n.getDateTime().toLocalDate().equals(LocalDate.now()) && message.equals(n.getMessage())) {
                return true;
            }
        }
        return false;
    }

    // knappen "Markér alle som læst" på notifikationer-siden
    public ServiceResult markAllRead(int patientId) {
        notificationMapper.markAllRead(patientId);
        return ServiceResult.OK;
    }

    // Markér en påmindelse som læst (UC7). Kun patientens egen, så man ikke kan ændre andres ved at rette id'et
    public ServiceResult markRead(int patientId, int notificationId) {
        return notificationMapper.markRead(notificationId, patientId) ? ServiceResult.OK : ServiceResult.NOT_FOUND;
    }
}
