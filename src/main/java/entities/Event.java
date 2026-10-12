package entities;

import enums.EventType;
import java.time.LocalDateTime;

// Et trin i en runde, som vises på tidslinjen (tabel event, US2/UC11).
public class Event {

    private int id;
    private int roundId;
    private LocalDateTime dateTime;
    private EventType eventType;
    private String description;

    // id = 0 for en ny række – databasen giver det rigtige id ved save
    public Event(int id, int roundId, LocalDateTime dateTime, EventType eventType, String description) {
        this.id = id;
        this.roundId = roundId;
        this.dateTime = dateTime;
        this.eventType = eventType;
        this.description = description;
    }

    // setId bruges af mapperen efter save. Resten kan kun læses.
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
