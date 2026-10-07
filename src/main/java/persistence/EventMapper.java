package persistence;

import entities.Event;
import enums.EventType;
import exceptions.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventMapper {

    private final ConnectionPool connectionPool;

    public EventMapper(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    // gemmer ét trin og returnerer det id, databasen gav det
    public int save(Event event) {
        String sql = "INSERT INTO event (round_id, date_time, event_type_id, description) VALUES (?, ?, (SELECT id FROM event_type WHERE name = ?), ?)";
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, event.getRoundId());
            statement.setObject(2, event.getDateTime());
            statement.setString(3, event.getEventType().name());        // ordet, fx "EGG_RETRIEVAL" -> databasen finder selv id'et
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
        String sql = "SELECT event.*, event_type.name AS event_type "
                + "FROM event "
                + "JOIN event_type ON event.event_type_id = event_type.id "
                + "WHERE event.round_id = ? "
                + "ORDER BY event.date_time";
        List<Event> events = new ArrayList<>();
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
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
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
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
                rs.getObject("date_time", LocalDateTime.class),
                EventType.valueOf(rs.getString("event_type")),
                rs.getString("description")
        );
    }
}


