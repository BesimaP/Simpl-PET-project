package dao;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

// Opretter alle tabeller ved at køre schema.sql. Filen er den eneste sandhed om skemaet.
public class DatabaseInitializer {

    public static void initialize() {
        String sql = readSchema();
        Connection connection = DatabaseConnection.getConnection();

        try (Statement statement = connection.createStatement()) {
            // Fjern kommentarlinjer, og kør én CREATE TABLE ad gangen (adskilt af semikolon)
            String withoutComments = sql.replaceAll("(?m)^\\s*--.*$", "");
            for (String statementText : withoutComments.split(";")) {
                if (!statementText.isBlank()) {
                    statement.execute(statementText);
                }
            }
            System.out.println("Database initialized from schema.sql");
        } catch (SQLException e) {
            throw new RuntimeException("Could not initialize database", e);
        }
    }

    // Læser schema.sql fra classpath (src/main/resources/data lander som data/ i target/classes)
    private static String readSchema() {
        try (InputStream in = DatabaseInitializer.class.getClassLoader().getResourceAsStream("data/schema.sql")) {
            if (in == null) {
                throw new RuntimeException("schema.sql not found on classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Could not read schema.sql", e);
        }
    }
}