package services;

import enums.ServiceResult;
import entities.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Unit tests af DocumentService (dokumenter, US11)
class DocumentServiceTest {

    private static List<Integer> patients = new ArrayList<>(); // alle patienter, testene har uploadet for – til oprydning

    @BeforeAll
    static void setup() {
        TestData.freshDatabase();
    }

    // Kører én gang efter alle tests: sletter de små testfiler, uploadDocument har skrevet i uploads/,
    // så mappen kun indeholder rigtige uploads. Stien står i databasen (filePath)
    @AfterAll
    static void deleteTestFiles() throws IOException {
        for (int patientId : patients) {
            for (Document d : new DocumentService().getDocuments(patientId)) {
                Files.deleteIfExists(Path.of(d.getFilePath()));
            }
        }
    }

    // hjælper: patient med runde, som huskes til oprydningen
    private static int patientWithRound() {
        int patientId = TestData.newPatientWithRound();
        patients.add(patientId);
        return patientId;
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
        int patientId = patientWithRound();
        assertEquals(ServiceResult.OK, new DocumentService().uploadDocument(patientId, "Blodprøve uge 3", "BLOOD_TEST_RESULT", "svar.png", fakeFile(), 100));
        assertEquals(1, new DocumentService().getDocuments(patientId).size());
        assertEquals("Blodprøve uge 3", new DocumentService().getDocuments(patientId).get(0).getTitle());
    }

    @Test
    void findDocumentReturnsOwnDocumentButNotAnothersPatients() {
        int anna = patientWithRound();
        int maria = patientWithRound();
        new DocumentService().uploadDocument(anna, "Annas plan", "TREATMENT_PLAN", "plan.pdf", fakeFile(), 100);
        int documentId = new DocumentService().getDocuments(anna).get(0).getId();
        assertNotNull(new DocumentService().findDocument(anna, documentId));
        assertNull(new DocumentService().findDocument(maria, documentId)); // Maria må ikke kunne åbne Annas dokument
    }
}
