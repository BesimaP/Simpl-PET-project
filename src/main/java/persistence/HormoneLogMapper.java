package persistence;

import exceptions.DatabaseException;

import enums.HormoneType;
import entities.HormoneLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// SQL for tabellen hormone_log (US9).
public class HormoneLogMapper {
    private ConnectionPool connectionPool;

    public HormoneLogMapper(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    // gemmer målingen og returnerer det nye id
    public int save(HormoneLog log) {
        String sql = "INSERT INTO hormone_log (round_id, date_time, hormone_type_id, value, unit) VALUES (?, ?, (SELECT id FROM hormone_type WHERE name = ?), ?, ?)";
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, log.getRoundId());
            statement.setObject(2, log.getDateTime());
            statement.setString(3, log.getHormoneType().name());  // id'et slås op i hormone_type
            statement.setDouble(4, log.getValue());
            statement.setString(5, log.getUnit());

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    log.setId(keys.getInt(1));
                }
            }
            return log.getId();

        } catch (SQLException e) {
            throw new DatabaseException("Could not save hormone log", e);
        }
    }

    // nyeste først – bruges til listen og kurven på hormoner-siden
    public List<HormoneLog> findByRound(int roundId) {
        String sql = "SELECT hormone_log.*, hormone_type.name AS hormone_type "
                   + "FROM hormone_log "
                   + "JOIN hormone_type ON hormone_type.id = hormone_log.hormone_type_id "
                   + "WHERE hormone_log.round_id = ? "
                   + "ORDER BY hormone_log.date_time DESC";
        List<HormoneLog> logs = new ArrayList<>();
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
            throw new DatabaseException("Could not find hormone logs for round " + roundId, e);
        }
    }

    // hormone_log har ikke selv patient_id, så vi finder patientens runder via fertility_journey.
    // Bruges efter "WHERE id = ? AND", så man kun kan ramme sine egne målinger
    private static final String OWN_ROUNDS = "round_id IN (SELECT round.id FROM round "
            + "JOIN fertility_journey ON fertility_journey.id = round.fertility_journey_id "
            + "WHERE fertility_journey.patient_id = ?)";

    // sletter kun, hvis målingen ligger i en af patientens egne runder
    public boolean delete(int id, int patientId) {
        String sql = "DELETE FROM hormone_log WHERE id = ? AND " + OWN_ROUNDS;
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setInt(2, patientId);
            return statement.executeUpdate() > 0;   // 0 rækker = findes ikke eller ikke hendes
        } catch (SQLException e) {
            throw new DatabaseException("Could not delete hormone log " + id, e);
        }
    }

    private HormoneLog mapRow(ResultSet rs) throws SQLException {
        return new HormoneLog(
                rs.getInt("id"),
                rs.getInt("round_id"),
                rs.getObject("date_time", LocalDateTime.class),
                HormoneType.valueOf(rs.getString("hormone_type")),
                rs.getDouble("value"),
                rs.getString("unit")
        );
    }
}
