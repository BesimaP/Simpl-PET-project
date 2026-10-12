package services;

import entities.Event;
import entities.Round;
import enums.EventType;
import persistence.ConnectionPool;
import persistence.EventMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Logik for tidslinjen (US2). Patienten tilføjer ikke selv trin – de oprettes af RoundService og AppointmentService.
public class TimelineService {

    private EventMapper eventMapper;
    private ConnectionPool connectionPool; // RoundService laves først, når den skal bruges – den laver selv en TimelineService, så de ellers ville lave hinanden i ring

    public TimelineService(ConnectionPool connectionPool){
        this.eventMapper = new EventMapper(connectionPool);
        this.connectionPool = connectionPool;
    }

    // Kaldes af andre services, ikke af en controller. description må være null
    public void addEvent(int roundId, LocalDateTime dateTime, EventType type, String description) {
        eventMapper.save(new Event(0, roundId, dateTime, type, description));
    }

    // Trinene for en runde – kun hvis runden er patientens egen, ellers en tom liste
    public List<Event> getEvents(int patientId, int roundId) {
        Round round = new RoundService(connectionPool).findRound(patientId, roundId);
        if (round == null) {
            return new ArrayList<>();
        }
        return eventMapper.findByRound(round.getId());
    }
}
