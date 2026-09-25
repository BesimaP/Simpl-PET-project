package entities;

import enums.HormoneType;

import java.util.ArrayList;
import java.util.List;

// Kurven på hormoner-siden (US9). IKKE en tabel i databasen – bare et "kort" med de punkter, SVG'en skal tegne.
// HormoneService regner målingerne om til koordinater, så skabelonen kun skal tegne, ikke regne.
public class HormoneCurve {

    // Ét punkt på kurven: x/y i SVG-koordinater + tekst til hover/akse
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
        public String getDateLabel() { return dateLabel; }
    }

    private HormoneType type;
    private String unit;
    private double max;
    private double average;    // gennemsnit af værdierne
    private double averageY;   // gennemsnittet som y-koordinat (til den stiplede linje)
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
    public double getMax() { return max; }
    public double getAverage() { return average; }
    public double getAverageY() { return averageY; }

    public void setAverage(double average, double averageY) {
        this.average = average;
        this.averageY = averageY;
    }
    public List<Point> getPoints() { return points; }

    // "x1,y1 x2,y2 …" – præcis den tekst <polyline points="…"> i SVG skal have
    public String getPolyline() {
        String s = "";
        for (Point p : points) {
            s += p.getX() + "," + p.getY() + " ";
        }
        return s.trim();
    }
}
