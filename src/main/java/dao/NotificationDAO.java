package dao;

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
public class NotificationDAO {
    private Connection connection;

    public NotificationDAO(Connection connection) {
        this.connection = connection;
    }

    // gemmer én påmindelse og returnerer det id, databasen gav den
    public int save(Notification notification) {
        String sql = "INSERT INTO notification (patient_id, date_time, notification_type, title, message, is_read) VALUES (?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            statement.setInt(1, notification.getPatientId());
            statement.setString(2, notification.getDateTime().toString());          // LocalDateTime -> tekst (PostgreSQL: setTimestamp)
            statement.setString(3, notification.getNotificationType().name());      // enum -> "MEDICATION_REMINDER"
            statement.setString(4, notification.getTitle());
            statement.setString(5, notification.getMessage());
            statement.setInt(6, notification.isRead() ? 1 : 0);                     // boolean -> 0/1 (PostgreSQL: setBoolean)
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
        String sql = "SELECT * FROM notification WHERE patient_id = ? ORDER BY date_time DESC";
        List<Notification> notifications = new ArrayList<>();
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
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
        String sql = "SELECT COUNT(*) FROM notification WHERE patient_id = ? AND is_read = 0";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, patientId);
            ResultSet rs = statement.executeQuery();
            rs.next();               // COUNT giver altid præcis én række
            return rs.getInt(1);     // første (og eneste) kolonne = tallet
        } catch (SQLException e) {
            throw new DatabaseException("Could not count unread notifications for patient " + patientId, e);
        }
    }

    // sætter is_read = 1 på én påmindelse (UC7: markér som læst)
    public void markRead(int id) {
        String sql = "UPDATE notification SET is_read = 1 WHERE id = ?";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not mark notification " + id + " as read", e);
        }
    }

    // sletter én påmindelse ud fra dens id
    public void delete(int id) {
        String sql = "DELETE FROM notification WHERE id = ?";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not delete notification " + id, e);
        }
    }

    // én række fra databasen -> ét Notification-objekt
    private Notification mapRow(ResultSet rs) throws SQLException {
        return new Notification(
                rs.getInt("id"),
                rs.getInt("patient_id"),
                LocalDateTime.parse(rs.getString("date_time")),
                NotificationType.valueOf(rs.getString("notification_type")),
                rs.getString("title"),
                rs.getString("message"),
                rs.getInt("is_read") == 1   // 0/1 -> boolean
        );
    }
}
