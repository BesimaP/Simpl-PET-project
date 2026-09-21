package dao;

import entities.UserAccount;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// Al SQL for tabellen user_account
public class UserAccountDAO {

    private Connection connection;

    public UserAccountDAO(Connection connection) {
        this.connection = connection;
    }

    // Gemmer en ny konto og returnerer det id, databasen gav den
    public int save(UserAccount userAccount) {
        String sql = "INSERT INTO user_account (username, password_hash) VALUES (?, ?)";
        try {
            PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            statement.setString(1, userAccount.getUsername());
            statement.setString(2, userAccount.getPasswordHash());
            statement.executeUpdate();

            ResultSet keys = statement.getGeneratedKeys();
            if (keys.next()) {
                userAccount.setId(keys.getInt(1));
            }
            return userAccount.getId();

        } catch (SQLException e) {
            throw new RuntimeException("Could not save user account", e);
        }
    }

    // Finder én konto ud fra brugernavnet – returnerer null, hvis den ikke findes (bruges ved login)
    public UserAccount findByUsername(String username) {
        String sql = "SELECT * FROM user_account WHERE username = ?";

        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, username);

            // executeQuery = SELECT (giver rækker tilbage). executeUpdate = INSERT/UPDATE/DELETE
            ResultSet rs = statement.executeQuery();

            // if, ikke while: der kan højst være én række, fordi username er UNIQUE
            if (rs.next()) {
                // rækken -> et UserAccount-objekt (kortet), som controlleren kan kigge på
                return new UserAccount(rs.getInt("id"), rs.getString("username"), rs.getString("password_hash"));
            }
            return null; // ingen række = brugeren findes ikke
        } catch (SQLException e) {
            throw new RuntimeException("Could not find user " + username, e);
        }
    }

    // Skifter kodeordet på én konto (min-profil). UPDATE ændrer en række, der allerede findes – ingen ny række, intet id tilbage
    public void updatePassword(int id, String passwordHash) {
        // SET = hvad der ændres, WHERE = hvilken række. Uden WHERE ville ALLE konti få det nye kodeord!
        String sql = "UPDATE user_account SET password_hash = ? WHERE id = ?";

        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, passwordHash); // første ? = det nye kodeord (hash)
            statement.setInt(2, id);              // andet ? = kontoens id
            statement.executeUpdate();            // executeUpdate = INSERT/UPDATE/DELETE (ingen rækker tilbage)

        } catch (SQLException e) {
            throw new RuntimeException("Could not update password for account " + id, e);
        }
    }

    // Sletter én konto (slet konto på min-profil).
    // Patienten og alt under den (forløb, runder, målinger …) slettes automatisk, fordi schema.sql har ON DELETE CASCADE
    public void delete(int id) {
        String sql = "DELETE FROM user_account WHERE id = ?";

        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, id);   // ? = kontoens id
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete account " + id, e);
        }
    }

    // Finder én konto ud fra id – returnerer null, hvis den ikke findes (bruges når kodeord skiftes)
    public UserAccount findById(int id) {
        String sql = "SELECT * FROM user_account WHERE id = ?";

        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, id); // ? = kontoens id

            // executeQuery = SELECT (giver rækker tilbage)
            ResultSet rs = statement.executeQuery();

            // if, ikke while: id er PRIMARY KEY, så der er højst én række
            if (rs.next()) {
                // rækken -> et UserAccount-objekt (kortet)
                return new UserAccount(rs.getInt("id"), rs.getString("username"), rs.getString("password_hash"));
            }
            return null; // ingen række = kontoen findes ikke

        } catch (SQLException e) {
            throw new RuntimeException("Could not find account " + id, e);
        }
    }
}
