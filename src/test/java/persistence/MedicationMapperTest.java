package persistence;

import entities.Medication;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Integrationstest af MedicationMapper mod schemaet "test" i databasen Simpl (se TestDatabase).
// Medicinlisten er STAMDATA: de 17 præparater indsættes af schema_postgres.sql, og mapperen kan kun læse dem.
// Derfor er der ingen @BeforeEach med testdata – listen er den samme i alle tests (clearData() rører den ikke).
class MedicationMapperTest {

    private static ConnectionPool connectionPool;
    private static MedicationMapper medicationMapper;

    // Køres ÉN gang før alle testene: laver alle 20 tabeller i schemaet test (inkl. medicinlisten)
    @BeforeAll
    static void setUpClass() {
        connectionPool = TestDatabase.pool();
        medicationMapper = new MedicationMapper(connectionPool);
        TestDatabase.createTables();
    }

    @Test
    void testConnection() throws SQLException {
        // try ( … ): forbindelsen afleveres igen bagefter, så testen ikke "stjæler" en fra nøgleringen
        try (Connection connection = connectionPool.getConnection()) {
            assertNotNull(connection);
        }
    }

    @Test
    void findAll() {
        List<Medication> medications = medicationMapper.findAll();

        assertEquals(17, medications.size());                          // alle 17 fra schema_postgres.sql
        assertEquals("Cetrotide", medications.get(0).getDescription()); // alfabetisk efter visningsnavn (ORDER BY description)
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
        // WHERE name = ? skelner mellem store og små bogstaver – name gemmes med STORE (som enum-navne)
        assertNull(medicationMapper.findByName("gonal_f"));
    }
}
