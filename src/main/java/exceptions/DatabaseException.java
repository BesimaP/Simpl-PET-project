package exceptions;

import java.sql.SQLException;

// Mapperne kaster den i stedet for SQLException. Den er unchecked, så vi ikke skal fange den alle steder –
// ExceptionConfig viser fejlsiden. cause er den oprindelige SQLException, så vi kan se fejlen i loggen.
public class DatabaseException extends RuntimeException {
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

    // Afviste databasen en dublet? PostgreSQL giver så SQLState "23505".
    // Bruges ved dobbeltklik, hvor to ens forespørgsler begge kan nå forbi vores tjek i Java.
    public boolean isDuplicate() {
        return getCause() instanceof SQLException sqlException && "23505".equals(sqlException.getSQLState());
    }
}
