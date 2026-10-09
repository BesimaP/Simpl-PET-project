package persistence;

import entities.DiaryEntry;
import exceptions.DatabaseException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Integrationstest af DiaryEntryMapper mod schemaet "test" i databasen Simpl (se TestDatabase).
// Et dagbogsnotat hænger direkte på patienten. Samme "kun egne"-mønster som DiagnosisMapper.
class DiaryEntryMapperTest {

    private static ConnectionPool connectionPool;
    private static DiaryEntryMapper diaryEntryMapper;

    // Køres ÉN gang før alle testene: laver alle 20 tabeller i schemaet test
    @BeforeAll
    static void setUpClass() {
        connectionPool = TestDatabase.pool();
        diaryEntryMapper = new DiaryEntryMapper(connectionPool);
        TestDatabase.createTables();
    }

    // Køres før HVER test: 2 patienter og 2 dagbogsnotater til Anna
    @BeforeEach
    void setUp() {
        TestDatabase.clearData();   // tom + id starter forfra på 1

        try (Connection connection = connectionPool.getConnection();
             Statement stmt = connection.createStatement()) {

            // Patienter (id 1 = Anna, id 2 = Bo)
            stmt.execute("""
            INSERT INTO test.patient (username, password_hash, first_name, last_name, date_of_birth) VALUES
            ('anna', 'hash1', 'Anna', 'Jensen', '1990-05-01'),
            ('bo', 'hash2', 'Bo', 'Hansen', '1988-11-20')
            """);

            // Notater til Anna: id 1 (1. sep) og id 2 (5. sep)
            stmt.execute("""
            INSERT INTO test.diary_entry (patient_id, date_time, title, content) VALUES
            (1, '2026-09-01 20:00', 'Første stik', 'Det gik fint'),
            (1, '2026-09-05 21:30', 'Scanning', 'Fem follikler')
            """);

        } catch (SQLException e) {
            fail("Test data setup failed: " + e.getMessage());
        }
    }

    @Test
    void testConnection() throws SQLException {
        // try ( … ): forbindelsen afleveres igen bagefter, så testen ikke "stjæler" en fra nøgleringen
        try (Connection connection = connectionPool.getConnection()) {
            assertNotNull(connection);
        }
    }

    @Test
    void findByPatient() {
        List<DiaryEntry> entries = diaryEntryMapper.findByPatient(1);

        assertEquals(2, entries.size());
        assertEquals("Scanning", entries.get(0).getTitle());                   // nyeste først (ORDER BY date_time DESC)
        assertEquals(LocalDateTime.of(2026, 9, 5, 21, 30), entries.get(0).getDateTime());
        assertEquals("Det gik fint", entries.get(1).getContent());
    }

    @Test
    void findByPatientWithoutEntriesReturnsEmptyList() {
        assertTrue(diaryEntryMapper.findByPatient(2).isEmpty());              // Bo har ingen notater
    }

    @Test
    void save() {
        DiaryEntry entry = new DiaryEntry(0, 2, LocalDateTime.of(2026, 9, 10, 8, 0), "Dag 1", "Spændt");
        int id = diaryEntryMapper.save(entry);

        assertEquals(3, id);                                                  // der ligger 2 i forvejen
        assertEquals(id, entry.getId());                                      // mapperen sætter id'et på objektet
        assertEquals("Dag 1", diaryEntryMapper.findByPatient(2).get(0).getTitle());
    }

    @Test
    void deleteOwnEntry() {
        assertTrue(diaryEntryMapper.delete(1, 1));
        assertEquals(1, diaryEntryMapper.findByPatient(1).size());
    }

    @Test
    void deleteOtherPatientsEntryDoesNothing() {
        // Bo (id 2) prøver at slette Annas notat -> WHERE id = ? AND patient_id = ? rammer 0 rækker
        assertFalse(diaryEntryMapper.delete(1, 2));
        assertEquals(2, diaryEntryMapper.findByPatient(1).size());
    }

    // ---------- regler, der ligger i DATABASEN ----------

    @Test
    void saveWithoutContentThrowsDatabaseException() {
        // content er NOT NULL
        DiaryEntry entry = new DiaryEntry(0, 1, LocalDateTime.of(2026, 9, 10, 8, 0), "Tom", null);
        assertThrows(DatabaseException.class, () -> diaryEntryMapper.save(entry));
    }

    @Test
    void saveOnUnknownPatientThrowsDatabaseException() {
        // fremmednøgle: patient 99 findes ikke
        DiaryEntry entry = new DiaryEntry(0, 99, LocalDateTime.of(2026, 9, 10, 8, 0), "Titel", "Tekst");
        assertThrows(DatabaseException.class, () -> diaryEntryMapper.save(entry));
    }
}
