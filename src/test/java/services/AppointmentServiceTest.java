package services;

import enums.ServiceResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Unit tests af AppointmentService (aftaler, US3)
class AppointmentServiceTest {

    @BeforeAll
    static void setup() {
        TestData.freshDatabase();
    }

    @Test
    void addAppointmentWithBlankLocationReturnsInvalidInput() {
        int patientId = TestData.newPatientWithJourney();
        assertEquals(ServiceResult.INVALID_INPUT, new AppointmentService().addAppointment(patientId, "SCANNING", "", "2026-09-20", "10:30"));
    }

    @Test
    void addAppointmentWithUnknownTypeReturnsInvalidInput() {
        int patientId = TestData.newPatientWithJourney();
        assertEquals(ServiceResult.INVALID_INPUT, new AppointmentService().addAppointment(patientId, "MASSAGE", "Vitanova", "2026-09-20", "10:30"));
    }

    @Test
    void addAppointmentWithInvalidTimeReturnsInvalidInput() {
        int patientId = TestData.newPatientWithJourney();
        assertEquals(ServiceResult.INVALID_INPUT, new AppointmentService().addAppointment(patientId, "SCANNING", "Vitanova", "2026-09-20", "halv elleve"));
    }

    @Test
    void addAppointmentWithoutJourneyReturnsNoActiveJourney() {
        int patientId = TestData.newPatient();
        assertEquals(ServiceResult.NO_ACTIVE_JOURNEY, new AppointmentService().addAppointment(patientId, "SCANNING", "Vitanova", "2026-09-20", "10:30"));
    }

    @Test
    void addAppointmentReturnsOk() {
        int patientId = TestData.newPatientWithJourney();
        // aftaler hænger på forløbet – ingen runde nødvendig
        assertEquals(ServiceResult.OK, new AppointmentService().addAppointment(patientId, "SCANNING", "Vitanova", "2026-09-20", "10:30"));
    }
}
