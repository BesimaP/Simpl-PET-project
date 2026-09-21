package service;

import dao.AppointmentDAO;
import dao.DatabaseConnection;
import dao.FertilityJourneyDAO;
import entities.Appointment;
import entities.FertilityJourney;
import enums.AppointmentType;
import enums.ServiceResult;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

// Forretningslogik for aftaler (US3). Kender IKKE Javalin. Aftaler hænger på forløbet, ikke runden.
public class AppointmentService {

    public ServiceResult addAppointment(int patientId, String type, String location, String date, String time){
        // 1. regel: alle felter skal være udfyldt
        if(isBlank(type)|| isBlank(location) || isBlank(date) || isBlank(time)){
            return ServiceResult.INVALID_INPUT;
        }

        // 2. tekst fra formularen -> rigtige typer. Ugyldigt input giver INVALID_INPUT i stedet for et crash
        AppointmentType appointmentType;
        LocalDateTime dateTime;
        try{
            appointmentType = AppointmentType.valueOf(type);
            dateTime = LocalDateTime.of(LocalDate.parse(date), LocalTime.parse(time));
        }catch (IllegalArgumentException | DateTimeParseException e){
            return ServiceResult.INVALID_INPUT;
        }

        // 3. find det aktive forløb – en aftale hører til forløbet (ikke runden)
        FertilityJourney journey = new FertilityJourneyDAO(DatabaseConnection.getConnection()).findActiveByPatient(patientId);
        if(journey == null){
            return ServiceResult.NO_ACTIVE_JOURNEY;
        }

        // 4. gem
        new AppointmentDAO(DatabaseConnection.getConnection()).save(new Appointment(0, journey.getId(), dateTime, appointmentType, location));
        return ServiceResult.OK;


    }
    private boolean isBlank(String s){
        return s == null || s.isBlank();
    }
}
