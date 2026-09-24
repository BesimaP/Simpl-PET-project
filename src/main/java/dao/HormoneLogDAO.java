package dao;

import enums.HormoneType;
import entities.HormoneLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// DAO = Data Access Object. Det eneste sted, der taler med tabellen hormone_log (US9).
// Resten af programmet kalder bare save/findByRound/delete og behøver ikke kende SQL.
public class HormoneLogDAO {
    // forbindelsen til databasen – vi får den udefra, så alle DAO'er deler den samme
    private Connection connection;

    public HormoneLogDAO(Connection connection) {
        this.connection = connection;
    }

    // gemmer én hormonmåling og returnerer det id, databasen gav rækken
    public int save(HormoneLog log) {
        // id er ikke med – databasen laver det selv. ? = pladsholdere, der fyldes ud nedenfor
        String sql = "INSERT INTO hormone_log (round_id, date_time, hormone_type, value, unit) VALUES (?, ?, ?, ?, ?)";
        try {
            // RETURN_GENERATED_KEYS = vi vil gerne have det nye id tilbage bagefter
            PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);

            // fyld de fem ? ud – nummeret er rækkefølgen i sql-strengen (1 = første ?)
            statement.setInt(1, log.getRoundId());
            statement.setString(2, log.getDateTime().toString()); // LocalDateTime -> "2026-09-14T10:30" (PostgreSQL: setTimestamp)
            statement.setString(3, log.getHormoneType().name());  // enum -> "FSH" (skal matche CHECK i schema.sql)
            statement.setDouble(4, log.getValue());               // REAL i databasen = double i Java
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
            throw new RuntimeException("Could not save hormone log", e);
        }
    }

    // henter alle målinger i én runde, nyeste først – bruges til listen (og senere kurven) på hormoner-siden
    public List<HormoneLog> findByRound(int roundId) {
        String sql = "SELECT * FROM hormone_log WHERE round_id = ? ORDER BY date_time DESC";
        List<HormoneLog> logs = new ArrayList<>(); // tom liste, som vi fylder op
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, roundId);

            // executeQuery = SELECT (giver rækker tilbage). executeUpdate = INSERT/UPDATE/DELETE
            ResultSet rs = statement.executeQuery();

            // rs.next() hopper til næste række – false når der ikke er flere
            while (rs.next()) {
                logs.add(mapRow(rs)); // lav rækken om til et objekt og læg det i listen
            }
            return logs;

        } catch (SQLException e) {
            throw new RuntimeException("Could not find hormone logs for round " + roundId, e);
        }
    }

    // sletter én måling ud fra dens id
    public void delete(int id) {
        String sql = "DELETE FROM hormone_log WHERE id = ?";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete hormone log " + id, e);
        }
    }

    // oversætter én række fra databasen til et HormoneLog-objekt – den modsatte vej af save
    private HormoneLog mapRow(ResultSet rs) throws SQLException {
        return new HormoneLog(
                rs.getInt("id"),
                rs.getInt("round_id"),
                LocalDateTime.parse(rs.getString("date_time")),      // "2026-09-14T10:30" -> LocalDateTime
                HormoneType.valueOf(rs.getString("hormone_type")),  // "FSH" -> enum
                rs.getDouble("value"),
                rs.getString("unit")
        );
    }
}
