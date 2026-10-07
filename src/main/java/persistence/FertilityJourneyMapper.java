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
            String sql = "INSERT INTO fertility_journey (patient_id, start_date, journey_status_id) VALUES (?, ?, ?)";
            try (Connection connection = connectionPool.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, journey.getPatientId());
                statement.setObject(2, journey.getStartDate()); // LocalDate -> DATE
                statement.setInt(3, 1);       // 1 = ACTIVE i tabellen journey_status
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
            // journey_status_id = 1 betyder ACTIVE (se tabellen journey_status).
            // Tallet står direkte i SQL'en, fordi det aldrig ændrer sig – patient_id er et ?, fordi det gør.
            String sql = "SELECT * FROM fertility_journey WHERE patient_id = ? AND journey_status_id = 1";

            try (Connection connection = connectionPool.getConnection();
                // gør SQL'en klar til at køre
                PreparedStatement statement = connection.prepareStatement(sql)){
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
                            JourneyStatus.ACTIVE);
                }
                return null; // ingen række = patienten har intet aktivt forløb

            } catch (SQLException e) {
                // e sendes med, så den rigtige databasefejl kan ses bagved vores egen besked
                throw new DatabaseException("Could not find active journey for patient " + patientId, e);
            }
        }

    }




