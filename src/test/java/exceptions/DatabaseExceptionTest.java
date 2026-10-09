package exceptions;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

// Unit tests af DatabaseException.isDuplicate – ingen database: vi laver selv en SQLException med den fejlkode, PostgreSQL ville give
class DatabaseExceptionTest {

    @Test
    void uniqueViolationIsDuplicate() {
        SQLException cause = new SQLException("duplicate key value", "23505");   // 23505 = UNIQUE-regel brudt
        assertTrue(new DatabaseException("Could not save", cause).isDuplicate());
    }

    @Test
    void otherSqlErrorIsNotDuplicate() {
        SQLException cause = new SQLException("connection refused", "08001");    // 08001 = kunne ikke forbinde
        assertFalse(new DatabaseException("Could not save", cause).isDuplicate());
    }
}
