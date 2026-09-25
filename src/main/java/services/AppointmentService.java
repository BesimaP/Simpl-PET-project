package services;

import dao.AppointmentDAO;
import dao.DatabaseConnection;
import entities.Appointment;
import entities.FertilityJourney;
import enums.AppointmentType;
import enums.ServiceResult;
import exceptions.NoActiveJourneyException;
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

    // Gemmer én aftale på patientens aktive forløb
    public ServiceResult addAppointment(int patientId, String type, String location, String date, String time) {
        // 1. regel: alle felter skal være udfyldt
        if (isBlank(type) || isBlank(location) || isBlank(date) || isBlank(time)) {
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
            journey = new DashboardService().findActiveJourney(patientId);
        } catch (NoActiveJourneyException e) {
            return ServiceResult.NO_ACTIVE_JOURNEY;
        }

        // 4. byg kortet (id 0 = databasen giver et) og læg det i skuffen appointment
        new AppointmentDAO(DatabaseConnection.getConnection()).save(new Appointment(0, journey.getId(), dateTime, appointmentType, location));
        return ServiceResult.OK;
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

    // hjælper: alle aftaler på forløbet, tidligste først (DAO'en sorterer). Bruges af de to ovenfor
    private List<Appointment> getAll(int patientId) {
        try {
            FertilityJourney journey = new DashboardService().findActiveJourney(patientId);
            return new AppointmentDAO(DatabaseConnection.getConnection()).findByJourney(journey.getId());
        } catch (NoActiveJourneyException e) {
            return new ArrayList<>();
        }
    }

    // lille hjælper: null eller kun mellemrum tæller som tomt
    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
