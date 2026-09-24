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
        assertEquals(ServiceResult.INVALID_INPUT, new DashboardService().createJourney(patientId, ""));
    }

    @Test
    void createJourneyWithInvalidDateReturnsInvalidInput() {
        int patientId = TestData.newPatient();
        assertEquals(ServiceResult.INVALID_INPUT, new DashboardService().createJourney(patientId, "1/9-2026"));
    }

    @Test
    void createJourneyReturnsOkAndJourneyIsActive() throws NoActiveJourneyException {
        int patientId = TestData.newPatient();
        DashboardService service = new DashboardService();
        assertEquals(ServiceResult.OK, service.createJourney(patientId, "2026-09-01"));
        assertEquals(patientId, service.findActiveJourney(patientId).getPatientId());
    }

    @Test
    void createJourneyTwiceReturnsAlreadyExists() {
        int patientId = TestData.newPatient();
        DashboardService service = new DashboardService();
        service.createJourney(patientId, "2026-09-01");
        // regel: kun ét aktivt forløb ad gangen
        assertEquals(ServiceResult.ALREADY_EXISTS, service.createJourney(patientId, "2026-09-02"));
    }

    @Test
    void findActiveJourneyWithoutJourneyThrowsException() {
        int patientId = TestData.newPatient();
        assertThrows(NoActiveJourneyException.class, () -> new DashboardService().findActiveJourney(patientId));
    }
}
