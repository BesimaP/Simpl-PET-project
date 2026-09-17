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

        public HormoneLog(int id, int roundId, LocalDateTime dateTime, HormoneType hormoneType, double value, String unit) {
            this.id = id;
            this.roundId = roundId;
            this.dateTime = dateTime;
            this.hormoneType = hormoneType;
            this.value = value;
            this.unit = unit;
        }

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

        public String getUnit() {
            return unit;
        }
    }