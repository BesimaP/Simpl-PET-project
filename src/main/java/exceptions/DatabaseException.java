package exceptions;

import java.sql.SQLException;

// Egen exception til databasefejl. DAO'erne kaster den i stedet for at lade SQLException slippe ud.
// extends RuntimeException = "unchecked": den skal IKKE fanges alle steder – Main har én samlet handler
// (config.routes.exception), der viser en fejlside. cause = den oprindelige SQLException, så den rigtige fejl kan ses i loggen
public class DatabaseException extends RuntimeException {
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

    // Var fejlen, at databasen afviste en dublet (UNIQUE-regel brudt)? PostgreSQL giver så fejlkoden (SQLState) "23505".
    // Bruges af services ved dobbeltklik: to ens forespørgsler kan begge nå forbi tjekket i Java, før den første er gemt
    public boolean isDuplicate() {
        return getCause() instanceof SQLException sqlException && "23505".equals(sqlException.getSQLState());
    }
}
