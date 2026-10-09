package services;

import enums.ServiceResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Integrationstests af DiaryService (dagbog, US4)
class DiaryServiceTest {

    @BeforeAll
    static void setup() {
        TestData.freshDatabase();
    }

    @Test
    void saveEntryWithBlankTitleReturnsInvalidInput() {
        int patientId = TestData.newPatientWithJourney();
        assertEquals(ServiceResult.INVALID_INPUT, new DiaryService(TestData.pool()).saveEntry(patientId, "2026-09-12", "", "Tekst"));
    }

    @Test
    void saveEntryWithInvalidDateReturnsInvalidInput() {
        int patientId = TestData.newPatientWithJourney();
        assertEquals(ServiceResult.INVALID_INPUT, new DiaryService(TestData.pool()).saveEntry(patientId, "12/9", "Titel", "Tekst"));
    }

    @Test
    void saveEntryWithoutJourneyIsAllowed() {
        int patientId = TestData.newPatient();
        // dagbog hører til patienten – man kan skrive før første forløb
        assertEquals(ServiceResult.OK, new DiaryService(TestData.pool()).saveEntry(patientId, "2026-09-12", "Før forløbet", "Venter på henvisning."));
        assertEquals(1, new DiaryService(TestData.pool()).getEntries(patientId).size());
    }

    @Test
    void saveEntryReturnsOk() {
        int patientId = TestData.newPatientWithJourney();
        // noter hænger på forløbet – ingen runde nødvendig
        assertEquals(ServiceResult.OK, new DiaryService(TestData.pool()).saveEntry(patientId, "2026-09-12", "Scanning i dag", "Alt så fint ud"));
    }

    @Test
    void newPatientHasNoEntries() {
        int patientId = TestData.newPatient();
        assertTrue(new DiaryService(TestData.pool()).getEntries(patientId).isEmpty());
    }

    @Test
    void savedEntryKeepsTitleAndContent() {
        int patientId = TestData.newPatientWithJourney();
        new DiaryService(TestData.pool()).saveEntry(patientId, "2026-09-12", "Scanning", "Gik fint");
        assertEquals(1, new DiaryService(TestData.pool()).getEntries(patientId).size());
        assertEquals("Scanning", new DiaryService(TestData.pool()).getEntries(patientId).get(0).getTitle());
        assertEquals("Gik fint", new DiaryService(TestData.pool()).getEntries(patientId).get(0).getContent());
    }

    @Test
    void entriesFromOnePatientAreNotVisibleToAnother() {
        int anna = TestData.newPatientWithJourney();
        int maria = TestData.newPatientWithJourney();
        new DiaryService(TestData.pool()).saveEntry(anna, "2026-09-12", "Annas note", "Privat");
        assertEquals(1, new DiaryService(TestData.pool()).getEntries(anna).size());
        assertEquals(0, new DiaryService(TestData.pool()).getEntries(maria).size());
    }

    @Test
    void deleteEntryRemovesOwnEntry() {
        int patientId = TestData.newPatientWithJourney();
        new DiaryService(TestData.pool()).saveEntry(patientId, "2026-09-12", "Slet mig", "…");
        int id = new DiaryService(TestData.pool()).getEntries(patientId).get(0).getId();
        assertEquals(ServiceResult.OK, new DiaryService(TestData.pool()).deleteEntry(patientId, id));
        assertTrue(new DiaryService(TestData.pool()).getEntries(patientId).isEmpty());
    }

    @Test
    void deleteEntryOfAnotherPatientReturnsNotFound() {
        int anna = TestData.newPatientWithJourney();
        int maria = TestData.newPatientWithJourney();
        new DiaryService(TestData.pool()).saveEntry(anna, "2026-09-12", "Annas note", "Privat");
        int id = new DiaryService(TestData.pool()).getEntries(anna).get(0).getId();
        assertEquals(ServiceResult.NOT_FOUND, new DiaryService(TestData.pool()).deleteEntry(maria, id));
        assertEquals(1, new DiaryService(TestData.pool()).getEntries(anna).size()); // stadig der
    }
}
