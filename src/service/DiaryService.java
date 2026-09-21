package service;

import dao.DatabaseConnection;
import dao.DiaryEntryDAO;
import entities.DiaryEntry;
import entities.FertilityJourney;
import enums.ServiceResult;

import java.time.LocalDate;

// Forretningslogik for dagbogsnoter (US4). Kender IKKE Javalin. Svarer med ServiceResult: SAVED = ok.
public class DiaryService {
    public ServiceResult saveEntry(int patientId, String date, String title, String note){
        // 1. find patientens aktive forløb – noter hænger på forløbet, så uden forløb er der ingen skuffe at lægge noten i
        FertilityJourney journey = new DashboardService().findActiveJourney(patientId);
        if (journey == null) {
            return ServiceResult.NO_ACTIVE_JOURNEY;
        }

        // 2. byg kortet af felterne: "2026-09-21" -> LocalDateTime (kl. 00:00)
        DiaryEntryDAO diaryDao = new DiaryEntryDAO(DatabaseConnection.getConnection());
        DiaryEntry entry = new DiaryEntry(0,journey.getId(), LocalDate.parse(date).atStartOfDay(), title, note);
        // 3. læg kortet i skuffen diary_entry
        diaryDao.save(entry);

        return ServiceResult.SAVED;
    }
}
