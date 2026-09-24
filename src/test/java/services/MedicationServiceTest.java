package services;

import enums.ServiceResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Unit tests af MedicationService (medicin, US8). Medicin-stamdata (GONAL_F …) kommer fra INSERT i schema.sql
class MedicationServiceTest {

    @BeforeAll
    static void setup() {
        TestData.freshDatabase();
    }

    @Test
    void logDoseWithBlankFieldReturnsInvalidInput() {
        int patientId = TestData.newPatientWithRound();
        assertEquals(ServiceResult.INVALID_INPUT, new MedicationService().logDose(patientId, "GONAL_F", "", "IU", "2026-09-12", "08:00", false));
    }

    @Test
    void logDoseWithTextAsDoseReturnsInvalidInput() {
        int patientId = TestData.newPatientWithRound();
        assertEquals(ServiceResult.INVALID_INPUT, new MedicationService().logDose(patientId, "GONAL_F", "mange", "IU", "2026-09-12", "08:00", false));
    }

    @Test
    void logDoseWithZeroDoseReturnsInvalidInput() {
        int patientId = TestData.newPatientWithRound();
        // regel: dosis skal være større end 0
        assertEquals(ServiceResult.INVALID_INPUT, new MedicationService().logDose(patientId, "GONAL_F", "0", "IU", "2026-09-12", "08:00", false));
    }

    @Test
    void logDoseWithUnknownMedicationReturnsInvalidInput() {
        int patientId = TestData.newPatientWithRound();
        assertEquals(ServiceResult.INVALID_INPUT, new MedicationService().logDose(patientId, "PANODIL", "150", "IU", "2026-09-12", "08:00", false));
    }

    @Test
    void logDoseWithoutJourneyReturnsNoActiveJourney() {
        int patientId = TestData.newPatient();
        assertEquals(ServiceResult.NO_ACTIVE_JOURNEY, new MedicationService().logDose(patientId, "GONAL_F", "150", "IU", "2026-09-12", "08:00", false));
    }

    @Test
    void logDoseWithoutRoundReturnsNoActiveRound() {
        int patientId = TestData.newPatientWithJourney();
        assertEquals(ServiceResult.NO_ACTIVE_ROUND, new MedicationService().logDose(patientId, "GONAL_F", "150", "IU", "2026-09-12", "08:00", false));
    }

    @Test
    void logDoseReturnsOk() {
        int patientId = TestData.newPatientWithRound();
        assertEquals(ServiceResult.OK, new MedicationService().logDose(patientId, "GONAL_F", "150", "IU", "2026-09-12", "08:00", true));
    }
}
