package services;

import enums.ServiceResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Unit tests af HormoneService (hormonlog, US9)
class HormoneServiceTest {

    @BeforeAll
    static void setup() {
        TestData.freshDatabase();
    }

    @Test
    void saveLogWithBlankFieldReturnsInvalidInput() {
        int patientId = TestData.newPatientWithRound();
        assertEquals(ServiceResult.INVALID_INPUT, new HormoneService().saveLog(patientId, "FSH", "", "IU/L", "2026-09-12"));
    }

    @Test
    void saveLogWithoutJourneyReturnsNoActiveJourney() {
        int patientId = TestData.newPatient();
        assertEquals(ServiceResult.NO_ACTIVE_JOURNEY, new HormoneService().saveLog(patientId, "FSH", "7.5", "IU/L", "2026-09-12"));
    }

    @Test
    void saveLogWithoutRoundReturnsNoActiveRound() {
        int patientId = TestData.newPatientWithJourney();
        assertEquals(ServiceResult.NO_ACTIVE_ROUND, new HormoneService().saveLog(patientId, "FSH", "7.5", "IU/L", "2026-09-12"));
    }

    @Test
    void saveLogWithTextAsValueReturnsInvalidInput() {
        int patientId = TestData.newPatientWithRound();
        // "abc" kan ikke blive til et tal
        assertEquals(ServiceResult.INVALID_INPUT, new HormoneService().saveLog(patientId, "FSH", "abc", "IU/L", "2026-09-12"));
    }

    @Test
    void saveLogWithUnknownHormoneReturnsInvalidInput() {
        int patientId = TestData.newPatientWithRound();
        assertEquals(ServiceResult.INVALID_INPUT, new HormoneService().saveLog(patientId, "KAFFE", "7.5", "IU/L", "2026-09-12"));
    }

    @Test
    void saveLogReturnsOk() {
        int patientId = TestData.newPatientWithRound();
        assertEquals(ServiceResult.OK, new HormoneService().saveLog(patientId, "E2_OESTRADIOL", "450", "pmol/L", "2026-09-12"));
    }
}
