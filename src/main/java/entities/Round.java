package entities;

import enums.Result;
import enums.RoundStatus;
import enums.TreatmentType;
import java.time.LocalDate;

// Et behandlingsforsøg i et forløb (tabel round, US10a/10b).
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

    // id = 0 for en ny række – databasen giver det rigtige id ved save
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

}
