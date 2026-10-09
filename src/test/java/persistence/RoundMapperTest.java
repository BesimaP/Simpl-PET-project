package persistence;

import entities.Round;
import enums.Result;
import enums.RoundStatus;
import enums.TreatmentType;
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

// Integrationstest af RoundMapper: mapperen taler med en RIGTIG PostgreSQL-database.
// Testene bruger schemaet "test" i databasen Simpl (se TestDatabase), så de aldrig rører de rigtige data i "public".
class RoundMapperTest {

    private static ConnectionPool connectionPool;
    private static RoundMapper roundMapper;

    // Køres ÉN gang før alle testene: laver alle 20 tabeller i schemaet test
    @BeforeAll
    static void setUpClass() {
        connectionPool = TestDatabase.pool();
        roundMapper = new RoundMapper(connectionPool);
        TestDatabase.createTables();
    }

    // Køres før HVER test: tøm tabellerne og indsæt kæden patient -> forløb -> 2 runder
    // Rækkefølgen betyder noget: en runde peger på et forløb, og et forløb peger på en patient (fremmednøgler)
    @BeforeEach
    void setUp() {
        TestDatabase.clearData();   // tom + id starter forfra på 1

        try (Connection connection = connectionPool.getConnection();
             Statement stmt = connection.createStatement()) {

            // Patient (id 1 = Anna)
            stmt.execute("""
            INSERT INTO test.patient (username, password_hash, first_name, last_name, date_of_birth) VALUES
            ('anna', 'hash1', 'Anna', 'Jensen', '1990-05-01')
            """);

            // Forløb (id 1) til Anna. journey_status_id 1 = ACTIVE
            stmt.execute("""
            INSERT INTO test.fertility_journey (patient_id, start_date, journey_status_id) VALUES
            (1, '2026-01-10', 1)
            """);

            // Runder i forløb 1. treatment_type_id 1 = IVF, 2 = ICSI. result_id 2 = NEGATIVE
            // runde 1 (id 1): afsluttet · runde 2 (id 2): i gang (end_date = NULL)
            stmt.execute("""
            INSERT INTO test.round (fertility_journey_id, round_number, treatment_type_id, start_date, end_date, result_id) VALUES
            (1, 1, 1, '2026-02-01', '2026-03-01', 2),
            (1, 2, 2, '2026-04-01', NULL, NULL)
            """);

        } catch (SQLException e) {
            fail("Test data setup failed: " + e.getMessage());
        }
    }

    @Test
    void testConnection() throws SQLException {
        assertNotNull(connectionPool.getConnection());
    }

    @Test
    void findActiveByJourney() {
        Round round = roundMapper.findActiveByJourney(1);
        assertNotNull(round);
        assertEquals(2, round.getId());                        // runde 2 er den uden slutdato
        assertEquals(TreatmentType.ICSI, round.getTreatmentType());
        assertEquals(RoundStatus.IN_PROGRESS, round.getStatus());
        assertNull(round.getEndDate());
    }

    @Test
    void findActiveByJourneyUnknownReturnsNull() {
        assertNull(roundMapper.findActiveByJourney(99));
    }

    @Test
    void findByJourney() {
        List<Round> rounds = roundMapper.findByJourney(1);
        assertEquals(2, rounds.size());
        assertEquals(1, rounds.get(0).getRoundNumber());          // ældste først (ORDER BY round_number)
        assertEquals(Result.NEGATIVE, rounds.get(0).getResult()); // LEFT JOIN result giver ordet tilbage
        assertNull(rounds.get(1).getResult());                    // runden i gang har intet resultat endnu
    }

    @Test
    void endRound() {
        roundMapper.endRound(2, LocalDate.of(2026, 5, 1), Result.POSITIVE);

        assertNull(roundMapper.findActiveByJourney(1));           // ingen runde i gang længere
        Round round = roundMapper.findByJourney(1).get(1);
        assertEquals(LocalDate.of(2026, 5, 1), round.getEndDate());
        assertEquals(Result.POSITIVE, round.getResult());
    }

    @Test
    void saveAfterEndingRound() {
        roundMapper.endRound(2, LocalDate.of(2026, 5, 1), Result.NEGATIVE);   // først skal runde 2 afsluttes

        Round round = new Round(0, 1, 3, TreatmentType.FET, LocalDate.of(2026, 6, 1), null, RoundStatus.IN_PROGRESS, null);
        int id = roundMapper.save(round);

        assertEquals(3, id);                                      // der ligger 2 i forvejen
        assertEquals(TreatmentType.FET, roundMapper.findActiveByJourney(1).getTreatmentType());
    }

    // ---------- regler, der ligger i DATABASEN (kan kun fanges af en integrationstest) ----------

    @Test
    void saveSecondActiveRoundThrowsDatabaseException() {
        // runde 2 er stadig i gang -> one_active_round_per_journey (unikt index på end_date IS NULL) afviser
        Round round = new Round(0, 1, 3, TreatmentType.IVF, LocalDate.of(2026, 6, 1), null, RoundStatus.IN_PROGRESS, null);
        assertThrows(DatabaseException.class, () -> roundMapper.save(round));
    }

    @Test
    void saveDuplicateRoundNumberThrowsDatabaseException() {
        roundMapper.endRound(2, LocalDate.of(2026, 5, 1), Result.NEGATIVE);
        // rundenummer 2 findes allerede i forløb 1 -> UNIQUE (fertility_journey_id, round_number) afviser
        Round round = new Round(0, 1, 2, TreatmentType.IVF, LocalDate.of(2026, 6, 1), null, RoundStatus.IN_PROGRESS, null);
        assertThrows(DatabaseException.class, () -> roundMapper.save(round));
    }

    @Test
    void endRoundBeforeStartDateThrowsDatabaseException() {
        // runde 2 startede 2026-04-01 -> CHECK (end_date >= start_date) afviser en slutdato før
        assertThrows(DatabaseException.class,
                () -> roundMapper.endRound(2, LocalDate.of(2026, 3, 1), Result.NEGATIVE));
    }
}
