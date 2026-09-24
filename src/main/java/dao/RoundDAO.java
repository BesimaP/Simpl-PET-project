package dao;

import exceptions.DatabaseException;

import entities.Round;
import enums.Result;
import enums.RoundStatus;
import enums.TreatmentType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Al SQL for tabellen round (US10a/10b, US2). En runde hører til ét forløb; kun én kan være IN_PROGRESS ad gangen.
public class RoundDAO {
    private Connection connection;

    public RoundDAO(Connection connection) {
        this.connection = connection;
    }

    // Gemmer en ny runde og returnerer det id, databasen gav den. end_date og result er tomme, til runden afsluttes.
    public int save(Round round) {
        String sql = "INSERT INTO round (fertility_journey_id, round_number, treatment_type, start_date, status) VALUES (?, ?, ?, ?, ?)";
        try {
            PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            statement.setInt(1, round.getFertilityJourneyId());
            statement.setInt(2, round.getRoundNumber());
            statement.setString(3, round.getTreatmentType().name()); // enum -> "IVF"
            statement.setString(4, round.getStartDate().toString()); // LocalDate -> "2026-08-28"
            statement.setString(5, round.getStatus().name());        // enum -> "IN_PROGRESS"
            statement.executeUpdate();

            ResultSet keys = statement.getGeneratedKeys();
            if (keys.next()) {
                round.setId(keys.getInt(1)); // objektet får rækkens id
            }
            return round.getId();

        } catch (SQLException e) {
            throw new DatabaseException("Could not save round", e);
        }
    }

    // Finder den runde, der er i gang i et forløb – returnerer null, hvis ingen runde er i gang.
    // Bruges af dashboard ("hvilken runde viser vi?") og af alle sider, der gemmer noget på runden.
    public Round findActiveByJourney(int fertilityJourneyId) {
        String sql = "SELECT * FROM round WHERE fertility_journey_id = ? AND status = ?";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, fertilityJourneyId);
            statement.setString(2, RoundStatus.IN_PROGRESS.name());

            // executeQuery = SELECT (giver rækker tilbage). executeUpdate = INSERT/UPDATE/DELETE
            ResultSet rs = statement.executeQuery();

            // if, ikke while: der kan højst være én runde i gang per forløb
            if (rs.next()) {
                return mapRow(rs); // rækken -> et Round-objekt (kortet)
            }
            return null; // ingen række = ingen runde i gang

        } catch (SQLException e) {
            throw new DatabaseException("Could not find active round for journey " + fertilityJourneyId, e);
        }
    }

    // Henter alle runder i et forløb, ældste først – til rundehistorik og til at finde næste rundenummer
    public List<Round> findByJourney(int fertilityJourneyId) {
        String sql = "SELECT * FROM round WHERE fertility_journey_id = ? ORDER BY round_number";
        List<Round> rounds = new ArrayList<>(); // tom liste, som fyldes op
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, fertilityJourneyId);
            ResultSet rs = statement.executeQuery();

            // while: der kan være mange runder – én tur i løkken per række
            while (rs.next()) {
                rounds.add(mapRow(rs));
            }
            return rounds;

        } catch (SQLException e) {
            throw new DatabaseException("Could not find rounds for journey " + fertilityJourneyId, e);
        }
    }

    // Afslutter en runde: sætter slutdato, resultat og status COMPLETED (US10b). UPDATE ændrer en række, der findes.
    public void endRound(int id, LocalDate endDate, Result result) {
        String sql = "UPDATE round SET end_date = ?, result = ?, status = ? WHERE id = ?";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, endDate.toString());
            statement.setString(2, result == null ? null : result.name()); // resultat må være tomt
            statement.setString(3, RoundStatus.COMPLETED.name());
            statement.setInt(4, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not end round " + id, e);
        }
    }

    // én række fra databasen -> ét Round-objekt. Bruges af begge find-metoder, så oversættelsen kun står ét sted.
    // end_date og result er NULL, mens runden er i gang – derfor tjekkes de, før de oversættes (ellers crasher parse/valueOf)
    private Round mapRow(ResultSet rs) throws SQLException {
        String endDate = rs.getString("end_date");
        String result = rs.getString("result");
        return new Round(
                rs.getInt("id"),
                rs.getInt("fertility_journey_id"),
                rs.getInt("round_number"),
                TreatmentType.valueOf(rs.getString("treatment_type")),
                LocalDate.parse(rs.getString("start_date")),
                endDate == null ? null : LocalDate.parse(endDate),
                RoundStatus.valueOf(rs.getString("status")),
                result == null ? null : Result.valueOf(result));
    }
}
