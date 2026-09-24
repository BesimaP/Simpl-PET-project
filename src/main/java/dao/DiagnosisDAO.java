package dao;

import entities.Diagnosis;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Al SQL for tabellen diagnosis (US7). En diagnose hænger på patienten – ikke på forløb eller runde.
public class DiagnosisDAO {
    private Connection connection;

    public DiagnosisDAO(Connection connection) {
        this.connection = connection;
    }

    // gemmer én diagnose og returnerer det id, databasen gav den
    public int save(Diagnosis diagnosis) {
        String sql = "INSERT INTO diagnosis (patient_id, name, description) VALUES (?, ?, ?)";
        try {
            PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            statement.setInt(1, diagnosis.getPatientId());
            statement.setString(2, diagnosis.getName());
            statement.setString(3, diagnosis.getDescription()); // må være null – kolonnen er ikke NOT NULL
            statement.executeUpdate();

            ResultSet keys = statement.getGeneratedKeys();
            if (keys.next()) {
                diagnosis.setId(keys.getInt(1));
            }
            return diagnosis.getId();

        } catch (SQLException e) {
            throw new RuntimeException("Could not save diagnosis", e);
        }
    }

    // henter alle patientens diagnoser, alfabetisk – til listen på diagnoser-siden
    public List<Diagnosis> findByPatient(int patientId) {
        String sql = "SELECT * FROM diagnosis WHERE patient_id = ? ORDER BY name";
        List<Diagnosis> diagnoses = new ArrayList<>();
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, patientId);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                diagnoses.add(mapRow(rs));
            }
            return diagnoses;

        } catch (SQLException e) {
            throw new RuntimeException("Could not find diagnoses for patient " + patientId, e);
        }
    }

    // sletter én diagnose ud fra dens id (US7: patienten kan fjerne en diagnose igen)
    public void delete(int id) {
        String sql = "DELETE FROM diagnosis WHERE id = ?";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete diagnosis " + id, e);
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
