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

    // gemmer dosen og returnerer det nye id. Enheden gemmes ikke her, den står på medication (3NF)
    public int save(MedicationLog log) {
        String sql = "INSERT INTO medication_log (round_id, medication_id, scheduled_date_time, dose, taken) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, log.getRoundId());
            statement.setInt(2, log.getMedicationId());
            statement.setObject(3, log.getScheduledDateTime());
            statement.setDouble(4, log.getDose());
            statement.setBoolean(5, log.isTaken());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    log.setId(keys.getInt(1));
                }
            }
            return log.getId();

        } catch (SQLException e) {
            throw new DatabaseException("Could not save medication log", e);
        }
    }

    // ældste først – til medicin-siden og "dagens medicin" på dashboardet.
    // Enheden hentes med JOIN fra medication
    public List<MedicationLog> findByRound(int roundId) {
        String sql = "SELECT medication_log.*, medication.unit "
                + "FROM medication_log "
                + "JOIN medication ON medication.id = medication_log.medication_id "
                + "WHERE medication_log.round_id = ? ORDER BY medication_log.scheduled_date_time";
        List<MedicationLog> logs = new ArrayList<>();
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, roundId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRow(rs));
                }
                return logs;
            }

        } catch (SQLException e) {
            throw new DatabaseException("Could not find medication logs for round " + roundId, e);
        }
    }

    // medication_log har ikke selv patient_id, så vi finder patientens runder via fertility_journey.
    // Bruges efter "WHERE id = ? AND", så man kun kan ramme sine egne doser
    private static final String OWN_ROUNDS = "round_id IN (SELECT round.id FROM round "
            + "JOIN fertility_journey ON fertility_journey.id = round.fertility_journey_id "
            + "WHERE fertility_journey.patient_id = ?)";

    // "markér som taget" (US8 AC3)
    public boolean markTaken(int id, int patientId) {
        String sql = "UPDATE medication_log SET taken = TRUE WHERE id = ? AND " + OWN_ROUNDS;
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setInt(2, patientId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Could not mark medication log " + id + " as taken", e);
        }
    }

    // fortryder "markér som taget"
    public boolean markNotTaken(int id, int patientId) {
        String sql = "UPDATE medication_log SET taken = FALSE WHERE id = ? AND " + OWN_ROUNDS;
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setInt(2, patientId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Could not mark medication log " + id + " as not taken", e);
        }
    }

    public boolean delete(int id, int patientId) {
        String sql = "DELETE FROM medication_log WHERE id = ? AND " + OWN_ROUNDS;
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setInt(2, patientId);
            return statement.executeUpdate() > 0;   // 0 rækker = findes ikke eller ikke hendes
        } catch (SQLException e) {
            throw new DatabaseException("Could not delete medication log " + id, e);
        }
    }

    private MedicationLog mapRow(ResultSet rs) throws SQLException {
        return new MedicationLog(
                rs.getInt("id"),
                rs.getInt("round_id"),
                rs.getInt("medication_id"),
                rs.getObject("scheduled_date_time", LocalDateTime.class),
                rs.getDouble("dose"),
                rs.getString("unit"),       // fra medication via JOIN
                rs.getBoolean("taken")
        );
    }
}


