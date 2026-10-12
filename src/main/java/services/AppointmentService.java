package services;

import persistence.AppointmentMapper;
import entities.Appointment;
import entities.FertilityJourney;
import enums.AppointmentType;
import enums.EventType;
import entities.Round;
import exceptions.NoActiveRoundException;
import enums.ServiceResult;
import exceptions.NoActiveJourneyException;
import persistence.ConnectionPool;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

// Logik for aftaler (US3). Aftaler hænger på forløbet, ikke runden, fordi første konsultation sker før nogen runde.
public class AppointmentService {
    private AppointmentMapper appointmentMapper;
    private DashboardService dashboardService;
    private RoundService roundService;
    private TimelineService timelineService;

    public AppointmentService(ConnectionPool connectionPool) {
        this.appointmentMapper = new AppointmentMapper(connectionPool);
        this.dashboardService = new DashboardService(connectionPool);
        this.roundService = new RoundService(connectionPool);
        this.timelineService = new TimelineService(connectionPool);
    }

    // Gemmer en aftale på det aktive forløb. Svar: OK, INVALID_INPUT eller NO_ACTIVE_JOURNEY
    public ServiceResult addAppointment(int patientId, String type, String location, String date, String time) {
        // alle felter skal være udfyldt
        if (InputCheck.isBlank(type) || InputCheck.isBlank(location) || InputCheck.isBlank(date) || InputCheck.isBlank(time)) {
            return ServiceResult.INVALID_INPUT;
        }
        // stedet skal kunne være i kolonnen (VARCHAR(100))
        if (InputCheck.isTooLong(location, 100)) {
            return ServiceResult.INVALID_INPUT;
        }

        // ugyldig type eller dato giver INVALID_INPUT i stedet for et crash
        AppointmentType appointmentType;
        LocalDateTime dateTime;
        try {
            appointmentType = AppointmentType.valueOf(type);
            dateTime = LocalDateTime.of(LocalDate.parse(date), LocalTime.parse(time));
        } catch (IllegalArgumentException | DateTimeParseException e) {
            return ServiceResult.INVALID_INPUT;
        }

        FertilityJourney journey;
        try {
            journey = dashboardService.findActiveJourney(patientId);
        } catch (NoActiveJourneyException e) {
            return ServiceResult.NO_ACTIVE_JOURNEY;
        }

        appointmentMapper.save(new Appointment(0, journey.getId(), dateTime, appointmentType, location.trim()));

        // ægudtagning, oplægning og graviditetstest skal også på tidslinjen (US2) –
        // men kun hvis en runde er i gang, og aftalen ikke ligger før runden startede
        EventType eventType = toEventType(appointmentType);
        if (eventType != null) {
            try {
                Round round = roundService.findActiveRound(patientId);
                if (!dateTime.toLocalDate().isBefore(round.getStartDate())) {
                    timelineService.addEvent(round.getId(), dateTime, eventType, location.trim());
                }
            } catch (NoActiveJourneyException | NoActiveRoundException e) {
                // ingen runde i gang: aftalen er gemt, den kommer bare ikke på tidslinjen
            }
        }
        return ServiceResult.OK;
    }

    // hvilke aftaletyper er også et trin på tidslinjen? Resten giver null
    private EventType toEventType(AppointmentType type) {
        switch (type) {
            case EGG_RETRIEVAL:   return EventType.EGG_RETRIEVAL;
            case EMBRYO_TRANSFER: return EventType.EMBRYO_TRANSFER;
            case PREGNANCY_TEST:  return EventType.PREGNANCY_TEST;
            default:              return null;
        }
    }


    // Kommende aftaler på det aktive forløb – til "Næste aftale" på dashboardet. Intet forløb giver en tom liste
    public List<Appointment> getUpcoming(int patientId) {
        List<Appointment> upcoming = new ArrayList<>();
        for (Appointment a : getAll(patientId)) {
            if (a.getDateTime().isAfter(LocalDateTime.now())) {
                upcoming.add(a);
            }
        }
        return upcoming;
    }

    // Kommende og tidligere aftaler for et bestemt forløb (også et afsluttet). Kun patientens eget forløb
    public List<Appointment> getUpcoming(int patientId, int journeyId) {
        List<Appointment> upcoming = new ArrayList<>();
        for (Appointment a : getAllForJourney(patientId, journeyId)) {
            if (a.getDateTime().isAfter(LocalDateTime.now())) {
                upcoming.add(a);
            }
        }
        return upcoming;
    }

    public List<Appointment> getPast(int patientId, int journeyId) {
        List<Appointment> past = new ArrayList<>();
        for (Appointment a : getAllForJourney(patientId, journeyId)) {
            if (!a.getDateTime().isAfter(LocalDateTime.now())) {
                past.add(a);
            }
        }
        return past;
    }

    // aftaler på et af patientens egne forløb – tilhører det en anden, får vi en tom liste
    private List<Appointment> getAllForJourney(int patientId, int journeyId) {
        if (dashboardService.findJourney(patientId, journeyId) == null) {
            return new ArrayList<>();
        }
        return appointmentMapper.findByJourney(journeyId);
    }

    // alle aftaler på det aktive forløb, tidligste først (mapperen sorterer)
    private List<Appointment> getAll(int patientId) {
        try {
            FertilityJourney journey = dashboardService.findActiveJourney(patientId);
            return appointmentMapper.findByJourney(journey.getId());
        } catch (NoActiveJourneyException e) {
            return new ArrayList<>();
        }
    }
}
