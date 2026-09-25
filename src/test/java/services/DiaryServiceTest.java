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

    @Test
    void getEntriesWithoutJourneyReturnsEmptyList() {
        int patientId = TestData.newPatient();
        assertTrue(new DiaryService().getEntries(patientId).isEmpty());
    }

    @Test
    void savedEntryKeepsTitleAndContent() {
        int patientId = TestData.newPatientWithJourney();
        new DiaryService().saveEntry(patientId, "2026-09-12", "Scanning", "Gik fint");
        assertEquals(1, new DiaryService().getEntries(patientId).size());
        assertEquals("Scanning", new DiaryService().getEntries(patientId).get(0).getTitle());
        assertEquals("Gik fint", new DiaryService().getEntries(patientId).get(0).getContent());
    }

    @Test
    void entriesFromOnePatientAreNotVisibleToAnother() {
        int anna = TestData.newPatientWithJourney();
        int maria = TestData.newPatientWithJourney();
        new DiaryService().saveEntry(anna, "2026-09-12", "Annas note", "Privat");
        assertEquals(1, new DiaryService().getEntries(anna).size());
        assertEquals(0, new DiaryService().getEntries(maria).size());
    }
}
