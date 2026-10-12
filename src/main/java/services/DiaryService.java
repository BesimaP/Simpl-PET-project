package services;

import persistence.ConnectionPool;
import persistence.DiaryEntryMapper;
import entities.DiaryEntry;
import enums.ServiceResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

// Logik for dagbogen (US4). Noter hænger på patienten og ikke forløbet, så man kan skrive før, mellem og efter forløb.
public class DiaryService {
    private DiaryEntryMapper diaryEntryMapper;

    public DiaryService(ConnectionPool connectionPool) {
        this.diaryEntryMapper = new DiaryEntryMapper(connectionPool);
    }

    // Gemmer en note. Svar: OK eller INVALID_INPUT
    public ServiceResult saveEntry(int patientId, String date, String title, String note) {
        // dato, titel og note skal være udfyldt
        if (InputCheck.isBlank(date) || InputCheck.isBlank(title) || InputCheck.isBlank(note)) {
            return ServiceResult.INVALID_INPUT;
        }
        // titlen skal kunne være i kolonnen (VARCHAR(100))
        if (InputCheck.isTooLong(title, 100)) {
            return ServiceResult.INVALID_INPUT;
        }

        // noten får kl. 00:00, da formularen kun har en dato
        LocalDateTime dateTime;
        try {
            dateTime = LocalDate.parse(date).atStartOfDay();
        } catch (DateTimeParseException e) {
            return ServiceResult.INVALID_INPUT;
        }

        DiaryEntry entry = new DiaryEntry(0, patientId, dateTime, title.trim(), note);
        diaryEntryMapper.save(entry);

        return ServiceResult.OK;
    }

    // nyeste først
    public List<DiaryEntry> getEntries(int patientId) {
        return diaryEntryMapper.findByPatient(patientId);
    }

    // Sletter kun, hvis noten er patientens egen (WHERE id = ? AND patient_id = ?). Ellers NOT_FOUND
    public ServiceResult deleteEntry(int patientId, int entryId) {
        return diaryEntryMapper.delete(entryId, patientId) ? ServiceResult.OK : ServiceResult.NOT_FOUND;
    }
}
