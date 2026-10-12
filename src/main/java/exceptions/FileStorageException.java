package exceptions;

// Kastes, når en uploadet fil ikke kan gemmes på disken. Unchecked ligesom DatabaseException – ExceptionConfig viser fejlsiden.
public class FileStorageException extends RuntimeException {
    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
