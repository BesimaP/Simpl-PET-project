package persistence;

import exceptions.DatabaseException;

import entities.Patient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;


// Al SQL for tabellen patient. Arkivaren: den eneste, der taler SQL med patient-skuffen
public class PatientMapper {

    private ConnectionPool connectionPool; // nøgleringen (gives med udefra)

    public PatientMapper(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    // Gemmer en ny patient og returnerer det id, databasen gav den
    public int save(Patient patient) {
        String sql = "INSERT INTO patient (username, password_hash, first_name, last_name, date_of_birth) VALUES (?, ?, ?, ?, ?)";

        // try ( … ): lån en forbindelse fra nøgleringen – den afleveres automatisk, når blokken er færdig
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, patient.getUsername());
            statement.setString(2, patient.getPasswordHash());
            statement.setString(3, patient.getFirstName());
            statement.setString(4, patient.getLastName());
            statement.setObject(5, patient.getDateOfBirth()); // LocalDate -> DATE i PostgreSQL
            statement.executeUpdate();

            // RETURN_GENERATED_KEYS: databasen sender det nye id tilbage
            ResultSet keys = statement.getGeneratedKeys();
            if (keys.next()) {
                patient.setId(keys.getInt(1));
            }
            return patient.getId();

        } catch (SQLException e) {
            throw new DatabaseException("Could not save patient", e);
        }
    }

    // Finder én patient ud fra brugernavnet – returnerer null, hvis den ikke findes (bruges ved login og opret profil)
    public Patient findByUsername(String username) {
        // ? = pladsholder for brugernavnet, som sættes nedenfor (aldrig lim tekst ind i SQL-strengen selv)
        String sql = "SELECT * FROM patient WHERE username = ?";

        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            // fyld ? ud – setString, fordi username er tekst
            statement.setString(1, username);

            // executeQuery = SELECT (giver rækker tilbage). executeUpdate = INSERT/UPDATE/DELETE
            ResultSet rs = statement.executeQuery();

            // if, ikke while: der kan højst være én række, fordi username er UNIQUE
            if (rs.next()) {
                // rækken -> et Patient-objekt (kortet). date_of_birth er en DATE, som hentes direkte som LocalDate
                return new Patient(rs.getInt("id"), rs.getString("username"), rs.getString("password_hash"), rs.getString("first_name"), rs.getString("last_name"), rs.getObject("date_of_birth", LocalDate.class));
            }
            return null; // ingen række = brugeren findes ikke

        } catch (SQLException e) {
            throw new DatabaseException("Could not find user " + username, e);
        }
    }

    // Retter patientens navn (min-profil). UPDATE ændrer en række, der findes
    public void updateName(int id, String firstName, String lastName) {
        // SET = hvad der ændres, WHERE = hvilken række. Uden WHERE ville ALLE patienter få det nye navn!
        String sql = "UPDATE patient SET first_name = ?, last_name = ? WHERE id = ?";

        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, firstName); // første ? = nyt fornavn
            statement.setString(2, lastName);  // andet ? = nyt efternavn
            statement.setInt(3, id);           // tredje ? = patientens id
            statement.executeUpdate();         // executeUpdate = INSERT/UPDATE/DELETE (ingen rækker tilbage)

        } catch (SQLException e) {
            throw new DatabaseException("Could not update name for patient " + id, e);
        }
    }

    // Finder én patient ud fra id – returnerer null, hvis den ikke findes (bruges af min-profil)
    public Patient findById(int id) {
        String sql = "SELECT * FROM patient WHERE id = ?";

        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            ResultSet rs = statement.executeQuery();

            // if, ikke while: id er PRIMARY KEY, så der er højst én række
            if (rs.next()) {
                return new Patient(rs.getInt("id"), rs.getString("username"), rs.getString("password_hash"), rs.getString("first_name"), rs.getString("last_name"), rs.getObject("date_of_birth", LocalDate.class));
            }
            return null;

        } catch (SQLException e) {
            throw new DatabaseException("Could not find patient " + id, e);
        }
    }

    // Skifter kodeordet (min-profil). UPDATE ændrer en række, der allerede findes – ingen ny række, intet id tilbage
    public void updatePassword(int id, String passwordHash) {
        // SET = hvad der ændres, WHERE = hvilken række. Uden WHERE ville ALLE patienter få det nye kodeord!
        String sql = "UPDATE patient SET password_hash = ? WHERE id = ?";

        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, passwordHash); // første ? = det nye kodeord (hash)
            statement.setInt(2, id);              // andet ? = patientens id
            statement.executeUpdate();            // executeUpdate = INSERT/UPDATE/DELETE (ingen rækker tilbage)

        } catch (SQLException e) {
            throw new DatabaseException("Could not update password for patient " + id, e);
        }
    }

    // Sletter patienten (slet konto på min-profil).
    // Alt under patienten (forløb, runder, målinger, dagbog …) slettes automatisk, fordi schema_postgres.sql har ON DELETE CASCADE
    public void delete(int id) {
        String sql = "DELETE FROM patient WHERE id = ?";

        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id); // ? = patientens id
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Could not delete patient " + id, e);
        }
    }
}