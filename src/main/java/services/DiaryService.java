package services;

import dao.DatabaseConnection;
import dao.DiaryEntryDAO;
import entities.DiaryEntry;
import entities.FertilityJourney;
import enums.ServiceResult;
import exceptions.NoActiveJourneyException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

// Forretningslogik for dagbogsnoter (US4). Kender IKKE Javalin – controlleren læser formularen og kalder én metode her.
// Svarer med ServiceResult: OK · INVALID_INPUT = tomt felt/ugyldig dato · NO_ACTIVE_JOURNEY = intet forløb at lægge noten på
public class DiaryService {

    // Gemmer én note på patientens aktive forløb
    public ServiceResult saveEntry(int patientId, String date, String title, String note) {
        // 0. regel: dato, titel og note skal være udfyldt (serveren stoler ikke på required i HTML)
        if (isBlank(date) || isBlank(title) || isBlank(note)) {
            return ServiceResult.INVALID_INPUT;
        }

        // 1. find patientens aktive forløb – noter hænger på forløbet, så uden forløb er der ingen skuffe at lægge noten i
        //    findActiveJourney kaster, hvis der ikke er et – vi fanger og oversætter til et ServiceResult
        FertilityJourney journey;
        try {
            journey = new DashboardService().findActiveJourney(patientId);
        } catch (NoActiveJourneyException e) {
            return ServiceResult.NO_ACTIVE_JOURNEY;
        }

        // 2. "2026-09-21" -> LocalDateTime (kl. 00:00). try/catch: ugyldig dato giver INVALID_INPUT i stedet for et crash
        LocalDateTime dateTime;
        try {
            dateTime = LocalDate.parse(date).atStartOfDay();
        } catch (DateTimeParseException e) {
            return ServiceResult.INVALID_INPUT;
        }

        // 3. byg kortet af felterne og læg det i skuffen diary_entry
        DiaryEntryDAO diaryDao = new DiaryEntryDAO(DatabaseConnection.getConnection());
        DiaryEntry entry = new DiaryEntry(0, journey.getId(), dateTime, title, note);
        diaryDao.save(entry);

        return ServiceResult.OK;
    }

    // lille hjælper: null eller kun mellemrum tæller som tomt
    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
