package persistence;

import entities.Round;
import enums.Result;
import enums.RoundStatus;
import enums.TreatmentType;
import exceptions.DatabaseException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RoundMapper {

    private final ConnectionPool connectionPool;

    public RoundMapper(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    // gemmer en ny runde og returnerer det nye id. end_date og result er tomme, indtil runden afsluttes
    public int save(Round round) {
        String sql = "INSERT INTO round (fertility_journey_id, round_number, treatment_type_id, start_date) VALUES (?, ?, (SELECT id FROM treatment_type WHERE name = ?), ?)";
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, round.getFertilityJourneyId());
            statement.setInt(2, round.getRoundNumber());
            statement.setString(3, round.getTreatmentType().name()); // id'et slås op i treatment_type
            statement.setObject(4, round.getStartDate());

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    round.setId(keys.getInt(1));
                }
            }
            return round.getId();

        } catch (SQLException e) {
            throw new DatabaseException("Could not save round", e);
        }
    }

    // runden der er i gang, eller null. Bruges af dashboardet og af de sider, der gemmer noget på runden
    public Round findActiveByJourney(int fertilityJourneyId) {
        // round_overview er et view i schemaet, der også har navnene på behandlingstype og resultat
        String sql = "SELECT * FROM round_overview WHERE fertility_journey_id = ? AND end_date IS NULL";
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, fertilityJourneyId);


            try (ResultSet rs = statement.executeQuery()) {
                // der kan kun være én runde i gang per forløb
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }

        } catch (SQLException e) {
            throw new DatabaseException("Could not find active round for journey " + fertilityJourneyId, e);
        }
    }

    // ældste først – til rundehistorik og til at finde næste rundenummer
    public List<Round> findByJourney(int fertilityJourneyId) {
        String sql = "SELECT * FROM round_overview WHERE fertility_journey_id = ? ORDER BY round_number";
        List<Round> rounds = new ArrayList<>();
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, fertilityJourneyId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rounds.add(mapRow(rs));
                }
                return rounds;
            }

        } catch (SQLException e) {
            throw new DatabaseException("Could not find rounds for journey " + fertilityJourneyId, e);
        }
    }

    // afslutter runden (US10b). Vi har ingen status-kolonne – runden er afsluttet, når end_date er udfyldt
    public void endRound(int id, LocalDate endDate, Result result) {
        String sql = "UPDATE round SET end_date = ?, result_id = (SELECT id FROM result WHERE name = ?) WHERE id = ?";
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, endDate);
            statement.setString(2, result == null ? null : result.name()); // resultat må gerne være tomt
            statement.setInt(3, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not end round " + id, e);
        }
    }

    // end_date og result er NULL, mens runden er i gang, så vi tjekker dem før valueOf
    private Round mapRow(ResultSet rs) throws SQLException {
        LocalDate endDate = rs.getObject("end_date", LocalDate.class);
        String result = rs.getString("result");
        return new Round(
                rs.getInt("id"),
                rs.getInt("fertility_journey_id"),
                rs.getInt("round_number"),
                TreatmentType.valueOf(rs.getString("treatment_type")),
                rs.getObject("start_date", LocalDate.class),
                endDate,
                endDate == null ? RoundStatus.IN_PROGRESS : RoundStatus.COMPLETED,
                result == null ? null : Result.valueOf(result));
    }
}


