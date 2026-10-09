package services;

import persistence.DiagnosisMapper;

import enums.ServiceResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Integrationstests af DiagnosisService (diagnoser, US7)
class DiagnosisServiceTest {

    @BeforeAll
    static void setup() {
        TestData.freshDatabase();
    }

    @Test
    void addDiagnosisWithBlankNameReturnsInvalidInput() {
        int patientId = TestData.newPatient();
        assertEquals(ServiceResult.INVALID_INPUT, new DiagnosisService(TestData.pool()).addDiagnosis(patientId, "  ", "beskrivelse"));
    }

    @Test
    void addDiagnosisWithoutDescriptionReturnsOk() {
        int patientId = TestData.newPatient();
        // beskrivelsen er valgfri – diagnoser hænger på patienten, intet forløb nødvendigt
        assertEquals(ServiceResult.OK, new DiagnosisService(TestData.pool()).addDiagnosis(patientId, "PCOS", ""));
    }

    @Test
    void addDiagnosisTwiceGivesTwoDiagnosesForPatient() {
        int patientId = TestData.newPatient();
        DiagnosisService service = new DiagnosisService(TestData.pool());
        service.addDiagnosis(patientId, "PCOS", null);
        service.addDiagnosis(patientId, "Endometriose", "Grad 2");
        // en patient kan have flere diagnoser
        assertEquals(2, new DiagnosisMapper(TestData.pool()).findByPatient(patientId).size());
    }

    @Test
    void newPatientHasNoDiagnoses() {
        int patientId = TestData.newPatient();
        assertTrue(new DiagnosisService(TestData.pool()).getDiagnoses(patientId).isEmpty());
    }

    @Test
    void savedDiagnosisKeepsName() {
        int patientId = TestData.newPatient();
        new DiagnosisService(TestData.pool()).addDiagnosis(patientId, "PCOS", "Konstateret ved første konsultation");
        assertEquals("PCOS", new DiagnosisService(TestData.pool()).getDiagnoses(patientId).get(0).getName());
    }

    @Test
    void nameIsSavedWithoutSpacesBeforeAndAfter() {
        int patientId = TestData.newPatient();
        DiagnosisService service = new DiagnosisService(TestData.pool());
        service.addDiagnosis(patientId, "  PCOS  ", "");
        assertEquals("PCOS", service.getDiagnoses(patientId).get(0).getName());
    }
}
