package services;

import enums.ServiceResult;
import java.time.LocalDate;
import enums.EventType;
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

    @Test
    void getUpcomingWithoutJourneyReturnsEmptyList() {
        int patientId = TestData.newPatient();
        assertTrue(new AppointmentService().getUpcoming(patientId).isEmpty());
    }

    @Test
    void appointmentInFutureIsUpcomingNotPast() {
        int patientId = TestData.newPatientWithJourney();
        String nextYear = LocalDate.now().plusYears(1).toString(); // altid i fremtiden, uanset hvornår testen kører
        new AppointmentService().addAppointment(patientId, "SCANNING", "Vitanova", nextYear, "10:30");
        assertEquals(1, new AppointmentService().getUpcoming(patientId).size());
        assertEquals(0, new AppointmentService().getPast(patientId).size());
    }

    @Test
    void appointmentInPastIsPastNotUpcoming() {
        int patientId = TestData.newPatientWithJourney();
        String lastYear = LocalDate.now().minusYears(1).toString();
        new AppointmentService().addAppointment(patientId, "BLOOD_TEST", "Vitanova", lastYear, "10:30");
        assertEquals(0, new AppointmentService().getUpcoming(patientId).size());
        assertEquals(1, new AppointmentService().getPast(patientId).size());
    }

    @Test
    void savedAppointmentKeepsTypeAndLocation() {
        int patientId = TestData.newPatientWithJourney();
        new AppointmentService().addAppointment(patientId, "CONSULTATION", "Rigshospitalet", "2099-01-01", "09:00");
        assertEquals("Rigshospitalet", new AppointmentService().getUpcoming(patientId).get(0).getLocation());
        assertEquals("Konsultation", new AppointmentService().getUpcoming(patientId).get(0).getAppointmentType().getLabel());
    }

    @Test
    void eggRetrievalAppointmentCreatesTimelineEvent() {
        int patientId = TestData.newPatientWithRound(); // runde i gang -> 1 event (STIMULATION_START) i forvejen
        new AppointmentService().addAppointment(patientId, "EGG_RETRIEVAL", "Vitanova", "2026-10-05", "08:00");
        assertEquals(2, new TimelineService().getEvents(patientId).size());
        assertEquals(EventType.EGG_RETRIEVAL, new TimelineService().getEvents(patientId).get(1).getEventType());
    }

    @Test
    void scanningAppointmentDoesNotCreateTimelineEvent() {
        int patientId = TestData.newPatientWithRound();
        new AppointmentService().addAppointment(patientId, "SCANNING", "Vitanova", "2026-10-05", "08:00");
        // scanning er en almindelig aftale, ikke et trin i runden
        assertEquals(1, new TimelineService().getEvents(patientId).size());
    }

    @Test
    void eggRetrievalWithoutRoundIsSavedButNoEvent() {
        int patientId = TestData.newPatientWithJourney(); // forløb, men ingen runde
        assertEquals(ServiceResult.OK, new AppointmentService().addAppointment(patientId, "EGG_RETRIEVAL", "Vitanova", "2099-10-05", "08:00"));
        assertEquals(1, new AppointmentService().getUpcoming(patientId).size());
        assertTrue(new TimelineService().getEvents(patientId).isEmpty());
    }
}
