package services;

import dao.DatabaseConnection;
import dao.DiagnosisDAO;
import enums.ServiceResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Unit tests af DiagnosisService (diagnoser, US7)
class DiagnosisServiceTest {

    @BeforeAll
    static void setup() {
        TestData.freshDatabase();
    }

    @Test
    void addDiagnosisWithBlankNameReturnsInvalidInput() {
        int patientId = TestData.newPatient();
        assertEquals(ServiceResult.INVALID_INPUT, new DiagnosisService().addDiagnosis(patientId, "  ", "beskrivelse"));
    }

    @Test
    void addDiagnosisWithoutDescriptionReturnsOk() {
        int patientId = TestData.newPatient();
        // beskrivelsen er valgfri – diagnoser hænger på patienten, intet forløb nødvendigt
        assertEquals(ServiceResult.OK, new DiagnosisService().addDiagnosis(patientId, "PCOS", ""));
    }

    @Test
    void addDiagnosisTwiceGivesTwoDiagnosesForPatient() {
        int patientId = TestData.newPatient();
        DiagnosisService service = new DiagnosisService();
        service.addDiagnosis(patientId, "PCOS", null);
        service.addDiagnosis(patientId, "Endometriose", "Grad 2");
        // en patient kan have flere diagnoser
        assertEquals(2, new DiagnosisDAO(DatabaseConnection.getConnection()).findByPatient(patientId).size());
    }
}
