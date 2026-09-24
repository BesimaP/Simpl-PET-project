package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// Én delt forbindelse til simpl.db. Oprettes første gang getConnection() kaldes.
public class DatabaseConnection {

    private static Connection connection;

    public static Connection getConnection() {
        if (connection == null) {
            try {
                connection = DriverManager.getConnection("jdbc:sqlite:simpl.db");
                // SQLite har foreign keys slået fra som standard — skal slås til pr. forbindelse
                connection.createStatement().execute("PRAGMA foreign_keys = ON");
            } catch (SQLException e) {
                throw new RuntimeException("Could not connect to database", e);
            }
        }
        return connection;
    }
}
