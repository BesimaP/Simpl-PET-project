package persistence;

import entities.FertilityJourney;
import enums.JourneyStatus;
import exceptions.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class FertilityJourneyMapper {

    private final ConnectionPool connectionPool;

    public FertilityJourneyMapper(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    // Gemmer et nyt forløb og returnerer det id, databasen gav det
    public int save(FertilityJourney journey) {
        String sql = "INSERT INTO fertility_journey (patient_id, start_date, journey_status_id) VALUES (?, ?, (SELECT id FROM journey_status WHERE name = ?))";
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, journey.getPatientId());
            statement.setObject(2, journey.getStartDate()); // LocalDate -> DATE
            statement.setString(3, journey.getStatus().name()); // ordet, fx "ACTIVE" -> databasen finder selv id'et
            statement.executeUpdate();

            ResultSet keys = statement.getGeneratedKeys();
            if (keys.next()) {
                journey.setId(keys.getInt(1));
            }
            return journey.getId();

        } catch (SQLException e) {
            throw new DatabaseException("Could not save fertility journey", e);
        }
    }

    // Finder patientens AKTIVE forløb – returnerer null, hvis der ikke er noget i gang.
    // Bruges af dashboard: "har patienten et forløb, eller skal vi vise 'opret forløb'-skærmen?"
    public FertilityJourney findActiveByPatient(int patientId) {
        // to betingelser: rigtig patient OG status ACTIVE. Afsluttede forløb (COMPLETED) kommer ikke med.
        // JOIN: læg status-kortet ved siden af, så vi kan spørge på ordet "ACTIVE" i stedet for tallet 1
        String sql = "SELECT fertility_journey.* "
                + "FROM fertility_journey "
                + "JOIN journey_status ON journey_status.id = fertility_journey.journey_status_id "
                + "WHERE fertility_journey.patient_id = ? AND journey_status.name = 'ACTIVE'";

        // låner en nøgle fra nøgleringen og gør SQL'en klar til at køre – begge afleveres automatisk bagefter
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            // fyld ? ud med patientens id (setInt, fordi det er et tal)
            statement.setInt(1, patientId);

            // executeQuery = SELECT (giver rækker tilbage). executeUpdate = INSERT/UPDATE/DELETE
            ResultSet rs = statement.executeQuery();

            // if, ikke while: der kan højst være ét aktivt forløb per patient (regel fra US1)
            if (rs.next()) {
                // rækken -> et FertilityJourney-objekt (kortet). Status er altid ACTIVE her (det spurgte vi efter)
                return new FertilityJourney(
                        rs.getInt("id"),
                        rs.getInt("patient_id"),
                        rs.getObject("start_date", LocalDate.class),   // PostgreSQL giver selv en LocalDate
                        JourneyStatus.ACTIVE);
            }
            return null; // ingen række = patienten har intet aktivt forløb

        } catch (SQLException e) {
            // e sendes med, så den rigtige databasefejl kan ses bagved vores egen besked
            throw new DatabaseException("Could not find active journey for patient " + patientId, e);
        }
    }

    // Afslutter et forløb: status sættes til COMPLETED (UPDATE). Bagefter kan patienten oprette et nyt forløb (US1)
    public void endJourney(int id) {
        // statussen slås op i typetabellen journey_status ud fra ordet – samme mønster som i save
        String sql = "UPDATE fertility_journey SET journey_status_id = (SELECT id FROM journey_status WHERE name = ?) WHERE id = ?";
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, JourneyStatus.COMPLETED.name());   // "COMPLETED"
            statement.setInt(2, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Could not end fertility journey " + id, e);
        }
    }
}
