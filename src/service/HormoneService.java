package service;

import dao.DatabaseConnection;
import dao.HormoneLogDAO;
import dao.RoundDAO;
import entities.FertilityJourney;
import entities.HormoneLog;
import entities.Round;
import enums.HormoneType;

import java.time.LocalDate;

// Forretningslogik for hormonmålinger (US9). Kender IKKE Javalin – controlleren kalder saveLog med felterne.
// Svarer med null = ok, ellers en fejlkode som controlleren kan vise.
public class HormoneService {

    // Gemmer én måling på den runde, der er i gang. En måling SKAL ligge på en runde (round_id i databasen).
    public String saveLog(int patientId, String hormone, String value, String unit, String date) {
        // 1. find patientens aktive forløb – uden forløb kan der ikke være en runde
        FertilityJourney journey = new DashboardService().findActiveJourney(patientId);
        if (journey == null) {
            return "intet-forloeb";
        }

        // 2. find den runde, der er i gang i forløbet – det er den, målingen skal hænge på
        RoundDAO roundDao = new RoundDAO(DatabaseConnection.getConnection());
        Round round = roundDao.findActiveByJourney(journey.getId());
        if (round == null) {
            return "ingen-runde";
        }

        // 3. byg kortet af felterne. Tekst fra formularen oversættes til de typer, HormoneLog vil have:
        //    "2026-09-21" -> LocalDateTime (kl. 00:00), "FSH" -> enum, "450" -> 450.0
        HormoneLogDAO hormoneLogDao = new HormoneLogDAO(DatabaseConnection.getConnection());
        HormoneLog log = new HormoneLog(0, round.getId(), LocalDate.parse(date).atStartOfDay(),
                HormoneType.valueOf(hormone), Double.parseDouble(value), unit);

        // 4. læg kortet i skuffen hormone_log
        hormoneLogDao.save(log);

        return null; // ok
    }
}
