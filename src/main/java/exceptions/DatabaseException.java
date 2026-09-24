package exceptions;

// Egen exception til databasefejl. DAO'erne kaster den i stedet for at lade SQLException slippe ud.
// extends RuntimeException = "unchecked": den skal IKKE fanges alle steder – Main har én samlet handler
// (config.routes.exception), der viser en fejlside. cause = den oprindelige SQLException, så den rigtige fejl kan ses i loggen
public class DatabaseException extends RuntimeException {
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
