package persistence;

import exceptions.DatabaseException;

import entities.Patient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;


// SQL for tabellen patient.
public class PatientMapper {

    private ConnectionPool connectionPool;

    public PatientMapper(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    // gemmer en ny patient og returnerer det nye id
    public int save(Patient patient) {
        String sql = "INSERT INTO patient (username, password_hash, first_name, last_name, date_of_birth) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, patient.getUsername());
            statement.setString(2, patient.getPasswordHash());
            statement.setString(3, patient.getFirstName());
            statement.setString(4, patient.getLastName());
            statement.setObject(5, patient.getDateOfBirth());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    patient.setId(keys.getInt(1));
                }
            }
            return patient.getId();

        } catch (SQLException e) {
            throw new DatabaseException("Could not save patient", e);
        }
    }

    // bruges ved login og opret profil. Returnerer null, hvis brugernavnet ikke findes
    public Patient findByUsername(String username) {
        // LOWER på begge sider, så "Anna" og "anna" er samme bruger (passer med det unikke indeks på LOWER(username))
        String sql = "SELECT * FROM patient WHERE LOWER(username) = LOWER(?)";

        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new Patient(rs.getInt("id"), rs.getString("username"), rs.getString("password_hash"), rs.getString("first_name"), rs.getString("last_name"), rs.getObject("date_of_birth", LocalDate.class));
                }
                return null;
            }

        } catch (SQLException e) {
            throw new DatabaseException("Could not find user " + username, e);
        }
    }

    // retter navn og fødselsdato fra min-profil
    public void updateProfile(int id, String firstName, String lastName, LocalDate dateOfBirth) {
        String sql = "UPDATE patient SET first_name = ?, last_name = ?, date_of_birth = ? WHERE id = ?";

        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, firstName);
            statement.setString(2, lastName);
            statement.setObject(3, dateOfBirth);
            statement.setInt(4, id);
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Could not update patient " + id, e);
        }
    }

    // bruges af min-profil. Returnerer null, hvis patienten ikke findes
    public Patient findById(int id) {
        String sql = "SELECT * FROM patient WHERE id = ?";

        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new Patient(rs.getInt("id"), rs.getString("username"), rs.getString("password_hash"), rs.getString("first_name"), rs.getString("last_name"), rs.getObject("date_of_birth", LocalDate.class));
                }
                return null;
            }

        } catch (SQLException e) {
            throw new DatabaseException("Could not find patient " + id, e);
        }
    }

    // skifter kodeord fra min-profil – det er hashen, der gemmes
    public void updatePassword(int id, String passwordHash) {
        String sql = "UPDATE patient SET password_hash = ? WHERE id = ?";

        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, passwordHash);
            statement.setInt(2, id);
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Could not update password for patient " + id, e);
        }
    }

    // slet konto på min-profil. Forløb, runder, målinger osv. forsvinder med, fordi schemaet har ON DELETE CASCADE
    public void delete(int id) {
        String sql = "DELETE FROM patient WHERE id = ?";

        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Could not delete patient " + id, e);
        }
    }
}