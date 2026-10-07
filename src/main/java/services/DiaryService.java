package services;

import persistence.ConnectionPool;
import persistence.DiaryEntryMapper;
import entities.DiaryEntry;
import enums.ServiceResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

// Forretningslogik for dagbogsnoter (US4). Kender IKKE Javalin – controlleren læser formularen og kalder én metode her.
// Noter hænger på patienten (ikke forløbet) – man kan skrive dagbog før, mellem og efter forløb.
// Svarer med ServiceResult: OK · INVALID_INPUT = tomt felt/ugyldig dato
public class DiaryService {
    private DiaryEntryMapper diaryEntryMapper; // arkivaren for dagbogen

    public DiaryService(ConnectionPool connectionPool) {
        this.diaryEntryMapper = new DiaryEntryMapper(connectionPool);
    }

    // Gemmer én note på patienten
    public ServiceResult saveEntry(int patientId, String date, String title, String note) {
        // 0. regel: dato, titel og note skal være udfyldt (serveren stoler ikke på required i HTML)
        if (isBlank(date) || isBlank(title) || isBlank(note)) {
            return ServiceResult.INVALID_INPUT;
        }
        // titlen skal kunne være i kolonnen (VARCHAR(100))
        if (isTooLong(title, 100)) {
            return ServiceResult.INVALID_INPUT;
        }

        // 1. "2026-09-21" -> LocalDateTime (kl. 00:00). try/catch: ugyldig dato giver INVALID_INPUT i stedet for et crash
        LocalDateTime dateTime;
        try {
            dateTime = LocalDate.parse(date).atStartOfDay();
        } catch (DateTimeParseException e) {
            return ServiceResult.INVALID_INPUT;
        }

        // 2. byg kortet af felterne og læg det i skuffen diary_entry
        DiaryEntry entry = new DiaryEntry(0, patientId, dateTime, title, note);
        diaryEntryMapper.save(entry);

        return ServiceResult.OK;
    }

    // Henter alle patientens noter, nyeste først – til listen på dagbog-siden (GET)
    public List<DiaryEntry> getEntries(int patientId) {
        return diaryEntryMapper.findByPatient(patientId);
    }

    // Sletter én note – men kun hvis den er patientens egen: mapperen sletter med "WHERE id = ? AND patient_id = ?".
    // Svar: OK · NOT_FOUND = findes ikke / ikke hendes (så blev 0 rækker slettet)
    public ServiceResult deleteEntry(int patientId, int entryId) {
        return diaryEntryMapper.delete(entryId, patientId) ? ServiceResult.OK : ServiceResult.NOT_FOUND;
    }

    // lille hjælper: null eller kun mellemrum tæller som tomt
    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    // lille hjælper: længere end kolonnen i databasen (VARCHAR(max))? Så ville INSERT fejle med en 500-fejl
    private boolean isTooLong(String s, int max) {
        return s != null && s.length() > max;
    }
}
