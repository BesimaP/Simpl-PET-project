package dao;

import entities.FertilityJourney;
import enums.JourneyStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

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

    // Finder patientens AKTIVE forløb – returnerer null, hvis der ikke er noget i gang.
    // Bruges af dashboard: "har patienten et forløb, eller skal vi vise 'opret forløb'-skærmen?"
    public FertilityJourney findActiveByPatient(int patientId) {
        // to betingelser: rigtig patient OG status ACTIVE. Afsluttede forløb (COMPLETED) kommer ikke med.
        // 'ACTIVE' står direkte i SQL'en, fordi det aldrig ændrer sig – patient_id er et ?, fordi det gør.
        String sql = "SELECT * FROM fertility_journey WHERE patient_id = ? AND status = 'ACTIVE'";

        try {
            // gør SQL'en klar til at køre
            PreparedStatement statement = connection.prepareStatement(sql);
            // fyld ? ud med patientens id (setInt, fordi det er et tal)
            statement.setInt(1, patientId);

            // executeQuery = SELECT (giver rækker tilbage). executeUpdate = INSERT/UPDATE/DELETE
            ResultSet rs = statement.executeQuery();

            // if, ikke while: der kan højst være ét aktivt forløb per patient (regel fra US1)
            if (rs.next()) {
                // rækken -> et FertilityJourney-objekt (kortet). Tekst i databasen oversættes tilbage:
                // "2026-08-28" -> LocalDate, "ACTIVE" -> enum JourneyStatus
                return new FertilityJourney(
                        rs.getInt("id"),
                        rs.getInt("patient_id"),
                        LocalDate.parse(rs.getString("start_date")),
                        JourneyStatus.valueOf(rs.getString("status")));
            }
            return null; // ingen række = patienten har intet aktivt forløb

        } catch (SQLException e) {
            // e sendes med, så den rigtige databasefejl kan ses bagved vores egen besked
            throw new RuntimeException("Could not find active journey for patient " + patientId, e);
        }
    }

}
