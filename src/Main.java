import dao.*;
import enums.*;
import entities.*;
import java.sql.Connection;
import java.time.LocalDate;

public class Main {
    public static void main(String[] args) {
        DatabaseInitializer.initialize();
        Connection connection = DatabaseConnection.getConnection();

        UserAccountDAO accountDAO = new UserAccountDAO(connection);
        PatientDAO patientDAO = new PatientDAO(connection);
        FertilityJourneyDAO journeyDAO = new FertilityJourneyDAO(connection);
        RoundDAO roundDAO = new RoundDAO(connection);

        int accountId = accountDAO.save(new UserAccount(0, "test", "hash"));
        int patientId = patientDAO.save(new Patient(0, accountId, "Test Testesen", LocalDate.of(1990, 1, 1)));
        int journeyId = journeyDAO.save(new FertilityJourney(0, patientId, LocalDate.now(), JourneyStatus.ACTIVE));
        int roundId = roundDAO.save(new Round(0, journeyId, 1, TreatmentType.IVF, LocalDate.now(), null, RoundStatus.IN_PROGRESS, null));

        System.out.println("Gemt: konto " + accountId + ", patient " + patientId + ", forløb " + journeyId + ", runde " + roundId);
    }
}