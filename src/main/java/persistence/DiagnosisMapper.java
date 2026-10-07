package persistence;

import entities.Diagnosis;
import exceptions.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DiagnosisMapper {

    private final ConnectionPool connectionPool;

    public DiagnosisMapper(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    // gemmer én diagnose og returnerer det id, databasen gav den
    public int save(Diagnosis diagnosis) {
        String sql = "INSERT INTO diagnosis (patient_id, name, description) VALUES (?, ?, ?)";
        try (Connection connection = connectionPool.getConnection();   // ← lån en forbindelse
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) { // ← bed om det nye id
            statement.setInt(1, diagnosis.getPatientId());
            statement.setString(2, diagnosis.getName());
            statement.setString(3, diagnosis.getDescription()); // må være null – kolonnen er ikke NOT NULL
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {        // ← hent det nye id
                if (keys.next()) {
                    diagnosis.setId(keys.getInt(1));
                }
            }
            return diagnosis.getId();

        } catch (SQLException e) {
            throw new DatabaseException("Could not save diagnosis", e);
        }
    }

    // henter alle patientens diagnoser, alfabetisk – til listen på diagnoser-siden
    public List<Diagnosis> findByPatient(int patientId) {
        String sql = "SELECT * FROM diagnosis WHERE patient_id = ? ORDER BY name";
        List<Diagnosis> diagnoses = new ArrayList<>();
        try (Connection connection = connectionPool.getConnection();                                   // ← lån en forbindelse
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, patientId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    diagnoses.add(mapRow(rs));
                }
            }
            return diagnoses;

        } catch (SQLException e) {
            throw new DatabaseException("Could not find diagnoses for patient " + patientId, e);
        }
    }

    // sletter én diagnose – men kun hvis den er patientens egen (AND patient_id = ?). true = slettet (US7)
    public boolean delete(int id, int patientId) {
        String sql = "DELETE FROM diagnosis WHERE id = ? AND patient_id = ?";
        try (Connection connection = connectionPool.getConnection();                                   // ← lån en forbindelse
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setInt(2, patientId);
            return statement.executeUpdate() > 0;   // antal rækker, der blev ramt: 0 = ikke fundet / ikke hendes
        } catch (SQLException e) {
            throw new DatabaseException("Could not delete diagnosis " + id, e);
        }
    }

    // én række fra databasen -> ét Diagnosis-objekt
    private Diagnosis mapRow(ResultSet rs) throws SQLException {
        return new Diagnosis(
                rs.getInt("id"),
                rs.getInt("patient_id"),
                rs.getString("name"),
                rs.getString("description") // bliver null, hvis feltet er tomt i databasen
        );
    }
}


