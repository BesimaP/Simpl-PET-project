package persistence;

import entities.MedicationLog;
import exceptions.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MedicationLogMapper {

    private final ConnectionPool connectionPool;

    public MedicationLogMapper(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    // gemmer én dosis og returnerer det id, databasen gav den
    public int save(MedicationLog log) {
        String sql = "INSERT INTO medication_log (round_id, medication_id, scheduled_date_time, dose, unit, taken) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, log.getRoundId());
            statement.setInt(2, log.getMedicationId());                      // id fra medication-tabellen, ikke navnet
            statement.setObject(3, log.getScheduledDateTime());
            statement.setDouble(4, log.getDose());                           // tal, fx 150.0
            statement.setString(5, log.getUnit());                           // fx "IU"
            statement.setBoolean(6, log.isTaken());                    // boolean -> BOOLEAN i PostgreSQL
            statement.executeUpdate();

            ResultSet keys = statement.getGeneratedKeys();
            if (keys.next()) {
                log.setId(keys.getInt(1));
            }
            return log.getId();

        } catch (SQLException e) {
            throw new DatabaseException("Could not save medication log", e);
        }
    }

    // henter alle doser i én runde, ældste først – til listen på medicin-siden og "dagens medicin" på dashboardet
    public List<MedicationLog> findByRound(int roundId) {
        String sql = "SELECT * FROM medication_log WHERE round_id = ? ORDER BY scheduled_date_time";
        List<MedicationLog> logs = new ArrayList<>();
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, roundId);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                logs.add(mapRow(rs));
            }
            return logs;

        } catch (SQLException e) {
            throw new DatabaseException("Could not find medication logs for round " + roundId, e);
        }
    }

    // sætter taken = TRUE på én dosis (US8 AC3: "markér som taget"). UPDATE ændrer en række, der allerede findes
    public void markTaken(int id) {
        String sql = "UPDATE medication_log SET taken = TRUE WHERE id = ?";
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not mark medication log " + id + " as taken", e);
        }
    }

    // sætter taken = FALSE på én dosis (fortryd "markér som taget")
    public void markNotTaken(int id) {
        String sql = "UPDATE medication_log SET taken = FALSE WHERE id = ?";
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not mark medication log " + id + " as not taken", e);
        }
    }

    // sletter én dosis ud fra dens id
    public void delete(int id) {
        String sql = "DELETE FROM medication_log WHERE id = ?";
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not delete medication log " + id, e);
        }
    }

    // én række fra databasen -> ét MedicationLog-objekt
    private MedicationLog mapRow(ResultSet rs) throws SQLException {
        return new MedicationLog(
                rs.getInt("id"),
                rs.getInt("round_id"),
                rs.getInt("medication_id"),
                rs.getObject("scheduled_date_time", LocalDateTime.class),
                rs.getDouble("dose"),
                rs.getString("unit"),
                rs.getBoolean("taken")
        );
    }
}


