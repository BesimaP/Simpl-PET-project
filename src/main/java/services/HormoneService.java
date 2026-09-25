package services;

import dao.DatabaseConnection;
import dao.HormoneLogDAO;
import entities.HormoneCurve;
import entities.HormoneLog;
import entities.Round;
import enums.HormoneType;
import enums.ServiceResult;
import exceptions.NoActiveJourneyException;
import exceptions.NoActiveRoundException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// Forretningslogik for hormonmålinger (US9). Kender IKKE Javalin – controlleren kalder saveLog med felterne.
// Svarer med ServiceResult: OK = ok, ellers hvad der gik galt (controlleren vælger side ud fra det).
public class HormoneService {

    // Gemmer én måling på den runde, der er i gang. En måling SKAL ligge på en runde (round_id i databasen).
    public ServiceResult saveLog(int patientId, String hormone, String value, String unit, String date) {
        // 0. regel: alle felter skal være udfyldt (serveren stoler ikke på required i HTML)
        if (isBlank(hormone) || isBlank(value) || isBlank(unit) || isBlank(date)) {
            return ServiceResult.INVALID_INPUT;
        }

        // 1-2. find den runde, der er i gang – det er den, målingen skal hænge på.
        //      findActiveRound kaster, hvis der ikke er forløb eller runde – vi fanger og oversætter til et ServiceResult
        Round round;
        try {
            round = new RoundService().findActiveRound(patientId);
        } catch (NoActiveJourneyException e) {
            return ServiceResult.NO_ACTIVE_JOURNEY;
        } catch (NoActiveRoundException e) {
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

    // Henter alle målinger i den runde, der er i gang, nyeste først – til listen på hormoner-siden (GET).
// Intet forløb eller ingen runde er ikke en fejl her: så er der bare ingen målinger at vise -> tom liste
    public List<HormoneLog> getLogs(int patientId) {
        try {
            Round round = new RoundService().findActiveRound(patientId);
            return new HormoneLogDAO(DatabaseConnection.getConnection()).findByRound(round.getId());
        } catch (NoActiveJourneyException | NoActiveRoundException e) {
            return new ArrayList<>();
        }
    }

    // Sletter én måling – kun patientens egen (id'et skal findes i rundens liste). Svar: OK · NOT_FOUND
    public ServiceResult deleteLog(int patientId, int logId) {
        for (HormoneLog log : getLogs(patientId)) {
            if (log.getId() == logId) {
                new HormoneLogDAO(DatabaseConnection.getConnection()).delete(logId);
                return ServiceResult.OK;
            }
        }
        return ServiceResult.NOT_FOUND;
    }

    // SVG'ens størrelse – skabelonen bruger samme tal i viewBox
    public static final int CHART_WIDTH = 320;
    public static final int CHART_HEIGHT = 140;
    private static final int PADDING = 20;   // luft til tekst i kanterne

    // Bygger kurven for ét hormon i den runde, der er i gang (US9). type = null -> hormonet fra den nyeste måling.
    // Målingerne regnes om til SVG-koordinater: x = jævnt fordelt (ældste til venstre), y = skaleret efter største værdi (0 i bunden).
    // Ingen målinger -> en tom kurve (ikke null), så skabelonen altid kan spørge ${curve.points.isEmpty()}
    public HormoneCurve getCurve(int patientId, HormoneType type) {
        List<HormoneLog> all = getLogs(patientId);   // nyeste først

        // 1. hvilket hormon? Det valgte – ellers det fra den nyeste måling
        if (type == null && !all.isEmpty()) {
            type = all.get(0).getHormoneType();
        }

        // 2. kun målinger af det hormon, ældste først (vi vender listen)
        List<HormoneLog> logs = new ArrayList<>();
        for (int i = all.size() - 1; i >= 0; i--) {
            if (all.get(i).getHormoneType() == type) {
                logs.add(all.get(i));
            }
        }

        // 3. største værdi bestemmer skalaen på y-aksen
        double max = 0;
        for (HormoneLog log : logs) {
            if (log.getValue() > max) {
                max = log.getValue();
            }
        }
        String unit = logs.isEmpty() ? "" : logs.get(0).getUnit();
        HormoneCurve curve = new HormoneCurve(type, unit, max);

        // 4. ét punkt per måling. Ét punkt alene sættes i midten
        int n = logs.size();
        double innerWidth = CHART_WIDTH - 2 * PADDING;
        double innerHeight = CHART_HEIGHT - 2 * PADDING;
        for (int i = 0; i < n; i++) {
            HormoneLog log = logs.get(i);
            double x = (n == 1) ? CHART_WIDTH / 2.0 : PADDING + innerWidth * i / (n - 1);
            double y = (max == 0) ? CHART_HEIGHT - PADDING : CHART_HEIGHT - PADDING - innerHeight * log.getValue() / max;
            String dateLabel = log.getDateTime().format(DateTimeFormatter.ofPattern("d/M"));
            curve.addPoint(new HormoneCurve.Point(Math.round(x), Math.round(y), log.getValue(), dateLabel));
        }

        // 5. gennemsnit: sum / antal, afrundet til én decimal – og samme omregning til y som punkterne
        if (n > 0) {
            double sum = 0;
            for (HormoneLog log : logs) {
                sum += log.getValue();
            }
            double average = Math.round(sum / n * 10) / 10.0;
            double averageY = (max == 0) ? CHART_HEIGHT - PADDING : CHART_HEIGHT - PADDING - innerHeight * average / max;
            curve.setAverage(average, Math.round(averageY));
        }
        return curve;
    }

    // lille hjælper: null eller kun mellemrum tæller som tomt
    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
