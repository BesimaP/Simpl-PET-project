package entities;

import java.time.LocalDateTime;

// En planlagt dosis af en Medication i en runde, og om den er taget (tabel medication_log, US8).
public class MedicationLog {

    private int id;
    private int roundId;               // FK til round.id
    private int medicationId;          // FK til medication.id
    private LocalDateTime scheduledDateTime;
    private double dose;
    private String unit;               // hentes fra medication-tabellen (JOIN) – gemmes ikke på loggen
    private boolean taken;

    // konstruktør: id = 0 når kortet er nyt (databasen giver det rigtige id ved save)
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

    // gettere: læs felterne. setId bruges af mapperen efter save; resten kan ikke ændres udefra
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

    // dosis som pæn tekst: 150.0 -> "150", 0.25 -> "0.25" (Thymeleaf: ${m.doseText}).
    // stripTrailingZeros fjerner nullerne efter kommaet, toPlainString undgår "1.5E+2"
    public String getDoseText() {
        return java.math.BigDecimal.valueOf(dose).stripTrailingZeros().toPlainString();
    }

    public String getUnit() {
        return unit;
    }

    public boolean isTaken() {
        return taken;
    }

    // Markér som taget (US8 AC3)
    public void markTaken() {
        this.taken = true;
    }
}
