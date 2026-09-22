package entities;

import enums.JourneyStatus;
import java.time.LocalDate;

// Patientens overordnede forløb (tabel fertility_journey, US1).
// Indeholder ikke længere rundedata — det ligger i Round (v2).
public class FertilityJourney {

    private int id;
    private int patientId;       // FK til patient.id
    private LocalDate startDate;
    private JourneyStatus status;

    // konstruktør: id = 0 når kortet er nyt (databasen giver det rigtige id ved save)
    public FertilityJourney(int id, int patientId, LocalDate startDate, JourneyStatus status) {
        this.id = id;
        this.patientId = patientId;
        this.startDate = startDate;
        this.status = status;
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public JourneyStatus getStatus() {
        return status;
    }

    public void setStatus(JourneyStatus status) {
        this.status = status;
    }
}
