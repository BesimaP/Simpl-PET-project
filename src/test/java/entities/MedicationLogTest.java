package entities;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

// Unittest af getDoseText(), som formaterer dosis til medicin-siden.
class MedicationLogTest {

    private MedicationLog logWithDose(double dose) {
        return new MedicationLog(1, 1, 1, LocalDateTime.of(2026, 9, 2, 20, 0), dose, "IU", false);
    }

    @Test
    void doseTextRemovesTrailingZeros() {
        assertEquals("150", logWithDose(150.0).getDoseText());
    }

    @Test
    void doseTextKeepsSmallDecimals() {
        assertEquals("0.25", logWithDose(0.25).getDoseText());     // fx Orgalutran 0,25 mg
    }

    @Test
    void doseTextIsNotScientific() {
        // uden toPlainString skriver BigDecimal 5000 som "5E+3"
        assertEquals("5000", logWithDose(5000.0).getDoseText());   // fx Pregnyl 5000 IE
    }
}
