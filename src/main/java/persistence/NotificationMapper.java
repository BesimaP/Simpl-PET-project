package persistence;

import exceptions.DatabaseException;

import enums.NotificationType;
import entities.Notification;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Al SQL for tabellen notification (US12). Påmindelser laves af systemet (fx dagens medicin ved login), ikke af patienten.
public class NotificationMapper {
    private ConnectionPool connectionPool; // nøgleringen (gives med udefra)

    public NotificationMapper(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    // gemmer én påmindelse og returnerer det id, databasen gav den
    public int save(Notification notification) {
        String sql = "INSERT INTO notification (patient_id, date_time, notification_type_id, title, message, is_read) "
                + "VALUES (?, ?, (SELECT id FROM notification_type WHERE name = ?), ?, ?, ?)";
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, notification.getPatientId());
            statement.setObject(2, notification.getDateTime());          // LocalDateTime -> tekst (PostgreSQL: setTimestamp)
            statement.setString(3, notification.getNotificationType().name());      // enum -> "MEDICATION_REMINDER"
            statement.setString(4, notification.getTitle());
            statement.setString(5, notification.getMessage());
            statement.setBoolean(6, notification.isRead());                         // boolean -> TRUE/FALSE i PostgreSQL
            statement.executeUpdate();

            ResultSet keys = statement.getGeneratedKeys();
            if (keys.next()) {
                notification.setId(keys.getInt(1));
            }
            return notification.getId();

        } catch (SQLException e) {
            throw new DatabaseException("Could not save notification", e);
        }
    }

    // henter alle patientens påmindelser, nyeste først – til notifikationer-siden
    public List<Notification> findByPatient(int patientId) {
        String sql = "SELECT notification.*, notification_type.name AS notification_type "
                + "FROM notification "
                + "JOIN notification_type ON notification_type.id = notification.notification_type_id "
                + "WHERE notification.patient_id = ? "
                + "ORDER BY notification.date_time DESC";
        List<Notification> notifications = new ArrayList<>();
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, patientId);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                notifications.add(mapRow(rs));
            }
            return notifications;

        } catch (SQLException e) {
            throw new DatabaseException("Could not find notifications for patient " + patientId, e);
        }
    }

    // tæller ulæste påmindelser – til den lille prik på klokken i topbaren
    public int countUnread(int patientId) {
        String sql = "SELECT COUNT(*) FROM notification WHERE patient_id = ? AND is_read = FALSE";
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, patientId);
            ResultSet rs = statement.executeQuery();
            rs.next();               // COUNT giver altid præcis én række
            return rs.getInt(1);     // første (og eneste) kolonne = tallet
        } catch (SQLException e) {
            throw new DatabaseException("Could not count unread notifications for patient " + patientId, e);
        }
    }

    // sætter is_read = TRUE på én påmindelse (UC7: markér som læst)
    public void markRead(int id) {
        String sql = "UPDATE notification SET is_read = TRUE WHERE id = ?";
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not mark notification " + id + " as read", e);
        }
    }

    // sætter is_read = TRUE på ALLE patientens påmindelser ("Markér alle som læst")
    public void markAllRead(int patientId) {
        String sql = "UPDATE notification SET is_read = TRUE WHERE patient_id = ?";
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, patientId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not mark notifications as read for patient " + patientId, e);
        }
    }

    // én række fra databasen -> ét Notification-objekt
    private Notification mapRow(ResultSet rs) throws SQLException {
        return new Notification(
                rs.getInt("id"),
                rs.getInt("patient_id"),
                rs.getObject("date_time", LocalDateTime.class),  // TIMESTAMP -> LocalDateTime
                NotificationType.valueOf(rs.getString("notification_type")),
                rs.getString("title"),
                rs.getString("message"),
                rs.getBoolean("is_read")    // TRUE/FALSE -> boolean
        );
    }
}
