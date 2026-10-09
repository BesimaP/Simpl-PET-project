package services;

import enums.Result;
import enums.RoundStatus;
import enums.ServiceResult;
import exceptions.NoActiveJourneyException;
import exceptions.NoActiveRoundException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Integrationstests af RoundService (start/afslut runde, US10a/10b)
class RoundServiceTest {

    @BeforeAll
    static void setup() {
        TestData.freshDatabase();
    }

    @Test
    void startRoundWithBlankFieldsReturnsInvalidInput() {
        int patientId = TestData.newPatientWithJourney();
        assertEquals(ServiceResult.INVALID_INPUT, new RoundService(TestData.pool()).startRound(patientId, "", "2026-09-10"));
    }

    @Test
    void startRoundWithUnknownTypeReturnsInvalidInput() {
        int patientId = TestData.newPatientWithJourney();
        // "XYZ" findes ikke i enum TreatmentType
        assertEquals(ServiceResult.INVALID_INPUT, new RoundService(TestData.pool()).startRound(patientId, "XYZ", "2026-09-10"));
    }

    @Test
    void startRoundWithoutJourneyReturnsNoActiveJourney() {
        int patientId = TestData.newPatient();
        assertEquals(ServiceResult.NO_ACTIVE_JOURNEY, new RoundService(TestData.pool()).startRound(patientId, "IVF", "2026-09-10"));
    }

    @Test
    void startRoundReturnsOkAndRoundIsInProgress() throws Exception {
        int patientId = TestData.newPatientWithJourney();
        RoundService service = new RoundService(TestData.pool());
        assertEquals(ServiceResult.OK, service.startRound(patientId, "IVF", "2026-09-10"));
        assertEquals(RoundStatus.IN_PROGRESS, service.findActiveRound(patientId).getStatus());
        assertEquals(1, service.findActiveRound(patientId).getRoundNumber()); // første runde = nr. 1
    }

    @Test
    void startRoundWhileRoundInProgressReturnsRoundInProgress() {
        int patientId = TestData.newPatientWithRound();
        // regel: kun én runde i gang ad gangen
        assertEquals(ServiceResult.ROUND_IN_PROGRESS, new RoundService(TestData.pool()).startRound(patientId, "ICSI", "2026-09-11"));
    }

    @Test
    void endRoundWithoutRoundReturnsNoActiveRound() {
        int patientId = TestData.newPatientWithJourney();
        assertEquals(ServiceResult.NO_ACTIVE_ROUND, new RoundService(TestData.pool()).endRound(patientId, Result.POSITIVE));
    }

    @Test
    void endRoundReturnsOkAndNoRoundIsActiveAfterwards() {
        int patientId = TestData.newPatientWithRound();
        RoundService service = new RoundService(TestData.pool());
        assertEquals(ServiceResult.OK, service.endRound(patientId, Result.NEGATIVE));
        // runden er afsluttet -> der er ikke længere en runde i gang
        assertThrows(NoActiveRoundException.class, () -> service.findActiveRound(patientId));
    }

    @Test
    void endRoundWithoutResultIsAllowed() {
        int patientId = TestData.newPatientWithRound();
        // resultatet må være null (kan udfyldes senere)
        assertEquals(ServiceResult.OK, new RoundService(TestData.pool()).endRound(patientId, null));
    }

    @Test
    void secondRoundGetsRoundNumberTwo() throws Exception {
        int patientId = TestData.newPatientWithRound();
        RoundService service = new RoundService(TestData.pool());
        service.endRound(patientId, Result.NEGATIVE);
        service.startRound(patientId, "FET", "2026-10-01");
        assertEquals(2, service.findActiveRound(patientId).getRoundNumber());
    }

    @Test
    void findActiveRoundWithoutJourneyThrowsNoActiveJourney() {
        int patientId = TestData.newPatient();
        assertThrows(NoActiveJourneyException.class, () -> new RoundService(TestData.pool()).findActiveRound(patientId));
    }

    @Test
    void getRoundsWithoutJourneyReturnsEmptyList() {
        int patientId = TestData.newPatient();
        assertTrue(new RoundService(TestData.pool()).getRounds(patientId).isEmpty());
    }

    @Test
    void getRoundsContainsBothFinishedAndActiveRound() {
        int patientId = TestData.newPatientWithRound();
        new RoundService(TestData.pool()).endRound(patientId, null);
        new RoundService(TestData.pool()).startRound(patientId, "FET", "2026-10-01");
        assertEquals(2, new RoundService(TestData.pool()).getRounds(patientId).size());
        assertEquals(RoundStatus.COMPLETED, new RoundService(TestData.pool()).getRounds(patientId).get(0).getStatus());
        assertEquals(RoundStatus.IN_PROGRESS, new RoundService(TestData.pool()).getRounds(patientId).get(1).getStatus());
    }

    @Test
    void startRoundWithInvalidDateReturnsInvalidInput() {
        int patientId = TestData.newPatientWithJourney();
        assertEquals(ServiceResult.INVALID_INPUT, new RoundService(TestData.pool()).startRound(patientId, "IVF", "10-09-2026"));
    }

    @Test
    void roundsFromEndedJourneyAreStillInHistory() {
        int patientId = TestData.newPatientWithRound();          // forløb 1 med en runde
        RoundService rounds = new RoundService(TestData.pool());
        DashboardService journeys = new DashboardService(TestData.pool());
        rounds.endRound(patientId, null);
        journeys.endJourney(patientId);                          // forløb 1 afsluttet
        journeys.createJourney(patientId, java.time.LocalDate.now().toString());   // forløb 2 (aktivt, ingen runder)
        var perJourney = rounds.getRoundsPerJourney(patientId);
        assertEquals(2, perJourney.size());                      // begge forløb er med
        int total = 0;
        for (var list : perJourney.values()) {
            total += list.size();
        }
        assertEquals(1, total);                                  // runden fra det afsluttede forløb kan stadig ses
    }

    @Test
    void findRoundOnlyFindsOwnRounds() {
        int anna = TestData.newPatientWithRound();
        int maria = TestData.newPatientWithRound();
        RoundService rounds = new RoundService(TestData.pool());
        int annasRound = rounds.getRounds(anna).get(0).getId();
        assertNotNull(rounds.findRound(anna, annasRound));
        assertNull(rounds.findRound(maria, annasRound));         // Maria må ikke se Annas runde
    }

    @Test
    void getActiveRoundStartReturnsStartDateOrNull() {
        int withRound = TestData.newPatientWithRound();       // runden startede 2026-09-10
        int withoutRound = TestData.newPatientWithJourney();
        RoundService service = new RoundService(TestData.pool());
        assertEquals(java.time.LocalDate.of(2026, 9, 10), service.getActiveRoundStart(withRound));
        assertNull(service.getActiveRoundStart(withoutRound));
    }
}
