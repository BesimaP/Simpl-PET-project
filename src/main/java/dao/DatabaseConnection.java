package dao;

import exceptions.DatabaseException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// Én delt forbindelse til databasen. Oprettes første gang getConnection() kaldes.
// Adressen (url) er det ENESTE sted, der ved hvilken database vi taler med – skift til PostgreSQL = ret url + driver i pom
public class DatabaseConnection {

    // standard: SQLite-filen simpl.db i projektets rodmappe
    private static String url = "jdbc:sqlite:simpl.db";
    private static Connection connection;

    public static Connection getConnection() {
        if (connection == null) {
            try {
                connection = DriverManager.getConnection(url);
                // SQLite har foreign keys slået fra som standard — skal slås til pr. forbindelse
                connection.createStatement().execute("PRAGMA foreign_keys = ON");
            } catch (SQLException e) {
                throw new DatabaseException("Could not connect to database", e);
            }
        }
        return connection;
    }

    // Kun til tests: skift til en anden database, FØR første getConnection().
    // "jdbc:sqlite::memory:" = en tom database i hukommelsen, som forsvinder, når testen er færdig – den rigtige simpl.db røres ikke
    public static void useTestDatabase() {
        url = "jdbc:sqlite::memory:";
        connection = null; // glem en evt. gammel forbindelse, så næste getConnection() åbner testdatabasen
    }
}
