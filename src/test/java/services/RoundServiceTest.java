package services;

import enums.Result;
import enums.RoundStatus;
import enums.ServiceResult;
import exceptions.NoActiveJourneyException;
import exceptions.NoActiveRoundException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Unit tests af RoundService (start/afslut runde, US10a/10b)
class RoundServiceTest {

    @BeforeAll
    static void setup() {
        TestData.freshDatabase();
    }

    @Test
    void startRoundWithBlankFieldsReturnsInvalidInput() {
        int patientId = TestData.newPatientWithJourney();
        assertEquals(ServiceResult.INVALID_INPUT, new RoundService().startRound(patientId, "", "2026-09-10"));
    }

    @Test
    void startRoundWithUnknownTypeReturnsInvalidInput() {
        int patientId = TestData.newPatientWithJourney();
        // "XYZ" findes ikke i enum TreatmentType
        assertEquals(ServiceResult.INVALID_INPUT, new RoundService().startRound(patientId, "XYZ", "2026-09-10"));
    }

    @Test
    void startRoundWithoutJourneyReturnsNoActiveJourney() {
        int patientId = TestData.newPatient();
        assertEquals(ServiceResult.NO_ACTIVE_JOURNEY, new RoundService().startRound(patientId, "IVF", "2026-09-10"));
    }

    @Test
    void startRoundReturnsOkAndRoundIsInProgress() throws Exception {
        int patientId = TestData.newPatientWithJourney();
        RoundService service = new RoundService();
        assertEquals(ServiceResult.OK, service.startRound(patientId, "IVF", "2026-09-10"));
        assertEquals(RoundStatus.IN_PROGRESS, service.findActiveRound(patientId).getStatus());
        assertEquals(1, service.findActiveRound(patientId).getRoundNumber()); // første runde = nr. 1
    }

    @Test
    void startRoundWhileRoundInProgressReturnsRoundInProgress() {
        int patientId = TestData.newPatientWithRound();
        // regel: kun én runde i gang ad gangen
        assertEquals(ServiceResult.ROUND_IN_PROGRESS, new RoundService().startRound(patientId, "ICSI", "2026-09-11"));
    }

    @Test
    void endRoundWithoutRoundReturnsNoActiveRound() {
        int patientId = TestData.newPatientWithJourney();
        assertEquals(ServiceResult.NO_ACTIVE_ROUND, new RoundService().endRound(patientId, Result.POSITIVE));
    }

    @Test
    void endRoundReturnsOkAndNoRoundIsActiveAfterwards() {
        int patientId = TestData.newPatientWithRound();
        RoundService service = new RoundService();
        assertEquals(ServiceResult.OK, service.endRound(patientId, Result.NEGATIVE));
        // runden er afsluttet -> der er ikke længere en runde i gang
        assertThrows(NoActiveRoundException.class, () -> service.findActiveRound(patientId));
    }

    @Test
    void endRoundWithoutResultIsAllowed() {
        int patientId = TestData.newPatientWithRound();
        // resultatet må være null (kan udfyldes senere)
        assertEquals(ServiceResult.OK, new RoundService().endRound(patientId, null));
    }

    @Test
    void secondRoundGetsRoundNumberTwo() throws Exception {
        int patientId = TestData.newPatientWithRound();
        RoundService service = new RoundService();
        service.endRound(patientId, Result.NEGATIVE);
        service.startRound(patientId, "FET", "2026-10-01");
        assertEquals(2, service.findActiveRound(patientId).getRoundNumber());
    }

    @Test
    void findActiveRoundWithoutJourneyThrowsNoActiveJourney() {
        int patientId = TestData.newPatient();
        assertThrows(NoActiveJourneyException.class, () -> new RoundService().findActiveRound(patientId));
    }

    @Test
    void getRoundsWithoutJourneyReturnsEmptyList() {
        int patientId = TestData.newPatient();
        assertTrue(new RoundService().getRounds(patientId).isEmpty());
    }

    @Test
    void getRoundsContainsBothFinishedAndActiveRound() {
        int patientId = TestData.newPatientWithRound();
        new RoundService().endRound(patientId, null);
        new RoundService().startRound(patientId, "FET", "2026-10-01");
        assertEquals(2, new RoundService().getRounds(patientId).size());
        assertEquals(RoundStatus.COMPLETED, new RoundService().getRounds(patientId).get(0).getStatus());
        assertEquals(RoundStatus.IN_PROGRESS, new RoundService().getRounds(patientId).get(1).getStatus());
    }

    @Test
    void startRoundWithInvalidDateReturnsInvalidInput() {
        int patientId = TestData.newPatientWithJourney();
        assertEquals(ServiceResult.INVALID_INPUT, new RoundService().startRound(patientId, "IVF", "10-09-2026"));
    }
}
