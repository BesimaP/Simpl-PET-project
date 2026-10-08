package services;

import enums.ServiceResult;
import exceptions.NoActiveJourneyException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Unit tests af DashboardService (forløb, US1)
class DashboardServiceTest {

    @BeforeAll
    static void setup() {
        TestData.freshDatabase();
    }

    @Test
    void createJourneyWithBlankDateReturnsInvalidInput() {
        int patientId = TestData.newPatient();
        assertEquals(ServiceResult.INVALID_INPUT, new DashboardService(TestData.pool()).createJourney(patientId, ""));
    }

    @Test
    void createJourneyWithInvalidDateReturnsInvalidInput() {
        int patientId = TestData.newPatient();
        assertEquals(ServiceResult.INVALID_INPUT, new DashboardService(TestData.pool()).createJourney(patientId, "1/9-2026"));
    }

    @Test
    void createJourneyReturnsOkAndJourneyIsActive() throws NoActiveJourneyException {
        int patientId = TestData.newPatient();
        DashboardService service = new DashboardService(TestData.pool());
        assertEquals(ServiceResult.OK, service.createJourney(patientId, "2026-09-01"));
        assertEquals(patientId, service.findActiveJourney(patientId).getPatientId());
    }

    @Test
    void createJourneyTwiceReturnsAlreadyExists() {
        int patientId = TestData.newPatient();
        DashboardService service = new DashboardService(TestData.pool());
        service.createJourney(patientId, "2026-09-01");
        // regel: kun ét aktivt forløb ad gangen
        assertEquals(ServiceResult.ALREADY_EXISTS, service.createJourney(patientId, "2026-09-02"));
    }

    @Test
    void findActiveJourneyWithoutJourneyThrowsException() {
        int patientId = TestData.newPatient();
        assertThrows(NoActiveJourneyException.class, () -> new DashboardService(TestData.pool()).findActiveJourney(patientId));
    }

    @Test
    void endJourneyWithoutRoundReturnsOkAndNewJourneyCanBeCreated() {
        int patientId = TestData.newPatientWithJourney();
        DashboardService service = new DashboardService(TestData.pool());
        assertEquals(ServiceResult.OK, service.endJourney(patientId));
        assertThrows(NoActiveJourneyException.class, () -> service.findActiveJourney(patientId));
        // US1: en patient kan have flere forløb over tid
        assertEquals(ServiceResult.OK, service.createJourney(patientId, "2026-11-01"));
    }

    @Test
    void endJourneyWithRoundInProgressReturnsRoundInProgress() {
        int patientId = TestData.newPatientWithRound();
        assertEquals(ServiceResult.ROUND_IN_PROGRESS, new DashboardService(TestData.pool()).endJourney(patientId));
    }

    @Test
    void endJourneyWithoutJourneyReturnsNoActiveJourney() {
        int patientId = TestData.newPatient();
        assertEquals(ServiceResult.NO_ACTIVE_JOURNEY, new DashboardService(TestData.pool()).endJourney(patientId));
    }
}
