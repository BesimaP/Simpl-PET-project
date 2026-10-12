package entities;

import java.time.LocalDateTime;

// En planlagt dosis i en runde, og om den er taget (tabel medication_log, US8).
public class MedicationLog {

    private int id;
    private int roundId;               // FK til round.id
    private int medicationId;          // FK til medication.id
    private LocalDateTime scheduledDateTime;
    private double dose;
    private String unit;               // kommer fra medication via JOIN – gemmes ikke på loggen
    private boolean taken;

    // id = 0 for en ny række – databasen giver det rigtige id ved save
    public MedicationLog(int id, int roundId, int medicationId, LocalDateTime scheduledDateTime,
                         double dose, String unit, boolean taken) {
        this.id = id;
        this.roundId = roundId;
        this.medicationId = medicationId;
        this.scheduledDateTime = scheduledDateTime;
        this.dose = dose;
        this.unit = unit;
        this.taken = taken;
    }

    // setId bruges af mapperen efter save. Resten kan kun læses.
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRoundId() {
        return roundId;
    }

    public int getMedicationId() {
        return medicationId;
    }

    public LocalDateTime getScheduledDateTime() {
        return scheduledDateTime;
    }

    public double getDose() {
        return dose;
    }

    // dosis uden unødvendige nuller: 150.0 -> "150", 0.25 -> "0.25". toPlainString undgår "1.5E+2"
    public String getDoseText() {
        return java.math.BigDecimal.valueOf(dose).stripTrailingZeros().toPlainString();
    }

    public String getUnit() {
        return unit;
    }

    public boolean isTaken() {
        return taken;
    }

}
