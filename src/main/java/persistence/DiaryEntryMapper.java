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

// Al SQL for tabellen diary_entry (US4). Noterne hænger på patienten – ikke på forløb eller runde.
public class DiaryEntryMapper {
    private ConnectionPool connectionPool; // nøgleringen (gives med udefra)

    public DiaryEntryMapper(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    // gemmer én dagbogsnote og returnerer det id, databasen gav den
    public int save(DiaryEntry entry) {
        String sql = "INSERT INTO diary_entry (patient_id, date_time, title, content) VALUES (?, ?, ?, ?)";
        // try ( … ): lån en forbindelse fra nøgleringen – den afleveres automatisk, når blokken er færdig
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, entry.getPatientId());
            statement.setObject(2, entry.getDateTime()); // LocalDateTime -> TIMESTAMP i PostgreSQL
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

    // henter alle patientens noter, nyeste først – til listen "Tidligere noter" på dagbog-siden
    public List<DiaryEntry> findByPatient(int patientId) {
        String sql = "SELECT * FROM diary_entry WHERE patient_id = ? ORDER BY date_time DESC";
        List<DiaryEntry> entries = new ArrayList<>();
        // try ( … ): lån en forbindelse fra nøgleringen – den afleveres automatisk, når blokken er færdig
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, patientId);
            try (ResultSet rs = statement.executeQuery()) {
                // én runde i løkken = én række i databasen
                while (rs.next()) {
                    entries.add(mapRow(rs));
                }
                return entries;
            }

        } catch (SQLException e) {
            throw new DatabaseException("Could not find diary entries for patient " + patientId, e);
        }
    }

    // sletter én note – men kun hvis den er patientens egen (AND patient_id = ?). true = slettet
    public boolean delete(int id, int patientId) {
        String sql = "DELETE FROM diary_entry WHERE id = ? AND patient_id = ?";
        // try ( … ): lån en forbindelse fra nøgleringen – den afleveres automatisk, når blokken er færdig
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setInt(2, patientId);
            return statement.executeUpdate() > 0;   // antal rækker, der blev ramt: 0 = ikke fundet / ikke hendes
        } catch (SQLException e) {
            throw new DatabaseException("Could not delete diary entry " + id, e);
        }
    }

    // én række fra databasen -> ét DiaryEntry-objekt
    private DiaryEntry mapRow(ResultSet rs) throws SQLException {
        return new DiaryEntry(
                rs.getInt("id"),
                rs.getInt("patient_id"),
                rs.getObject("date_time", LocalDateTime.class), // TIMESTAMP -> LocalDateTime
                rs.getString("title"),
                rs.getString("content")
        );
    }
}
