package persistence;

import entities.Document;
import enums.DocumentType;
import exceptions.DatabaseException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Integrationstest af DocumentMapper mod schemaet "test" i databasen Simpl (se TestDatabase).
// Et dokument hænger direkte på patienten. Typen gemmes som id (document_type) men slås op via navnet.
// Testen rører kun rækkerne i databasen – ikke selve filerne på disken.
class DocumentMapperTest {

    private static ConnectionPool connectionPool;
    private static DocumentMapper documentMapper;

    // Køres ÉN gang før alle testene: laver alle 20 tabeller i schemaet test
    @BeforeAll
    static void setUpClass() {
        connectionPool = TestDatabase.pool();
        documentMapper = new DocumentMapper(connectionPool);
        TestDatabase.createTables();
    }

    // Køres før HVER test: 2 patienter og 2 dokumenter til Anna
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

            // Dokumenter til Anna. document_type_id 1 = REFERRAL, 2 = BLOOD_TEST_RESULT
            stmt.execute("""
            INSERT INTO test.document (patient_id, upload_date, title, document_type_id, file_path) VALUES
            (1, '2026-08-01', 'Henvisning', 1, 'uploads/henvisning.pdf'),
            (1, '2026-09-01', 'Blodprøve', 2, 'uploads/blodprove.pdf')
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
        List<Document> documents = documentMapper.findByPatient(1);

        assertEquals(2, documents.size());
        assertEquals("Blodprøve", documents.get(0).getTitle());                       // nyeste først (upload_date DESC)
        assertEquals(DocumentType.BLOOD_TEST_RESULT, documents.get(0).getDocumentType()); // JOIN giver ordet -> enum
        assertEquals(LocalDate.of(2026, 8, 1), documents.get(1).getUploadDate());
        assertEquals("uploads/henvisning.pdf", documents.get(1).getFilePath());
    }

    @Test
    void findByPatientWithoutDocumentsReturnsEmptyList() {
        assertTrue(documentMapper.findByPatient(2).isEmpty());
    }

    @Test
    void save() {
        Document document = new Document(0, 2, LocalDate.of(2026, 9, 15), "Plan", DocumentType.TREATMENT_PLAN, "uploads/plan.pdf");
        int id = documentMapper.save(document);

        assertEquals(3, id);
        assertEquals(DocumentType.TREATMENT_PLAN, documentMapper.findByPatient(2).get(0).getDocumentType());
    }

    @Test
    void deleteOwnDocument() {
        assertTrue(documentMapper.delete(2, 1));
        assertEquals(1, documentMapper.findByPatient(1).size());
    }

    @Test
    void deleteOtherPatientsDocumentDoesNothing() {
        assertFalse(documentMapper.delete(1, 2));                     // Bo kan ikke slette Annas henvisning
        assertEquals(2, documentMapper.findByPatient(1).size());
    }

    // ---------- regler, der ligger i DATABASEN ----------

    @Test
    void saveWithoutFilePathThrowsDatabaseException() {
        // file_path er NOT NULL
        Document document = new Document(0, 1, LocalDate.of(2026, 9, 15), "Uden fil", DocumentType.OTHER, null);
        assertThrows(DatabaseException.class, () -> documentMapper.save(document));
    }

    @Test
    void saveTooLongTitleThrowsDatabaseException() {
        // title er VARCHAR(100)
        Document document = new Document(0, 1, LocalDate.of(2026, 9, 15), "x".repeat(101), DocumentType.OTHER, "uploads/x.pdf");
        assertThrows(DatabaseException.class, () -> documentMapper.save(document));
    }
}
