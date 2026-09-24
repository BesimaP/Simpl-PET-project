package dao;

import exceptions.DatabaseException;

import enums.EventType;
import entities.Event;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Al SQL for tabellen event (US2). Et event er et rigtigt trin i runden (ægudtagning, oplægning …) – det er dem, tidslinjen viser.
public class EventDAO {
    private Connection connection;

    public EventDAO(Connection connection) {
        this.connection = connection;
    }

    // gemmer ét trin og returnerer det id, databasen gav det
    public int save(Event event) {
        String sql = "INSERT INTO event (round_id, date_time, event_type, description) VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            statement.setInt(1, event.getRoundId());
            statement.setString(2, event.getDateTime().toString());     // LocalDateTime -> tekst (PostgreSQL: setTimestamp)
            statement.setString(3, event.getEventType().name());        // enum -> "EGG_RETRIEVAL" (matcher CHECK)
            statement.setString(4, event.getDescription());             // må være null
            statement.executeUpdate();

            ResultSet keys = statement.getGeneratedKeys();
            if (keys.next()) {
                event.setId(keys.getInt(1));
            }
            return event.getId();

        } catch (SQLException e) {
            throw new DatabaseException("Could not save event", e);
        }
    }

    // henter alle trin i én runde i tidsrækkefølge – det ER tidslinjen
    public List<Event> findByRound(int roundId) {
        String sql = "SELECT * FROM event WHERE round_id = ? ORDER BY date_time";
        List<Event> events = new ArrayList<>();
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, roundId);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                events.add(mapRow(rs));
            }
            return events;

        } catch (SQLException e) {
            throw new DatabaseException("Could not find events for round " + roundId, e);
        }
    }

    // sletter ét trin ud fra dets id
    public void delete(int id) {
        String sql = "DELETE FROM event WHERE id = ?";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not delete event " + id, e);
        }
    }

    // én række fra databasen -> ét Event-objekt
    private Event mapRow(ResultSet rs) throws SQLException {
        return new Event(
                rs.getInt("id"),
                rs.getInt("round_id"),
                LocalDateTime.parse(rs.getString("date_time")),
                EventType.valueOf(rs.getString("event_type")),
                rs.getString("description")
        );
    }
}
