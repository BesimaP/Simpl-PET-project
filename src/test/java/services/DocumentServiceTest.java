package services;

import enums.ServiceResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

// Unit tests af DocumentService (dokumenter, US11)
class DocumentServiceTest {

    @BeforeAll
    static void setup() {
        TestData.freshDatabase();
    }

    // lille hjælper: en "fil" i hukommelsen, så testen ikke behøver en rigtig fil på disken
    private static InputStream fakeFile() {
        return new ByteArrayInputStream("indhold".getBytes());
    }

    @Test
    void uploadWithBlankTitleReturnsInvalidInput() {
        int patientId = TestData.newPatientWithRound();
        assertEquals(ServiceResult.INVALID_INPUT, new DocumentService().uploadDocument(patientId, "", "TREATMENT_PLAN", "plan.pdf", fakeFile(), 100));
    }

    @Test
    void uploadWithUnknownTypeReturnsInvalidInput() {
        int patientId = TestData.newPatientWithRound();
        assertEquals(ServiceResult.INVALID_INPUT, new DocumentService().uploadDocument(patientId, "Plan", "RECIPE", "plan.pdf", fakeFile(), 100));
    }

    @Test
    void uploadWithWrongFileTypeReturnsInvalidInput() {
        int patientId = TestData.newPatientWithRound();
        // kun pdf, jpg og png er tilladt
        assertEquals(ServiceResult.INVALID_INPUT, new DocumentService().uploadDocument(patientId, "Plan", "TREATMENT_PLAN", "plan.docx", fakeFile(), 100));
    }

    @Test
    void uploadOver10MbReturnsInvalidInput() {
        int patientId = TestData.newPatientWithRound();
        long elevenMb = 11L * 1024 * 1024;
        assertEquals(ServiceResult.INVALID_INPUT, new DocumentService().uploadDocument(patientId, "Plan", "TREATMENT_PLAN", "plan.pdf", fakeFile(), elevenMb));
    }

    @Test
    void uploadWithoutJourneyReturnsNoActiveJourney() {
        int patientId = TestData.newPatient();
        assertEquals(ServiceResult.NO_ACTIVE_JOURNEY, new DocumentService().uploadDocument(patientId, "Plan", "TREATMENT_PLAN", "plan.pdf", fakeFile(), 100));
    }

    @Test
    void uploadWithoutRoundReturnsNoActiveRound() {
        int patientId = TestData.newPatientWithJourney();
        assertEquals(ServiceResult.NO_ACTIVE_ROUND, new DocumentService().uploadDocument(patientId, "Plan", "TREATMENT_PLAN", "plan.pdf", fakeFile(), 100));
    }

    @Test
    void uploadReturnsOkAndDocumentIsListed() {
        int patientId = TestData.newPatientWithRound();
        assertEquals(ServiceResult.OK, new DocumentService().uploadDocument(patientId, "Blodprøve uge 3", "BLOOD_TEST_RESULT", "svar.png", fakeFile(), 100));
        assertEquals(1, new DocumentService().getDocuments(patientId).size());
        assertEquals("Blodprøve uge 3", new DocumentService().getDocuments(patientId).get(0).getTitle());
    }

    @Test
    void findDocumentReturnsOwnDocumentButNotAnothersPatients() {
        int anna = TestData.newPatientWithRound();
        int maria = TestData.newPatientWithRound();
        new DocumentService().uploadDocument(anna, "Annas plan", "TREATMENT_PLAN", "plan.pdf", fakeFile(), 100);
        int documentId = new DocumentService().getDocuments(anna).get(0).getId();
        assertNotNull(new DocumentService().findDocument(anna, documentId));
        assertNull(new DocumentService().findDocument(maria, documentId)); // Maria må ikke kunne åbne Annas dokument
    }
}
