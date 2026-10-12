package persistence;

import exceptions.DatabaseException;

import entities.DiaryEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// SQL for tabellen diary_entry (US4). Noterne hører til patienten og ikke til et forløb eller en runde.
public class DiaryEntryMapper {
    private ConnectionPool connectionPool;

    public DiaryEntryMapper(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    // gemmer noten og returnerer det nye id
    public int save(DiaryEntry entry) {
        String sql = "INSERT INTO diary_entry (patient_id, date_time, title, content) VALUES (?, ?, ?, ?)";
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, entry.getPatientId());
            statement.setObject(2, entry.getDateTime());
            statement.setString(3, entry.getTitle());
            statement.setString(4, entry.getContent());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    entry.setId(keys.getInt(1));
                }
            }
            return entry.getId();

        } catch (SQLException e) {
            throw new DatabaseException("Could not save diary entry", e);
        }
    }

    // nyeste først til "Tidligere noter" på dagbog-siden
    public List<DiaryEntry> findByPatient(int patientId) {
        String sql = "SELECT * FROM diary_entry WHERE patient_id = ? ORDER BY date_time DESC";
        List<DiaryEntry> entries = new ArrayList<>();
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, patientId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    entries.add(mapRow(rs));
                }
                return entries;
            }

        } catch (SQLException e) {
            throw new DatabaseException("Could not find diary entries for patient " + patientId, e);
        }
    }

    // AND patient_id = ? så man kun kan slette sine egne noter
    public boolean delete(int id, int patientId) {
        String sql = "DELETE FROM diary_entry WHERE id = ? AND patient_id = ?";
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setInt(2, patientId);
            return statement.executeUpdate() > 0;   // 0 rækker = findes ikke eller ikke hendes
        } catch (SQLException e) {
            throw new DatabaseException("Could not delete diary entry " + id, e);
        }
    }

    private DiaryEntry mapRow(ResultSet rs) throws SQLException {
        return new DiaryEntry(
                rs.getInt("id"),
                rs.getInt("patient_id"),
                rs.getObject("date_time", LocalDateTime.class),
                rs.getString("title"),
                rs.getString("content")
        );
    }
}
