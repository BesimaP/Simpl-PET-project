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

    // gemmer én dosis og returnerer det id, databasen gav den. Enheden gemmes IKKE her – den står på medication (3NF)
    public int save(MedicationLog log) {
        String sql = "INSERT INTO medication_log (round_id, medication_id, scheduled_date_time, dose, taken) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, log.getRoundId());
            statement.setInt(2, log.getMedicationId());                      // id fra medication-tabellen, ikke navnet
            statement.setObject(3, log.getScheduledDateTime());
            statement.setDouble(4, log.getDose());                           // tal, fx 150.0
            statement.setBoolean(5, log.isTaken());                          // boolean -> BOOLEAN i PostgreSQL
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

    // henter alle doser i én runde, ældste først – til listen på medicin-siden og "dagens medicin" på dashboardet.
    // JOIN: enheden hentes fra medication-tabellen (den står kun ét sted)
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

    // runderne, der tilhører patienten: round -> fertility_journey -> patient_id.
    // Sættes bag "WHERE id = ? AND", så databasen selv sikrer, at man kun rammer sine egne rækker
    private static final String OWN_ROUNDS = "round_id IN (SELECT round.id FROM round "
            + "JOIN fertility_journey ON fertility_journey.id = round.fertility_journey_id "
            + "WHERE fertility_journey.patient_id = ?)";

    // sætter taken = TRUE på én dosis (US8 AC3: "markér som taget"). UPDATE ændrer en række, der allerede findes.
    // Kun patientens egen dosis. true = opdateret
    public boolean markTaken(int id, int patientId) {
        String sql = "UPDATE medication_log SET taken = TRUE WHERE id = ? AND " + OWN_ROUNDS;
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setInt(2, patientId);
            return statement.executeUpdate() > 0;   // 0 = ikke fundet / ikke hendes
        } catch (SQLException e) {
            throw new DatabaseException("Could not mark medication log " + id + " as taken", e);
        }
    }

    // sætter taken = FALSE på én dosis (fortryd "markér som taget"). Kun patientens egen dosis. true = opdateret
    public boolean markNotTaken(int id, int patientId) {
        String sql = "UPDATE medication_log SET taken = FALSE WHERE id = ? AND " + OWN_ROUNDS;
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setInt(2, patientId);
            return statement.executeUpdate() > 0;   // 0 = ikke fundet / ikke hendes
        } catch (SQLException e) {
            throw new DatabaseException("Could not mark medication log " + id + " as not taken", e);
        }
    }

    // sletter én dosis – men kun hvis den ligger i en af patientens egne runder. true = slettet
    public boolean delete(int id, int patientId) {
        String sql = "DELETE FROM medication_log WHERE id = ? AND " + OWN_ROUNDS;
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setInt(2, patientId);
            return statement.executeUpdate() > 0;   // antal rækker, der blev ramt: 0 = ikke fundet / ikke hendes
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
                rs.getString("unit"),       // fra medication (JOIN)
                rs.getBoolean("taken")
        );
    }
}


