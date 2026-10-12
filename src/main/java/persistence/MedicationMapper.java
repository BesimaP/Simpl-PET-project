package persistence;

import entities.Medication;
import exceptions.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MedicationMapper {

    private final ConnectionPool connectionPool;

    public MedicationMapper(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    // sorteret efter description (det navn brugeren ser) til dropdownen på medicin-siden
    public List<Medication> findAll() {
        String sql = "SELECT * FROM medication ORDER BY description";
        List<Medication> medications = new ArrayList<>();
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    medications.add(mapRow(rs));
                }
                return medications;
            }

        } catch (SQLException e) {
            throw new DatabaseException("Could not find medications", e);
        }
    }

    // returnerer null, hvis lægemidlet ikke findes
    public Medication findByName(String name) {
        String sql = "SELECT * FROM medication WHERE name = ?";
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet rs = statement.executeQuery()) {
                // if og ikke while, fordi name er UNIQUE
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }

        } catch (SQLException e) {
            throw new DatabaseException("Could not find medication " + name, e);
        }
    }

    private Medication mapRow(ResultSet rs) throws SQLException {
        return new Medication(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("unit")
        );
    }
}