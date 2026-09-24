package services;

import dao.DatabaseConnection;
import dao.HormoneLogDAO;
import dao.RoundDAO;
import entities.FertilityJourney;
import entities.HormoneLog;
import entities.Round;
import enums.HormoneType;
import enums.ServiceResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

// Forretningslogik for hormonmålinger (US9). Kender IKKE Javalin – controlleren kalder saveLog med felterne.
// Svarer med ServiceResult: OK = ok, ellers hvad der gik galt (controlleren vælger side ud fra det).
public class HormoneService {

    // Gemmer én måling på den runde, der er i gang. En måling SKAL ligge på en runde (round_id i databasen).
    public ServiceResult saveLog(int patientId, String hormone, String value, String unit, String date) {
        // 0. regel: alle felter skal være udfyldt (serveren stoler ikke på required i HTML)
        if (isBlank(hormone) || isBlank(value) || isBlank(unit) || isBlank(date)) {
            return ServiceResult.INVALID_INPUT;
        }

        // 1. find patientens aktive forløb – uden forløb kan der ikke være en runde
        FertilityJourney journey = new DashboardService().findActiveJourney(patientId);
        if (journey == null) {
            return ServiceResult.NO_ACTIVE_JOURNEY;
        }

        // 2. find den runde, der er i gang i forløbet – det er den, målingen skal hænge på
        RoundDAO roundDao = new RoundDAO(DatabaseConnection.getConnection());
        Round round = roundDao.findActiveByJourney(journey.getId());
        if (round == null) {
            return ServiceResult.NO_ACTIVE_ROUND;
        }

        // 3. tekst fra formularen -> de typer, HormoneLog vil have:
        //    "2026-09-21" -> LocalDateTime (kl. 00:00), "FSH" -> enum, "450" -> 450.0
        //    try/catch: en ugyldig dato, et ukendt hormon eller "abc" som tal giver INVALID_INPUT i stedet for et crash
        LocalDateTime dateTime;
        HormoneType hormoneType;
        double numericValue;
        try {
            dateTime = LocalDate.parse(date).atStartOfDay();
            hormoneType = HormoneType.valueOf(hormone);
            numericValue = Double.parseDouble(value);
        } catch (DateTimeParseException | IllegalArgumentException e) {
            return ServiceResult.INVALID_INPUT;
        }

        // 4. byg kortet og læg det i skuffen hormone_log
        HormoneLogDAO hormoneLogDao = new HormoneLogDAO(DatabaseConnection.getConnection());
        HormoneLog log = new HormoneLog(0, round.getId(), dateTime, hormoneType, numericValue, unit);

        hormoneLogDao.save(log);

        return ServiceResult.OK;
    }

    // lille hjælper: null eller kun mellemrum tæller som tomt
    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
