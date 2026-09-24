package services;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Unit tests af TimelineService (tidslinje, US2)
class TimelineServiceTest {

    @BeforeAll
    static void setup() {
        TestData.freshDatabase();
    }

    @Test
    void getEventsWithoutJourneyReturnsEmptyList() {
        int patientId = TestData.newPatient();
        // intet forløb er ikke en fejl – bare ingenting at vise
        assertTrue(new TimelineService().getEvents(patientId).isEmpty());
    }

    @Test
    void getEventsWithoutRoundReturnsEmptyList() {
        int patientId = TestData.newPatientWithJourney();
        assertTrue(new TimelineService().getEvents(patientId).isEmpty());
    }

    @Test
    void getEventsWithNewRoundReturnsEmptyList() {
        int patientId = TestData.newPatientWithRound();
        // TODO: når services opretter Events (fx ved start runde), skal denne forvente 1 i stedet for 0
        assertEquals(0, new TimelineService().getEvents(patientId).size());
    }
}
