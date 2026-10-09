package services;

import enums.ServiceResult;
import java.time.LocalDate;
import enums.EventType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Integrationstests af AppointmentService (aftaler, US3)
class AppointmentServiceTest {

    @BeforeAll
    static void setup() {
        TestData.freshDatabase();
    }

    @Test
    void addAppointmentWithBlankLocationReturnsInvalidInput() {
        int patientId = TestData.newPatientWithJourney();
        assertEquals(ServiceResult.INVALID_INPUT, new AppointmentService(TestData.pool()).addAppointment(patientId, "SCANNING", "", "2026-09-20", "10:30"));
    }

    @Test
    void addAppointmentWithUnknownTypeReturnsInvalidInput() {
        int patientId = TestData.newPatientWithJourney();
        assertEquals(ServiceResult.INVALID_INPUT, new AppointmentService(TestData.pool()).addAppointment(patientId, "MASSAGE", "Vitanova", "2026-09-20", "10:30"));
    }

    @Test
    void addAppointmentWithInvalidTimeReturnsInvalidInput() {
        int patientId = TestData.newPatientWithJourney();
        assertEquals(ServiceResult.INVALID_INPUT, new AppointmentService(TestData.pool()).addAppointment(patientId, "SCANNING", "Vitanova", "2026-09-20", "halv elleve"));
    }

    @Test
    void addAppointmentWithoutJourneyReturnsNoActiveJourney() {
        int patientId = TestData.newPatient();
        assertEquals(ServiceResult.NO_ACTIVE_JOURNEY, new AppointmentService(TestData.pool()).addAppointment(patientId, "SCANNING", "Vitanova", "2026-09-20", "10:30"));
    }

    @Test
    void addAppointmentReturnsOk() {
        int patientId = TestData.newPatientWithJourney();
        // aftaler hænger på forløbet – ingen runde nødvendig
        assertEquals(ServiceResult.OK, new AppointmentService(TestData.pool()).addAppointment(patientId, "SCANNING", "Vitanova", "2026-09-20", "10:30"));
    }

    @Test
    void getUpcomingWithoutJourneyReturnsEmptyList() {
        int patientId = TestData.newPatient();
        assertTrue(new AppointmentService(TestData.pool()).getUpcoming(patientId).isEmpty());
    }

    @Test
    void appointmentInFutureIsUpcomingNotPast() throws Exception {
        int patientId = TestData.newPatientWithJourney();
        String nextYear = LocalDate.now().plusYears(1).toString(); // altid i fremtiden, uanset hvornår testen kører
        new AppointmentService(TestData.pool()).addAppointment(patientId, "SCANNING", "Vitanova", nextYear, "10:30");
        assertEquals(1, new AppointmentService(TestData.pool()).getUpcoming(patientId).size());
        assertEquals(0, new AppointmentService(TestData.pool()).getPast(patientId, TestData.activeJourneyId(patientId)).size());
    }

    @Test
    void appointmentInPastIsPastNotUpcoming() throws Exception {
        int patientId = TestData.newPatientWithJourney();
        String lastYear = LocalDate.now().minusYears(1).toString();
        new AppointmentService(TestData.pool()).addAppointment(patientId, "BLOOD_TEST", "Vitanova", lastYear, "10:30");
        assertEquals(0, new AppointmentService(TestData.pool()).getUpcoming(patientId).size());
        assertEquals(1, new AppointmentService(TestData.pool()).getPast(patientId, TestData.activeJourneyId(patientId)).size());
    }

    @Test
    void savedAppointmentKeepsTypeAndLocation() {
        int patientId = TestData.newPatientWithJourney();
        new AppointmentService(TestData.pool()).addAppointment(patientId, "CONSULTATION", "Rigshospitalet", "2099-01-01", "09:00");
        assertEquals("Rigshospitalet", new AppointmentService(TestData.pool()).getUpcoming(patientId).get(0).getLocation());
        assertEquals("Konsultation", new AppointmentService(TestData.pool()).getUpcoming(patientId).get(0).getAppointmentType().getLabel());
    }

    @Test
    void eggRetrievalAppointmentCreatesTimelineEvent() throws Exception {
        int patientId = TestData.newPatientWithRound(); // runde i gang -> 1 event (STIMULATION_START) i forvejen
        new AppointmentService(TestData.pool()).addAppointment(patientId, "EGG_RETRIEVAL", "Vitanova", "2026-10-05", "08:00");
        assertEquals(2, TestData.eventsInActiveRound(patientId).size());
        assertEquals(EventType.EGG_RETRIEVAL, TestData.eventsInActiveRound(patientId).get(1).getEventType());
    }

    @Test
    void eggRetrievalBeforeRoundStartDoesNotCreateTimelineEvent() throws Exception {
        int patientId = TestData.newPatientWithRound(); // runden startede 2026-09-10
        new AppointmentService(TestData.pool()).addAppointment(patientId, "EGG_RETRIEVAL", "Vitanova", "2026-09-01", "08:00");
        // aftalen ligger før runden -> den gemmes, men kommer ikke på rundens tidslinje
        assertEquals(1, TestData.eventsInActiveRound(patientId).size());
    }

    @Test
    void scanningAppointmentDoesNotCreateTimelineEvent() throws Exception {
        int patientId = TestData.newPatientWithRound();
        new AppointmentService(TestData.pool()).addAppointment(patientId, "SCANNING", "Vitanova", "2026-10-05", "08:00");
        // scanning er en almindelig aftale, ikke et trin i runden
        assertEquals(1, TestData.eventsInActiveRound(patientId).size());
    }

    @Test
    void eggRetrievalWithoutRoundIsSavedButNoEvent() {
        int patientId = TestData.newPatientWithJourney(); // forløb, men ingen runde
        // aftalen gemmes uden fejl – der er bare ingen runde at lægge et tidslinje-trin på (event.round_id er NOT NULL)
        assertEquals(ServiceResult.OK, new AppointmentService(TestData.pool()).addAppointment(patientId, "EGG_RETRIEVAL", "Vitanova", "2099-10-05", "08:00"));
        assertEquals(1, new AppointmentService(TestData.pool()).getUpcoming(patientId).size());
    }

    @Test
    void appointmentsFromEndedJourneyCanStillBeShown() throws Exception {
        int patientId = TestData.newPatientWithJourney();
        AppointmentService appointments = new AppointmentService(TestData.pool());
        DashboardService journeys = new DashboardService(TestData.pool());
        appointments.addAppointment(patientId, "CONSULTATION", "Vitanova", "2026-09-05", "09:00");
        int oldJourneyId = journeys.getJourneys(patientId).get(0).getId();
        journeys.endJourney(patientId);
        journeys.createJourney(patientId, java.time.LocalDate.now().toString());
        assertTrue(appointments.getPast(patientId, TestData.activeJourneyId(patientId)).isEmpty());                    // det nye forløb har ingen aftaler
        assertEquals(1, appointments.getPast(patientId, oldJourneyId).size());   // men det gamle har stadig sin
    }

    @Test
    void appointmentsFromAnotherPatientsJourneyAreNotShown() {
        int anna = TestData.newPatientWithJourney();
        int maria = TestData.newPatient();
        new AppointmentService(TestData.pool()).addAppointment(anna, "SCANNING", "Vitanova", "2026-09-05", "09:00");
        int annasJourney = new DashboardService(TestData.pool()).getJourneys(anna).get(0).getId();
        assertTrue(new AppointmentService(TestData.pool()).getPast(maria, annasJourney).isEmpty());
    }
}
