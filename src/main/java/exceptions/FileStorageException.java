package exceptions;

// Egen exception, når en uploadet fil ikke kan gemmes på disken (fx mappen kan ikke oprettes).
// Unchecked ligesom DatabaseException – ExceptionConfig fanger den og viser fejlsiden
public class FileStorageException extends RuntimeException {
    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
