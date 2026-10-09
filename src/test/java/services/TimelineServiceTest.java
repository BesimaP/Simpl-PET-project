package services;

import enums.EventType;
import entities.Round;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Integrationstests af TimelineService (tidslinje, US2)
class TimelineServiceTest {

    @BeforeAll
    static void setup() {
        TestData.freshDatabase();
    }

    @Test
    void getEventsWithoutJourneyReturnsEmptyList() {
        int patientId = TestData.newPatient();
        // intet forløb er ikke en fejl – bare ingenting at vise
        assertTrue(new TimelineService(TestData.pool()).getEvents(patientId).isEmpty());
    }

    @Test
    void getEventsWithoutRoundReturnsEmptyList() {
        int patientId = TestData.newPatientWithJourney();
        assertTrue(new TimelineService(TestData.pool()).getEvents(patientId).isEmpty());
    }

    @Test
    void getEventsWithNewRoundReturnsStartEvent() {
        int patientId = TestData.newPatientWithRound();
        // start runde opretter automatisk ét trin: STIMULATION_START
        assertEquals(1, new TimelineService(TestData.pool()).getEvents(patientId).size());
    }

    @Test
    void startEventHasTypeStimulationStart() {
        int patientId = TestData.newPatientWithRound();
        assertEquals(EventType.STIMULATION_START, new TimelineService(TestData.pool()).getEvents(patientId).get(0).getEventType());
    }

    @Test
    void addEventAppearsOnTimeline() throws Exception {
        int patientId = TestData.newPatientWithRound();
        Round round = new RoundService(TestData.pool()).findActiveRound(patientId);
        new TimelineService(TestData.pool()).addEvent(round.getId(), LocalDateTime.of(2026, 9, 20, 9, 0), EventType.EMBRYO_TRANSFER, "Vitanova");
        assertEquals(2, new TimelineService(TestData.pool()).getEvents(patientId).size());
    }

    @Test
    void eventsAreSortedByDateOldestFirst() throws Exception {
        int patientId = TestData.newPatientWithRound(); // runden starter 2026-09-10
        Round round = new RoundService(TestData.pool()).findActiveRound(patientId);
        // gemmes i "forkert" rækkefølge – DAO'en sorterer efter date_time
        new TimelineService(TestData.pool()).addEvent(round.getId(), LocalDateTime.of(2026, 9, 30, 9, 0), EventType.PREGNANCY_TEST, null);
        new TimelineService(TestData.pool()).addEvent(round.getId(), LocalDateTime.of(2026, 9, 20, 9, 0), EventType.EGG_RETRIEVAL, null);
        assertEquals(EventType.STIMULATION_START, new TimelineService(TestData.pool()).getEvents(patientId).get(0).getEventType());
        assertEquals(EventType.EGG_RETRIEVAL, new TimelineService(TestData.pool()).getEvents(patientId).get(1).getEventType());
        assertEquals(EventType.PREGNANCY_TEST, new TimelineService(TestData.pool()).getEvents(patientId).get(2).getEventType());
    }

    @Test
    void addEventWithoutDescriptionIsAllowed() throws Exception {
        int patientId = TestData.newPatientWithRound();
        Round round = new RoundService(TestData.pool()).findActiveRound(patientId);
        new TimelineService(TestData.pool()).addEvent(round.getId(), LocalDateTime.of(2026, 9, 25, 9, 0), EventType.FERTILISATION, null);
        assertNull(new TimelineService(TestData.pool()).getEvents(patientId).get(1).getDescription());
    }

    @Test
    void eventsForAnotherPatientsRoundAreNotShown() {
        int anna = TestData.newPatientWithRound();
        int maria = TestData.newPatientWithRound();
        int annasRound = new RoundService(TestData.pool()).getRounds(anna).get(0).getId();
        assertFalse(new TimelineService(TestData.pool()).getEvents(anna, annasRound).isEmpty());   // start-runde-trinnet
        assertTrue(new TimelineService(TestData.pool()).getEvents(maria, annasRound).isEmpty());
    }
}
