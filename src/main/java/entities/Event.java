package entities;

import enums.EventType;
import java.time.LocalDateTime;

// Et konkret trin i en runde, vises på tidslinjen (tabel event, US2/UC11).
public class Event {

    private int id;
    private int roundId;
    private LocalDateTime dateTime;
    private EventType eventType;
    private String description;

    // konstruktør: id = 0 når kortet er nyt (databasen giver det rigtige id ved save)
    public Event(int id, int roundId, LocalDateTime dateTime, EventType eventType, String description) {
        this.id = id;
        this.roundId = roundId;
        this.dateTime = dateTime;
        this.eventType = eventType;
        this.description = description;
    }

    // gettere: læs felterne. setId bruges af DAO'en efter save; resten kan ikke ændres udefra
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRoundId() {
        return roundId;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getDescription() {
        return description;
    }
}
