package persistence;

import entities.Medication;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Integrationstest af MedicationMapper mod schema test (se TestDatabase).
// Medicinlisten er stamdata fra schemaet, så vi har ingen @BeforeEach med testdata.
class MedicationMapperTest {

    private static ConnectionPool connectionPool;
    private static MedicationMapper medicationMapper;

    @BeforeAll
    static void setUpClass() {
        connectionPool = TestDatabase.pool();
        medicationMapper = new MedicationMapper(connectionPool);
        TestDatabase.createTables();
    }

    @Test
    void testConnection() throws SQLException {
        try (Connection connection = connectionPool.getConnection()) {
            assertNotNull(connection);
        }
    }

    @Test
    void findAll() {
        List<Medication> medications = medicationMapper.findAll();

        assertEquals(17, medications.size());                          // de 17 præparater fra schemaet
        assertEquals("Cetrotide", medications.get(0).getDescription()); // sorteret efter description
    }

    @Test
    void findByName() {
        Medication medication = medicationMapper.findByName("GONAL_F");

        assertNotNull(medication);
        assertEquals(1, medication.getId());
        assertEquals("Gonal-F", medication.getDescription());
        assertEquals("IU", medication.getUnit());
    }

    @Test
    void findByNameUnknownReturnsNull() {
        assertNull(medicationMapper.findByName("PANODIL"));
    }

    @Test
    void findByNameIsCaseSensitive() {
        // name gemmes med store bogstaver, og WHERE name = ? skelner mellem store og små
        assertNull(medicationMapper.findByName("gonal_f"));
    }
}
