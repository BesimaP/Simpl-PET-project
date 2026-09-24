package entities;

import enums.Result;
import enums.RoundStatus;
import enums.TreatmentType;
import java.time.LocalDate;

// Ét behandlingsforsøg i et forløb (tabel round, US10a/10b). Ny i v2.
// endDate og result er null, indtil runden afsluttes (UC14).
public class Round {

    private int id;
    private int fertilityJourneyId;   // FK til fertility_journey.id
    private int roundNumber;
    private TreatmentType treatmentType;
    private LocalDate startDate;
    private LocalDate endDate;        // null indtil afsluttet
    private RoundStatus status;
    private Result result;            // null indtil afsluttet

    // konstruktør: id = 0 når kortet er nyt (databasen giver det rigtige id ved save)
    public Round(int id, int fertilityJourneyId, int roundNumber, TreatmentType treatmentType,
                 LocalDate startDate, LocalDate endDate, RoundStatus status, Result result) {
        this.id = id;
        this.fertilityJourneyId = fertilityJourneyId;
        this.roundNumber = roundNumber;
        this.treatmentType = treatmentType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.result = result;
    }

    // gettere: læs felterne. setId bruges af DAO'en efter save; resten kan ikke ændres udefra
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFertilityJourneyId() {
        return fertilityJourneyId;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public TreatmentType getTreatmentType() {
        return treatmentType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public RoundStatus getStatus() {
        return status;
    }

    public Result getResult() {
        return result;
    }

    // Afslut runde (UC14): sætter alle tre felter på én gang, så objektet aldrig er halvt afsluttet
    public void endRound(Result result, LocalDate endDate) {
        this.result = result;
        this.endDate = endDate;
        this.status = RoundStatus.COMPLETED;
    }
}
