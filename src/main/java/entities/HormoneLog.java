package entities;

import enums.HormoneType;
import java.time.LocalDateTime;

// En hormonmåling i en runde (tabel hormone_log, US9).
public class HormoneLog {

    private int id;
    private int roundId;
    private LocalDateTime dateTime;
    private HormoneType hormoneType;
    private double value;
    private String unit;

    // id = 0 for en ny række – databasen giver det rigtige id ved save
    public HormoneLog(int id, int roundId, LocalDateTime dateTime, HormoneType hormoneType, double value, String unit) {
        this.id = id;
        this.roundId = roundId;
        this.dateTime = dateTime;
        this.hormoneType = hormoneType;
        this.value = value;
        this.unit = unit;
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

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public HormoneType getHormoneType() {
        return hormoneType;
    }

    public double getValue() {
        return value;
    }

    // værdien uden unødvendige nuller: 2162.0 -> "2162", 3.5 -> "3.5"
    public String getValueText() {
        return java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    public String getUnit() {
        return unit;
    }
}
