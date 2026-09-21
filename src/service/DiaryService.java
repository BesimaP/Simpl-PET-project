package service;

import dao.DatabaseConnection;
import dao.DiaryEntryDAO;
import entities.DiaryEntry;
import entities.FertilityJourney;

import java.time.LocalDate;

public class DiaryService {
    public String saveEntry(int patientId, String date, String title, String note){
        // 1. find patientens aktive forløb – uden forløb kan der ikke være en runde
        FertilityJourney journey = new DashboardService().findActiveJourney(patientId);
        if (journey == null) {
            return "intet-forloeb";
        }

        DiaryEntryDAO diaryDao = new DiaryEntryDAO(DatabaseConnection.getConnection());
        DiaryEntry entry = new DiaryEntry(0,journey.getId(), LocalDate.parse(date).atStartOfDay(), title, note);
        diaryDao.save(entry);

        return null;
    }
}
