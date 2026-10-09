package services;

import entities.Event;
import entities.Round;
import enums.EventType;
import persistence.ConnectionPool;
import persistence.EventMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Forretningslogik for tidslinjen (US2). Kender IKKE Javalin.
// Tidslinjen viser kun – patienten tilføjer ikke selv trin. Systemet opretter dem via addEvent
// (RoundService ved start runde, AppointmentService ved ægudtagning/oplægning/graviditetstest).
public class TimelineService {

    private EventMapper eventMapper;
    private ConnectionPool connectionPool; // gemmes, fordi RoundService laves først, når den skal bruges (RoundService laver selv en TimelineService – ellers ville de lave hinanden i ring)

    public TimelineService(ConnectionPool connectionPool){
        this.eventMapper = new EventMapper(connectionPool);
        this.connectionPool = connectionPool;
    }

    // Lægger ét trin på en runde. Kaldes af andre services, ikke af en controller. description må være null
    public void addEvent(int roundId, LocalDateTime dateTime, EventType type, String description) {
        eventMapper.save(new Event(0, roundId, dateTime, type, description));
    }

    // Henter trinene for én bestemt runde – kun hvis runden er patientens egen (ellers tom liste)
    public List<Event> getEvents(int patientId, int roundId) {
        Round round = new RoundService(connectionPool).findRound(patientId, roundId);
        if (round == null) {
            return new ArrayList<>();
        }
        return eventMapper.findByRound(round.getId());
    }
}
