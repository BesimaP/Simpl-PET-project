package dao;

import entities.Medication;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Al SQL for tabellen medication. Stamdata for lægemidler (Gonal-F, Menopur …) – deles af alle patienter.
public class MedicationDAO {
    private Connection connection;

    public MedicationDAO(Connection connection) {
        this.connection = connection;
    }

    // gemmer et nyt lægemiddel og returnerer det id, databasen gav det
    public int save(Medication medication) {
        String sql = "INSERT INTO medication (name, description) VALUES (?, ?)";
        try {
            PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            statement.setString(1, medication.getName());        // UNIQUE i databasen – samme navn to gange giver fejl
            statement.setString(2, medication.getDescription()); // må være null
            statement.executeUpdate();

            ResultSet keys = statement.getGeneratedKeys();
            if (keys.next()) {
                medication.setId(keys.getInt(1));
            }
            return medication.getId();

        } catch (SQLException e) {
            throw new RuntimeException("Could not save medication", e);
        }
    }

    // henter alle lægemidler, alfabetisk – til dropdownen "Medicin" på medicin-siden
    public List<Medication> findAll() {
        String sql = "SELECT * FROM medication ORDER BY name";
        List<Medication> medications = new ArrayList<>();
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                medications.add(mapRow(rs));
            }
            return medications;

        } catch (SQLException e) {
            throw new RuntimeException("Could not find medications", e);
        }
    }

    // henter ét lægemiddel ud fra navnet – returnerer null, hvis det ikke findes
    public Medication findByName(String name) {
        String sql = "SELECT * FROM medication WHERE name = ?";
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, name);
            ResultSet rs = statement.executeQuery();

            // if i stedet for while: der kan højst være én række, fordi name er UNIQUE
            if (rs.next()) {
                return mapRow(rs);
            }
            return null;

        } catch (SQLException e) {
            throw new RuntimeException("Could not find medication " + name, e);
        }
    }

    // én række fra databasen -> ét Medication-objekt
    private Medication mapRow(ResultSet rs) throws SQLException {
        return new Medication(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description")
        );
    }
}
