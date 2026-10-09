package services;

import enums.ServiceResult;
import java.time.LocalDate;
import entities.MedicationLog;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Integrationstests af MedicationService (medicin, US8). Medicin-stamdata (GONAL_F …) kommer fra INSERT i schema_postgres.sql
class MedicationServiceTest {

    @BeforeAll
    static void setup() {
        TestData.freshDatabase();
    }

    @Test
    void logDoseWithBlankFieldReturnsInvalidInput() {
        int patientId = TestData.newPatientWithRound();
        assertEquals(ServiceResult.INVALID_INPUT, new MedicationService(TestData.pool()).logDose(patientId, "GONAL_F", "", "2026-09-12", "08:00", false));
    }

    @Test
    void logDoseWithTextAsDoseReturnsInvalidInput() {
        int patientId = TestData.newPatientWithRound();
        assertEquals(ServiceResult.INVALID_INPUT, new MedicationService(TestData.pool()).logDose(patientId, "GONAL_F", "mange", "2026-09-12", "08:00", false));
    }

    @Test
    void logDoseWithZeroDoseReturnsInvalidInput() {
        int patientId = TestData.newPatientWithRound();
        // regel: dosis skal være større end 0
        assertEquals(ServiceResult.INVALID_INPUT, new MedicationService(TestData.pool()).logDose(patientId, "GONAL_F", "0", "2026-09-12", "08:00", false));
    }

    @Test
    void logDoseWithUnknownMedicationReturnsInvalidInput() {
        int patientId = TestData.newPatientWithRound();
        assertEquals(ServiceResult.INVALID_INPUT, new MedicationService(TestData.pool()).logDose(patientId, "PANODIL", "150", "2026-09-12", "08:00", false));
    }

    @Test
    void logDoseWithoutJourneyReturnsNoActiveJourney() {
        int patientId = TestData.newPatient();
        assertEquals(ServiceResult.NO_ACTIVE_JOURNEY, new MedicationService(TestData.pool()).logDose(patientId, "GONAL_F", "150", "2026-09-12", "08:00", false));
    }

    @Test
    void logDoseWithoutRoundReturnsNoActiveRound() {
        int patientId = TestData.newPatientWithJourney();
        assertEquals(ServiceResult.NO_ACTIVE_ROUND, new MedicationService(TestData.pool()).logDose(patientId, "GONAL_F", "150", "2026-09-12", "08:00", false));
    }

    @Test
    void logDoseReturnsOk() {
        int patientId = TestData.newPatientWithRound();
        assertEquals(ServiceResult.OK, new MedicationService(TestData.pool()).logDose(patientId, "GONAL_F", "150", "2026-09-12", "08:00", true));
    }

    @Test
    void doseScheduledTodayIsInTodayLogs() {
        int patientId = TestData.newPatientWithRound();
        new MedicationService(TestData.pool()).logDose(patientId, "GONAL_F", "150", LocalDate.now().toString(), "08:00", false);
        assertEquals(1, new MedicationService(TestData.pool()).getTodayLogs(patientId).size());
        assertEquals(0, new MedicationService(TestData.pool()).getPastLogs(patientId).size());
    }

    @Test
    void doseScheduledYesterdayIsInPastLogs() {
        int patientId = TestData.newPatientWithRound();
        new MedicationService(TestData.pool()).logDose(patientId, "GONAL_F", "150", LocalDate.now().minusDays(1).toString(), "08:00", true);
        assertEquals(0, new MedicationService(TestData.pool()).getTodayLogs(patientId).size());
        assertEquals(1, new MedicationService(TestData.pool()).getPastLogs(patientId).size());
    }

    @Test
    void markTakenSetsTakenToTrue() {
        int patientId = TestData.newPatientWithRound();
        new MedicationService(TestData.pool()).logDose(patientId, "MENOPUR", "75", LocalDate.now().toString(), "20:00", false);
        MedicationLog log = new MedicationService(TestData.pool()).getTodayLogs(patientId).get(0);
        assertFalse(log.isTaken());
        assertEquals(ServiceResult.OK, new MedicationService(TestData.pool()).markTaken(patientId, log.getId()));
        assertTrue(new MedicationService(TestData.pool()).getTodayLogs(patientId).get(0).isTaken());
    }

    @Test
    void getMedicationNamesContainsSeededMedications() {
        // schema_postgres.sql lægger de faste præparater ind – opslaget id -> navn bruges af medicin.html
        assertTrue(new MedicationService(TestData.pool()).getMedicationNames().containsValue("Gonal-F"));
        assertTrue(new MedicationService(TestData.pool()).getMedicationNames().size() >= 4);
    }

    @Test
    void getTodayLogsWithoutRoundReturnsEmptyList() {
        int patientId = TestData.newPatientWithJourney();
        assertTrue(new MedicationService(TestData.pool()).getTodayLogs(patientId).isEmpty());
    }

    @Test
    void doseScheduledTomorrowIsInUpcomingLogs() {
        int patientId = TestData.newPatientWithRound();
        new MedicationService(TestData.pool()).logDose(patientId, "GONAL_F", "150", LocalDate.now().plusDays(1).toString(), "08:00", false);
        assertEquals(1, new MedicationService(TestData.pool()).getUpcomingLogs(patientId).size());
        assertTrue(new MedicationService(TestData.pool()).getTodayLogs(patientId).isEmpty());
        assertTrue(new MedicationService(TestData.pool()).getPastLogs(patientId).isEmpty());
    }

    @Test
    void doseTextHasNoTrailingZeros() {
        int patientId = TestData.newPatientWithRound();
        new MedicationService(TestData.pool()).logDose(patientId, "GONAL_F", "150", LocalDate.now().toString(), "08:00", false);
        new MedicationService(TestData.pool()).logDose(patientId, "OVITRELLE", "0.25", LocalDate.now().toString(), "09:00", false);
        var today = new MedicationService(TestData.pool()).getTodayLogs(patientId);
        assertEquals("150", today.get(0).getDoseText());    // ikke "150.0"
        assertEquals("0.25", today.get(1).getDoseText());
    }

    @Test
    void deleteDoseRemovesOwnDose() {
        int patientId = TestData.newPatientWithRound();
        MedicationService service = new MedicationService(TestData.pool());
        service.logDose(patientId, "GONAL_F", "1500", LocalDate.now().toString(), "08:00", false);   // fejlindtastning
        int id = service.getTodayLogs(patientId).get(0).getId();
        assertEquals(ServiceResult.OK, service.deleteDose(patientId, id));
        assertTrue(service.getTodayLogs(patientId).isEmpty());
    }

    @Test
    void deleteAnotherPatientsDoseReturnsNotFound() {
        int anna = TestData.newPatientWithRound();
        int maria = TestData.newPatientWithRound();
        MedicationService service = new MedicationService(TestData.pool());
        service.logDose(anna, "GONAL_F", "150", LocalDate.now().toString(), "08:00", false);
        int id = service.getTodayLogs(anna).get(0).getId();
        assertEquals(ServiceResult.NOT_FOUND, service.deleteDose(maria, id));
        assertEquals(1, service.getTodayLogs(anna).size());   // stadig der
    }
}
