package dao;

import exceptions.DatabaseException;

import entities.DiaryEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Al SQL for tabellen diary_entry (US4). Noterne hænger på forløbet, ikke på en runde.
public class DiaryEntryDAO {
    private Connection connection;

    public DiaryEntryDAO(Connection connection) {
        this.connection = connection;
    }

    // gemmer én dagbogsnote og returnerer det id, databasen gav den
    public int save(DiaryEntry entry) {
        String sql = "INSERT INTO diary_entry (fertility_journey_id, date_time, title, content) VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            statement.setInt(1, entry.getFertilityJourneyId());
            statement.setString(2, entry.getDateTime().toString()); // LocalDateTime -> tekst (PostgreSQL: setTimestamp)
            statement.setString(3, entry.getTitle());
            statement.setString(4, entry.getContent());
            statement.executeUpdate();

            ResultSet keys = statement.getGeneratedKeys();
            if (keys.next()) {
                entry.setId(keys.getInt(1));
            }
            return entry.getId();

        } catch (SQLException e) {
            throw new DatabaseException("Could not save diary entry", e);
        }
    }

    // henter alle noter på ét forløb, nyeste først – til listen "Seneste noter" på dagbog-siden
    public List<DiaryEntry> findByJourney(int fertilityJourneyId) {
        String sql = "SELECT * FROM diary_entry WHERE fertility_journey_id = ? ORDER BY date_time DESC";
        List<DiaryEntry> entries = new ArrayList<>();
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, fertilityJourneyId);
            ResultSet rs = statement.executeQuery();

            // én runde i løkken = én række i databasen
            while (rs.next()) {
                entries.add(mapRow(rs));
            }
            return entries;

        } catch (SQLException e) {
            throw new DatabaseException("Could not find diary entries for journey " + fertilityJourneyId, e);
        }
    }

    // sletter én note ud fra dens id
    public void delete(int id) {
        String sql = "DELETE FROM diary_entry WHERE id = ?";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not delete diary entry " + id, e);
        }
    }

    // én række fra databasen -> ét DiaryEntry-objekt
    private DiaryEntry mapRow(ResultSet rs) throws SQLException {
        return new DiaryEntry(
                rs.getInt("id"),
                rs.getInt("fertility_journey_id"),
                LocalDateTime.parse(rs.getString("date_time")),
                rs.getString("title"),
                rs.getString("content")
        );
    }
}
