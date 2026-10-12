package entities;

import enums.HormoneType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Unittest af HormoneCurve – ren Java, ingen database.
class HormoneCurveTest {

    private HormoneCurve curve;

    // ny kurve før hver test, så testene ikke påvirker hinanden
    @BeforeEach
    void setUp() {
        curve = new HormoneCurve(HormoneType.FSH, "IU/L", 450.0);
    }

    @Test
    void polylineWithTwoPoints() {
        curve.addPoint(new HormoneCurve.Point(10.0, 20.0, 5.5, "01/09"));
        curve.addPoint(new HormoneCurve.Point(30.0, 40.0, 7.0, "03/09"));

        // SVG's <polyline points> skal have formatet "x1,y1 x2,y2"
        assertEquals("10.0,20.0 30.0,40.0", curve.getPolyline());
    }

    @Test
    void polylineWithoutPointsIsEmpty() {
        assertEquals("", curve.getPolyline());
    }

    @Test
    void maxTextRemovesTrailingZeros() {
        assertEquals("450", curve.getMaxText());   // uden ".0"
    }

    @Test
    void valueTextKeepsDecimals() {
        HormoneCurve.Point point = new HormoneCurve.Point(0, 0, 5.5, "01/09");
        assertEquals("5.5", point.getValueText());
    }
}
