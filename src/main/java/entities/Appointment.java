package entities;

import enums.AppointmentType;
import java.time.LocalDateTime;

// En aftale (tabel appointment, US3). Den ligger på forløbet, fordi første konsultation sker før nogen runde.
public class Appointment {

    private int id;
    private int fertilityJourneyId;   // FK til fertility_journey.id
    private LocalDateTime dateTime;
    private AppointmentType appointmentType;
    private String location;

    // id = 0 for en ny række – databasen giver det rigtige id ved save
    public Appointment(int id, int fertilityJourneyId, LocalDateTime dateTime, AppointmentType appointmentType, String location) {
        this.id = id;
        this.fertilityJourneyId = fertilityJourneyId;
        this.dateTime = dateTime;
        this.appointmentType = appointmentType;
        this.location = location;
    }

    // setId bruges af mapperen efter save. Resten kan kun læses.
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFertilityJourneyId() {
        return fertilityJourneyId;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public AppointmentType getAppointmentType() {
        return appointmentType;
    }

    public String getLocation() {
        return location;
    }
}
