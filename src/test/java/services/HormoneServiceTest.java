package services;

import enums.ServiceResult;
import entities.HormoneCurve;
import enums.HormoneType;
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

    @Test
    void getLogsWithoutRoundReturnsEmptyList() {
        int patientId = TestData.newPatientWithJourney();
        assertTrue(new HormoneService().getLogs(patientId).isEmpty());
    }

    @Test
    void savedLogKeepsValueAndType() {
        int patientId = TestData.newPatientWithRound();
        new HormoneService().saveLog(patientId, "E2_OESTRADIOL", "450", "pmol/L", "2026-09-12");
        assertEquals(1, new HormoneService().getLogs(patientId).size());
        assertEquals(450.0, new HormoneService().getLogs(patientId).get(0).getValue());
        assertEquals(HormoneType.E2_OESTRADIOL, new HormoneService().getLogs(patientId).get(0).getHormoneType());
    }

    @Test
    void hormoneTypeLabelIsDanish() {
        // labels bruges i Thymeleaf: ${h.hormoneType.label}
        assertEquals("Østradiol", HormoneType.E2_OESTRADIOL.getLabel());
        assertEquals("Progesteron", HormoneType.PROGESTERONE.getLabel());
    }

    @Test
    void getCurveWithoutLogsIsEmptyNotNull() {
        int patientId = TestData.newPatientWithRound();
        HormoneCurve curve = new HormoneService().getCurve(patientId, null);
        assertNotNull(curve);
        assertTrue(curve.getPoints().isEmpty());
    }

    @Test
    void getCurveUsesNewestHormoneWhenNoneChosen() {
        int patientId = TestData.newPatientWithRound();
        new HormoneService().saveLog(patientId, "LH", "5", "IU/L", "2026-09-11");
        new HormoneService().saveLog(patientId, "E2_OESTRADIOL", "450", "pmol/L", "2026-09-12");
        assertEquals(HormoneType.E2_OESTRADIOL, new HormoneService().getCurve(patientId, null).getType());
    }

    @Test
    void getCurveHasOldestFirstAndMaxAtTop() {
        int patientId = TestData.newPatientWithRound();
        new HormoneService().saveLog(patientId, "E2_OESTRADIOL", "200", "pmol/L", "2026-09-11");
        new HormoneService().saveLog(patientId, "E2_OESTRADIOL", "800", "pmol/L", "2026-09-13");
        new HormoneService().saveLog(patientId, "LH", "5", "IU/L", "2026-09-12");   // andet hormon – skal ikke med
        HormoneCurve curve = new HormoneService().getCurve(patientId, HormoneType.E2_OESTRADIOL);
        assertEquals(2, curve.getPoints().size());
        assertEquals(800.0, curve.getMax());
        assertEquals(200.0, curve.getPoints().get(0).getValue());   // ældste først
        assertEquals("13/9", curve.getPoints().get(1).getDateLabel());
        // største værdi ligger øverst (y = PADDING = 20), og x vokser mod højre
        assertEquals(20.0, curve.getPoints().get(1).getY());
        assertTrue(curve.getPoints().get(0).getX() < curve.getPoints().get(1).getX());
        assertEquals("20.0,95.0 300.0,20.0", curve.getPolyline());
        // gennemsnit af 200 og 800 = 500 -> 500/800 = 62,5 % op: y = 120 - 62,5 = 57,5 -> afrundet 58
        assertEquals(500.0, curve.getAverage());
        assertEquals(58.0, curve.getAverageY());   // 200 af 800 = 25 % op ad de 100 px: y = 120 - 25 = 95
    }

    @Test
    void deleteLogRemovesOwnLog() {
        int patientId = TestData.newPatientWithRound();
        new HormoneService().saveLog(patientId, "LH", "5", "IU/L", "2026-09-12");
        int id = new HormoneService().getLogs(patientId).get(0).getId();
        assertEquals(ServiceResult.OK, new HormoneService().deleteLog(patientId, id));
        assertTrue(new HormoneService().getLogs(patientId).isEmpty());
    }

    @Test
    void deleteLogOfAnotherPatientReturnsNotFound() {
        int anna = TestData.newPatientWithRound();
        int maria = TestData.newPatientWithRound();
        new HormoneService().saveLog(anna, "LH", "5", "IU/L", "2026-09-12");
        int id = new HormoneService().getLogs(anna).get(0).getId();
        assertEquals(ServiceResult.NOT_FOUND, new HormoneService().deleteLog(maria, id));
        assertEquals(1, new HormoneService().getLogs(anna).size());
    }
}
