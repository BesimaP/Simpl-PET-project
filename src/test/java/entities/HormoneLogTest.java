package entities;

import enums.HormoneType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

// UNITTEST af HormoneLog: tester getValueText(), som laver tallet om til pæn tekst på hormon-siden. Ingen database.
class HormoneLogTest {

    private HormoneLog logWithValue(double value) {
        return new HormoneLog(1, 1, LocalDateTime.of(2026, 9, 3, 8, 0), HormoneType.FSH, value, "IU/L");
    }

    @Test
    void valueTextRemovesTrailingZeros() {
        assertEquals("8", logWithValue(8.0).getValueText());       // 8.0 -> "8"
    }

    @Test
    void valueTextKeepsDecimals() {
        assertEquals("8.5", logWithValue(8.5).getValueText());
    }

    @Test
    void valueTextForLargeValueIsNotScientific() {
        // toPlainString: 1200 skal ikke blive til "1.2E+3"
        assertEquals("1200", logWithValue(1200.0).getValueText());
    }

    @Test
    void valueTextForZero() {
        assertEquals("0", logWithValue(0.0).getValueText());       // grænseværdi: 0 er tilladt i databasen
    }
}
