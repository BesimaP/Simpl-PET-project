package persistence;

import entities.FertilityJourney;
import enums.JourneyStatus;
import exceptions.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FertilityJourneyMapper {

    private final ConnectionPool connectionPool;

    public FertilityJourneyMapper(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    // gemmer et nyt forløb og returnerer det nye id
    public int save(FertilityJourney journey) {
        String sql = "INSERT INTO fertility_journey (patient_id, start_date, journey_status_id) VALUES (?, ?, (SELECT id FROM journey_status WHERE name = ?))";
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, journey.getPatientId());
            statement.setObject(2, journey.getStartDate());
            statement.setString(3, journey.getStatus().name()); // id'et slås op i journey_status
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    journey.setId(keys.getInt(1));
                }
            }
            return journey.getId();

        } catch (SQLException e) {
            throw new DatabaseException("Could not save fertility journey", e);
        }
    }

    // patientens aktive forløb, eller null hvis der ikke er noget i gang.
    // Dashboardet bruger det til at vælge, om det skal vise "opret forløb"
    public FertilityJourney findActiveByPatient(int patientId) {
        // JOIN på journey_status, så vi kan spørge på 'ACTIVE' i stedet for at hardcode id'et
        String sql = "SELECT fertility_journey.* "
                + "FROM fertility_journey "
                + "JOIN journey_status ON journey_status.id = fertility_journey.journey_status_id "
                + "WHERE fertility_journey.patient_id = ? AND journey_status.name = 'ACTIVE'";

        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, patientId);

            try (ResultSet rs = statement.executeQuery()) {
                // if og ikke while: en patient kan kun have ét aktivt forløb ad gangen (US1)
                if (rs.next()) {
                    return new FertilityJourney(
                            rs.getInt("id"),
                            rs.getInt("patient_id"),
                            rs.getObject("start_date", LocalDate.class),
                            JourneyStatus.ACTIVE);
                }
                return null;
            }

        } catch (SQLException e) {
            throw new DatabaseException("Could not find active journey for patient " + patientId, e);
        }
    }

    // både aktive og afsluttede forløb, nyeste først. Bruges af rundehistorik, aftaler og tidslinje,
    // så patienten også kan se gamle forløb
    public List<FertilityJourney> findByPatient(int patientId) {
        String sql = "SELECT fertility_journey.*, journey_status.name AS journey_status "
                + "FROM fertility_journey "
                + "JOIN journey_status ON journey_status.id = fertility_journey.journey_status_id "
                + "WHERE fertility_journey.patient_id = ? "
                + "ORDER BY fertility_journey.start_date DESC, fertility_journey.id DESC";
        List<FertilityJourney> journeys = new ArrayList<>();
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, patientId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    journeys.add(new FertilityJourney(
                            rs.getInt("id"),
                            rs.getInt("patient_id"),
                            rs.getObject("start_date", LocalDate.class),
                            JourneyStatus.valueOf(rs.getString("journey_status"))));
                }
                return journeys;
            }

        } catch (SQLException e) {
            throw new DatabaseException("Could not find journeys for patient " + patientId, e);
        }
    }

    // sætter status til COMPLETED, så patienten kan oprette et nyt forløb (US1)
    public void endJourney(int id) {
        String sql = "UPDATE fertility_journey SET journey_status_id = (SELECT id FROM journey_status WHERE name = ?) WHERE id = ?";
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, JourneyStatus.COMPLETED.name());
            statement.setInt(2, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not end fertility journey " + id, e);
        }
    }
}
