package entities;

import enums.JourneyStatus;
import java.time.LocalDate;

// Patientens forløb (tabel fertility_journey, US1). De enkelte behandlingsforsøg ligger i Round.
public class FertilityJourney {

    private int id;
    private int patientId;       // FK til patient.id
    private LocalDate startDate;
    private JourneyStatus status;

    // id = 0 for en ny række – databasen giver det rigtige id ved save
    public FertilityJourney(int id, int patientId, LocalDate startDate, JourneyStatus status) {
        this.id = id;
        this.patientId = patientId;
        this.startDate = startDate;
        this.status = status;
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public JourneyStatus getStatus() {
        return status;
    }

}
