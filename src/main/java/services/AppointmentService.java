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

// Forretningslogik for aftaler (US3). Kender IKKE Javalin – controlleren læser formularen og kalder én metode her.
// Aftaler hænger på forløbet, ikke runden (første konsultation sker før nogen runde).
// Svarer med ServiceResult: OK · INVALID_INPUT = tomt felt/ugyldig type eller dato · NO_ACTIVE_JOURNEY = intet forløb
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

    // Gemmer én aftale på patientens aktive forløb
    public ServiceResult addAppointment(int patientId, String type, String location, String date, String time) {
        // 1. regel: alle felter skal være udfyldt
        if (isBlank(type) || isBlank(location) || isBlank(date) || isBlank(time)) {
            return ServiceResult.INVALID_INPUT;
        }
        // stedet skal kunne være i kolonnen (VARCHAR(100))
        if (isTooLong(location, 100)) {
            return ServiceResult.INVALID_INPUT;
        }

        // 2. tekst fra formularen -> rigtige typer. Ugyldigt input giver INVALID_INPUT i stedet for et crash
        AppointmentType appointmentType;
        LocalDateTime dateTime;
        try {
            appointmentType = AppointmentType.valueOf(type);                             // "SCANNING" -> enum (value i dropdownen)
            dateTime = LocalDateTime.of(LocalDate.parse(date), LocalTime.parse(time)); // "2026-09-22" + "10:30" -> ét tidspunkt
        } catch (IllegalArgumentException | DateTimeParseException e) {
            return ServiceResult.INVALID_INPUT;
        }

        // 3. find det aktive forløb – en aftale hører til forløbet (ikke runden).
        //    findActiveJourney kaster, hvis der ikke er et – vi fanger og oversætter til et ServiceResult
        FertilityJourney journey;
        try {
            journey = dashboardService.findActiveJourney(patientId);
        } catch (NoActiveJourneyException e) {
            return ServiceResult.NO_ACTIVE_JOURNEY;
        }

        // 4. byg kortet (id 0 = databasen giver et) og læg det i skuffen appointment
        appointmentMapper.save(new Appointment(0, journey.getId(), dateTime, appointmentType, location));

        // 5. de store trin (ægudtagning, oplægning, graviditetstest) skal også på tidslinjen (US2) – men kun hvis en runde er i gang
        //    og aftalen ikke ligger før runden startede (ellers hører den ikke til denne runde)
        EventType eventType = toEventType(appointmentType);
        if (eventType != null) {
            try {
                Round round = roundService.findActiveRound(patientId);
                if (!dateTime.toLocalDate().isBefore(round.getStartDate())) {
                    timelineService.addEvent(round.getId(), dateTime, eventType, location);
                }
            } catch (NoActiveJourneyException | NoActiveRoundException e) {
                // ingen runde i gang = aftalen gemmes, men kommer ikke på tidslinjen. Ikke en fejl
            }
        }
        return ServiceResult.OK;
    }

    // hjælper: hvilke aftaletyper er også et trin på tidslinjen? Resten (konsultation, scanning, blodprøve) giver null
    private EventType toEventType(AppointmentType type) {
        switch (type) {
            case EGG_RETRIEVAL:   return EventType.EGG_RETRIEVAL;
            case EMBRYO_TRANSFER: return EventType.EMBRYO_TRANSFER;
            case PREGNANCY_TEST:  return EventType.PREGNANCY_TEST;
            default:              return null;
        }
    }


    // Henter aftaler på patientens aktive forløb, delt i kommende (fra nu og frem) og tidligere.
    // Intet forløb -> tom liste
    public List<Appointment> getUpcoming(int patientId) {
        List<Appointment> upcoming = new ArrayList<>();
        for (Appointment a : getAll(patientId)) {
            if (a.getDateTime().isAfter(LocalDateTime.now())) {   // ligger efter nu = kommende
                upcoming.add(a);
            }
        }
        return upcoming;
    }

    public List<Appointment> getPast(int patientId) {
        List<Appointment> past = new ArrayList<>();
        for (Appointment a : getAll(patientId)) {
            if (!a.getDateTime().isAfter(LocalDateTime.now())) {  // ikke efter nu = tidligere
                past.add(a);
            }
        }
        return past;
    }

    // Samme to lister, men for ét bestemt forløb (også et afsluttet) – kun patientens eget, ellers tomme lister
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

    // hjælper: aftaler på ét af patientens egne forløb. Tilhører forløbet en anden -> tom liste
    private List<Appointment> getAllForJourney(int patientId, int journeyId) {
        if (dashboardService.findJourney(patientId, journeyId) == null) {
            return new ArrayList<>();
        }
        return appointmentMapper.findByJourney(journeyId);
    }

    // hjælper: alle aftaler på forløbet, tidligste først (mapperen sorterer). Bruges af de to ovenfor
    private List<Appointment> getAll(int patientId) {
        try {
            FertilityJourney journey = dashboardService.findActiveJourney(patientId);
            return appointmentMapper.findByJourney(journey.getId());
        } catch (NoActiveJourneyException e) {
            return new ArrayList<>();
        }
    }

    // lille hjælper: null eller kun mellemrum tæller som tomt
    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    // lille hjælper: længere end kolonnen i databasen (VARCHAR(max))? Så ville INSERT fejle med en 500-fejl
    private boolean isTooLong(String s, int max) {
        return s != null && s.length() > max;
    }
}
