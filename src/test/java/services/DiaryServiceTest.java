package services;

import enums.ServiceResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Unit tests af DiaryService (dagbog, US4)
class DiaryServiceTest {

    @BeforeAll
    static void setup() {
        TestData.freshDatabase();
    }

    @Test
    void saveEntryWithBlankTitleReturnsInvalidInput() {
        int patientId = TestData.newPatientWithJourney();
        assertEquals(ServiceResult.INVALID_INPUT, new DiaryService().saveEntry(patientId, "2026-09-12", "", "Tekst"));
    }

    @Test
    void saveEntryWithInvalidDateReturnsInvalidInput() {
        int patientId = TestData.newPatientWithJourney();
        assertEquals(ServiceResult.INVALID_INPUT, new DiaryService().saveEntry(patientId, "12/9", "Titel", "Tekst"));
    }

    @Test
    void saveEntryWithoutJourneyReturnsNoActiveJourney() {
        int patientId = TestData.newPatient();
        assertEquals(ServiceResult.NO_ACTIVE_JOURNEY, new DiaryService().saveEntry(patientId, "2026-09-12", "Titel", "Tekst"));
    }

    @Test
    void saveEntryReturnsOk() {
        int patientId = TestData.newPatientWithJourney();
        // noter hænger på forløbet – ingen runde nødvendig
        assertEquals(ServiceResult.OK, new DiaryService().saveEntry(patientId, "2026-09-12", "Scanning i dag", "Alt så fint ud"));
    }
}
