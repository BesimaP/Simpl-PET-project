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

// Mapper = arkivaren. Det eneste sted, der taler med tabellen hormone_log (US9).
// Resten af programmet kalder bare save/findByRound/delete og behøver ikke kende SQL.
public class HormoneLogMapper {
    // nøgleringen – vi låner en nøgle, hver gang vi skal i databasen
    private ConnectionPool connectionPool;

    public HormoneLogMapper(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    // gemmer én hormonmåling og returnerer det id, databasen gav rækken
    public int save(HormoneLog log) {
        // id er ikke med – databasen laver det selv. ? = pladsholdere, der fyldes ud nedenfor
        String sql = "INSERT INTO hormone_log (round_id, date_time, hormone_type_id, value, unit) VALUES (?, ?, (SELECT id FROM hormone_type WHERE name = ?), ?, ?)";
        try (Connection connection = connectionPool.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            // fyld de fem ? ud – nummeret er rækkefølgen i sql-strengen (1 = første ?)
            statement.setInt(1, log.getRoundId());
            statement.setObject(2, log.getDateTime()); // LocalDateTime direkte – PostgreSQL forstår selv datoen
            statement.setString(3, log.getHormoneType().name());  // ordet, fx "FSH" -> databasen finder selv id'et
            statement.setDouble(4, log.getValue());               // NUMERIC i databasen = double i Java
            statement.setString(5, log.getUnit());

            // kør INSERT'en
            statement.executeUpdate();

            // hent det id, databasen lige har givet rækken, og læg det på objektet
            ResultSet keys = statement.getGeneratedKeys();
            if (keys.next()) {
                log.setId(keys.getInt(1));
            }
            return log.getId();

        } catch (SQLException e) {
            // går SQL'en galt, stopper vi med en fejl, der fortæller hvad der skete
            throw new DatabaseException("Could not save hormone log", e);
        }
    }

    // henter alle målinger i én runde, nyeste først – bruges til listen (og senere kurven) på hormoner-siden
    public List<HormoneLog> findByRound(int roundId) {
        String sql = "SELECT hormone_log.*, hormone_type.name AS hormone_type "
                   + "FROM hormone_log "
                   + "JOIN hormone_type ON hormone_type.id = hormone_log.hormone_type_id "
                   + "WHERE hormone_log.round_id = ? "
                   + "ORDER BY hormone_log.date_time DESC";
        List<HormoneLog> logs = new ArrayList<>(); // tom liste, som vi fylder op
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)){

            statement.setInt(1, roundId);

            // executeQuery = SELECT (giver rækker tilbage). executeUpdate = INSERT/UPDATE/DELETE
            ResultSet rs = statement.executeQuery();

            // rs.next() hopper til næste række – false når der ikke er flere
            while (rs.next()) {
                logs.add(mapRow(rs)); // lav rækken om til et objekt og læg det i listen
            }
            return logs;

        } catch (SQLException e) {
            throw new DatabaseException("Could not find hormone logs for round " + roundId, e);
        }
    }

    // sletter én måling ud fra dens id
    public void delete(int id) {
        String sql = "DELETE FROM hormone_log WHERE id = ?";
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)){
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not delete hormone log " + id, e);
        }
    }

    // oversætter én række fra databasen til et HormoneLog-objekt – den modsatte vej af save
    private HormoneLog mapRow(ResultSet rs) throws SQLException {
        return new HormoneLog(
                rs.getInt("id"),
                rs.getInt("round_id"),
                rs.getObject("date_time", LocalDateTime.class),   // PostgreSQL giver selv en LocalDateTime
                HormoneType.valueOf(rs.getString("hormone_type")),  // "FSH" -> enum
                rs.getDouble("value"),
                rs.getString("unit")
        );
    }
}
