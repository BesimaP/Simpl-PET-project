package entities;

import enums.HormoneType;

import java.util.ArrayList;
import java.util.List;

// Kurven på hormoner-siden (US9). Ikke en tabel – bare de punkter, SVG'en skal tegne.
// HormoneService regner koordinaterne ud, så skabelonen ikke skal regne.
public class HormoneCurve {

    // Et punkt på kurven: x/y i SVG-koordinater plus tekst til hover og akse
    public static class Point {
        private double x;
        private double y;
        private double value;
        private String dateLabel;

        public Point(double x, double y, double value, String dateLabel) {
            this.x = x;
            this.y = y;
            this.value = value;
            this.dateLabel = dateLabel;
        }

        public double getX() { return x; }
        public double getY() { return y; }
        public double getValue() { return value; }
        public String getValueText() { return java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString(); }   // 450.0 -> "450"
        public String getDateLabel() { return dateLabel; }
    }

    private HormoneType type;
    private String unit;
    private double max;
    private double average;    // gennemsnit af værdierne
    private double averageY;   // gennemsnittet som y-koordinat (til den stiplede linje)
    private int skipped;       // målinger i en anden enhed – de kan ikke tegnes på samme akse
    private List<Point> points = new ArrayList<>();

    public HormoneCurve(HormoneType type, String unit, double max) {
        this.type = type;
        this.unit = unit;
        this.max = max;
    }

    public void addPoint(Point p) {
        points.add(p);
    }

    public HormoneType getType() { return type; }
    public String getUnit() { return unit; }
    public String getMaxText() { return java.math.BigDecimal.valueOf(max).stripTrailingZeros().toPlainString(); }   // 450.0 -> "450"
    public int getSkipped() { return skipped; }
    public void setSkipped(int skipped) { this.skipped = skipped; }
    public String getAverageText() { return java.math.BigDecimal.valueOf(average).stripTrailingZeros().toPlainString(); }   // 535.0 -> "535"
    public double getAverageY() { return averageY; }

    public void setAverage(double average, double averageY) {
        this.average = average;
        this.averageY = averageY;
    }
    public List<Point> getPoints() { return points; }

    // "x1,y1 x2,y2 …" – formatet som <polyline points="…"> skal have
    public String getPolyline() {
        String s = "";
        for (Point p : points) {
            s += p.getX() + "," + p.getY() + " ";
        }
        return s.trim();
    }
}
