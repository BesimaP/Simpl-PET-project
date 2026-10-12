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

// SQL for tabellen notification (US12). Påmindelserne laves af systemet (fx dagens medicin ved login), ikke af patienten.
public class NotificationMapper {
    private ConnectionPool connectionPool;

    public NotificationMapper(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    // gemmer påmindelsen og returnerer det nye id
    public int save(Notification notification) {
        String sql = "INSERT INTO notification (patient_id, date_time, notification_type_id, title, message, is_read) "
                + "VALUES (?, ?, (SELECT id FROM notification_type WHERE name = ?), ?, ?, ?)";
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, notification.getPatientId());
            statement.setObject(2, notification.getDateTime());
            statement.setString(3, notification.getNotificationType().name());      // id'et slås op i notification_type
            statement.setString(4, notification.getTitle());
            statement.setString(5, notification.getMessage());
            statement.setBoolean(6, notification.isRead());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    notification.setId(keys.getInt(1));
                }
            }
            return notification.getId();

        } catch (SQLException e) {
            throw new DatabaseException("Could not save notification", e);
        }
    }

    // nyeste først til notifikationer-siden
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
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapRow(rs));
                }
                return notifications;
            }

        } catch (SQLException e) {
            throw new DatabaseException("Could not find notifications for patient " + patientId, e);
        }
    }

    // antal ulæste – bruges til prikken på klokken i topbaren
    public int countUnread(int patientId) {
        String sql = "SELECT COUNT(*) FROM notification WHERE patient_id = ? AND is_read = FALSE";
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, patientId);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();               // COUNT giver altid én række
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not count unread notifications for patient " + patientId, e);
        }
    }

    // AND patient_id = ? så man kun kan markere sine egne påmindelser (UC7)
    public boolean markRead(int id, int patientId) {
        String sql = "UPDATE notification SET is_read = TRUE WHERE id = ? AND patient_id = ?";
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            statement.setInt(2, patientId);
            return statement.executeUpdate() > 0;   // 0 rækker = findes ikke eller ikke hendes
        } catch (SQLException e) {
            throw new DatabaseException("Could not mark notification " + id + " as read", e);
        }
    }

    // "Markér alle som læst"
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

    private Notification mapRow(ResultSet rs) throws SQLException {
        return new Notification(
                rs.getInt("id"),
                rs.getInt("patient_id"),
                rs.getObject("date_time", LocalDateTime.class),
                NotificationType.valueOf(rs.getString("notification_type")),
                rs.getString("title"),
                rs.getString("message"),
                rs.getBoolean("is_read")
        );
    }
}
