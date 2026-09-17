package dao;

import entities.MedicationLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Al SQL for tabellen medication_log (US8). Én række = én planlagt dosis i en runde, og om den er taget.
public class MedicationLogDAO {
    private Connection connection;

    public MedicationLogDAO(Connection connection) {
        this.connection = connection;
    }

    // gemmer én dosis og returnerer det id, databasen gav den
    public int save(MedicationLog log) {
        String sql = "INSERT INTO medication_log (round_id, medication_id, scheduled_date_time, dose, unit, taken) VALUES (?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            statement.setInt(1, log.getRoundId());
            statement.setInt(2, log.getMedicationId());                      // id fra medication-tabellen, ikke navnet
            statement.setString(3, log.getScheduledDateTime().toString());   // LocalDateTime -> tekst (PostgreSQL: setTimestamp)
            statement.setDouble(4, log.getDose());                           // tal, fx 150.0
            statement.setString(5, log.getUnit());                           // fx "IU"
            statement.setInt(6, log.isTaken() ? 1 : 0);                      // boolean -> 0/1 (PostgreSQL: setBoolean)
            statement.executeUpdate();

            ResultSet keys = statement.getGeneratedKeys();
            if (keys.next()) {
                log.setId(keys.getInt(1));
            }
            return log.getId();

        } catch (SQLException e) {
            throw new RuntimeException("Could not save medication log", e);
        }
    }

    // henter alle doser i én runde, ældste først – til listen på medicin-siden og "dagens medicin" på dashboardet
    public List<MedicationLog> findByRound(int roundId) {
        String sql = "SELECT * FROM medication_log WHERE round_id = ? ORDER BY scheduled_date_time";
        List<MedicationLog> logs = new ArrayList<>();
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, roundId);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                logs.add(mapRow(rs));
            }
            return logs;

        } catch (SQLException e) {
            throw new RuntimeException("Could not find medication logs for round " + roundId, e);
        }
    }

    // sætter taken = 1 på én dosis (US8 AC3: "markér som taget"). UPDATE ændrer en række, der allerede findes
    public void markTaken(int id) {
        String sql = "UPDATE medication_log SET taken = 1 WHERE id = ?";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not mark medication log " + id + " as taken", e);
        }
    }

    // sletter én dosis ud fra dens id
    public void delete(int id) {
        String sql = "DELETE FROM medication_log WHERE id = ?";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete medication log " + id, e);
        }
    }

    // én række fra databasen -> ét MedicationLog-objekt
    private MedicationLog mapRow(ResultSet rs) throws SQLException {
        return new MedicationLog(
                rs.getInt("id"),
                rs.getInt("round_id"),
                rs.getInt("medication_id"),
                LocalDateTime.parse(rs.getString("scheduled_date_time")),
                rs.getDouble("dose"),
                rs.getString("unit"),
                rs.getInt("taken") == 1   // 0/1 -> boolean
        );
    }
}
