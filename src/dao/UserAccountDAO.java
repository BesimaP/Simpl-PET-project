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
}