package dao;

import entities.FertilityJourney;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// Al SQL for tabellen fertility_journey
public class FertilityJourneyDAO {

    private Connection connection;

    public FertilityJourneyDAO(Connection connection) {
        this.connection = connection;
    }

    // Gemmer et nyt forløb og returnerer det id, databasen gav det
    public int save(FertilityJourney journey) {
        String sql = "INSERT INTO fertility_journey (patient_id, start_date, status) VALUES (?, ?, ?)";
        try {
            PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            statement.setInt(1, journey.getPatientId());
            statement.setString(2, journey.getStartDate().toString()); // LocalDate -> "2026-08-28"
            statement.setString(3, journey.getStatus().name());        // enum -> "ACTIVE"
            statement.executeUpdate();

            ResultSet keys = statement.getGeneratedKeys();
            if (keys.next()) {
                journey.setId(keys.getInt(1));
            }
            return journey.getId();

        } catch (SQLException e) {
            throw new RuntimeException("Could not save fertility journey", e);
        }
    }
}
