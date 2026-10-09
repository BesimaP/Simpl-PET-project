package persistence;

import entities.FertilityJourney;
import enums.JourneyStatus;
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

// Integrationstest af FertilityJourneyMapper mod schemaet "test" i databasen Simpl (se TestDatabase).
// Et forløb hænger på en patient, så @BeforeEach lægger først patienter i og derefter forløb.
class FertilityJourneyMapperTest {

    private static ConnectionPool connectionPool;
    private static FertilityJourneyMapper journeyMapper;

    // Køres ÉN gang før alle testene: laver alle 20 tabeller i schemaet test
    @BeforeAll
    static void setUpClass() {
        connectionPool = TestDatabase.pool();
        journeyMapper = new FertilityJourneyMapper(connectionPool);
        TestDatabase.createTables();
    }

    // Køres før HVER test: tøm tabellerne og indsæt de samme patienter og forløb
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

            // Forløb: Anna har et gammelt afsluttet forløb (id 1) og et aktivt (id 2). Bo har ingen forløb.
            // journey_status_id: 1 = ACTIVE, 2 = COMPLETED (se schema_postgres.sql)
            stmt.execute("""
            INSERT INTO test.fertility_journey (patient_id, start_date, journey_status_id) VALUES
            (1, '2025-01-10', 2),
            (1, '2026-03-01', 1)
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
    void findActiveByPatient() {
        FertilityJourney journey = journeyMapper.findActiveByPatient(1);

        assertNotNull(journey);
        assertEquals(2, journey.getId()); // det aktive, ikke det afsluttede
        assertEquals(LocalDate.of(2026, 3, 1), journey.getStartDate());
        assertEquals(JourneyStatus.ACTIVE, journey.getStatus());
    }

    @Test
    void findActiveByPatientWithoutJourneyReturnsNull() {
        assertNull(journeyMapper.findActiveByPatient(2)); // Bo har intet forløb
    }

    @Test
    void findByPatient() {
        List<FertilityJourney> journeys = journeyMapper.findByPatient(1);

        assertEquals(2, journeys.size());
        // ORDER BY start_date DESC: nyeste først
        assertEquals(2, journeys.get(0).getId());
        assertEquals(JourneyStatus.ACTIVE, journeys.get(0).getStatus());
        assertEquals(JourneyStatus.COMPLETED, journeys.get(1).getStatus());
    }

    @Test
    void findByPatientWithoutJourneysReturnsEmptyList() {
        assertTrue(journeyMapper.findByPatient(2).isEmpty()); // Bo har ingen forløb -> tom liste, ikke null
    }

    @Test
    void save() {
        FertilityJourney journey = new FertilityJourney(0, 2, LocalDate.of(2026, 10, 1), JourneyStatus.ACTIVE);
        int id = journeyMapper.save(journey);

        assertEquals(3, id); // der ligger 2 i forvejen
        assertEquals(3, journeyMapper.findActiveByPatient(2).getId());
    }

    @Test
    void saveSecondActiveJourneyThrowsDatabaseException() {
        // Anna har allerede et aktivt forløb. Det unikke indeks one_active_journey_per_patient afviser et til
        FertilityJourney journey = new FertilityJourney(0, 1, LocalDate.of(2026, 10, 1), JourneyStatus.ACTIVE);
        assertThrows(DatabaseException.class, () -> journeyMapper.save(journey));
    }

    @Test
    void endJourney() {
        journeyMapper.endJourney(2);

        assertNull(journeyMapper.findActiveByPatient(1)); // Anna har nu intet aktivt forløb
        assertEquals(JourneyStatus.COMPLETED, journeyMapper.findByPatient(1).get(0).getStatus());
    }

    @Test
    void saveNewActiveJourneyAfterEndJourney() {
        journeyMapper.endJourney(2);   // Annas aktive forløb afsluttes

        // nu må hun gerne få et nyt aktivt forløb: indekset gælder kun ACTIVE-forløb
        FertilityJourney journey = new FertilityJourney(0, 1, LocalDate.of(2026, 10, 1), JourneyStatus.ACTIVE);
        int id = journeyMapper.save(journey);

        assertEquals(3, id);
        assertEquals(3, journeyMapper.findActiveByPatient(1).getId());
        assertEquals(3, journeyMapper.findByPatient(1).size()); // de gamle forløb er der stadig
    }
}
