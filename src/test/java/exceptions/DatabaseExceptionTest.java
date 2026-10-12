package exceptions;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

// Unittest af isDuplicate. Vi laver selv en SQLException med den fejlkode, PostgreSQL ville give, så vi ikke skal bruge databasen
class DatabaseExceptionTest {

    @Test
    void uniqueViolationIsDuplicate() {
        SQLException cause = new SQLException("duplicate key value", "23505");   // 23505 = brud på UNIQUE
        assertTrue(new DatabaseException("Could not save", cause).isDuplicate());
    }

    @Test
    void otherSqlErrorIsNotDuplicate() {
        SQLException cause = new SQLException("connection refused", "08001");    // 08001 = kunne ikke forbinde
        assertFalse(new DatabaseException("Could not save", cause).isDuplicate());
    }
}
