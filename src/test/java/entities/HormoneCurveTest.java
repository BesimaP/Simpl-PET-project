package entities;

import enums.HormoneType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// UNITTEST af HormoneCurve: tester ren Java-logik – INGEN database, ingen mapper, ingen ConnectionPool.
// Derfor er der ingen @BeforeAll (der er ikke noget dyrt at sætte op) og testene kører på få millisekunder.
class HormoneCurveTest {

    private HormoneCurve curve;

    // Køres før HVER test: et nyt, tomt kurve-objekt, så testene ikke påvirker hinanden
    @BeforeEach
    void setUp() {
        curve = new HormoneCurve(HormoneType.FSH, "IU/L", 450.0);
    }

    @Test
    void polylineWithTwoPoints() {
        curve.addPoint(new HormoneCurve.Point(10.0, 20.0, 5.5, "01/09"));
        curve.addPoint(new HormoneCurve.Point(30.0, 40.0, 7.0, "03/09"));

        // <polyline points="…"> i SVG skal have "x1,y1 x2,y2"
        assertEquals("10.0,20.0 30.0,40.0", curve.getPolyline());
    }

    @Test
    void polylineWithoutPointsIsEmpty() {
        assertEquals("", curve.getPolyline());
    }

    @Test
    void maxTextRemovesTrailingZeros() {
        assertEquals("450", curve.getMaxText());   // 450.0 skal vises som "450"
    }

    @Test
    void valueTextKeepsDecimals() {
        HormoneCurve.Point point = new HormoneCurve.Point(0, 0, 5.5, "01/09");
        assertEquals("5.5", point.getValueText());
    }
}
