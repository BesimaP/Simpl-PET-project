package services;

import persistence.ConnectionPool;
import persistence.HormoneLogMapper;
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

// Logik for hormonmålinger og kurven på hormoner-siden (US9).
public class HormoneService {
    // enhederne i dropdownen i hormoner.html – samme liste som CHECK på hormone_log.unit
    public static final List<String> UNITS = List.of("pmol/L", "IU/L", "nmol/L", "pg/mL", "ng/mL", "mIU/mL");
    private static final double MAX_VALUE = 100_000_000;   // NUMERIC(10,2) = højst 8 cifre før kommaet
    private HormoneLogMapper hormoneLogMapper;
    private RoundService roundService;

    public HormoneService(ConnectionPool connectionPool) {
        this.hormoneLogMapper = new HormoneLogMapper(connectionPool);
        this.roundService = new RoundService(connectionPool);
    }

    // Gemmer en måling på den runde, der er i gang. En måling skal altid ligge på en runde.
    public ServiceResult saveLog(int patientId, String hormone, String value, String unit, String date) {
        // alle felter skal være udfyldt
        if (InputCheck.isBlank(hormone) || InputCheck.isBlank(value) || InputCheck.isBlank(unit) || InputCheck.isBlank(date)) {
            return ServiceResult.INVALID_INPUT;
        }

        Round round;
        try {
            round = roundService.findActiveRound(patientId);
        } catch (NoActiveJourneyException e) {
            return ServiceResult.NO_ACTIVE_JOURNEY;
        } catch (NoActiveRoundException e) {
            return ServiceResult.NO_ACTIVE_ROUND;
        }

        // ugyldig dato, ukendt hormon eller "abc" som tal giver INVALID_INPUT i stedet for et crash
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

        // parseDouble accepterer "NaN" og "Infinity", så det tjekker vi selv. Værdien skal også passe i NUMERIC(10,2)
        if (!Double.isFinite(numericValue) || numericValue < 0 || numericValue >= MAX_VALUE) {
            return ServiceResult.INVALID_INPUT;
        }

        // en måling før runden startede hører ikke til denne runde
        if (dateTime.toLocalDate().isBefore(round.getStartDate())) {
            return ServiceResult.INVALID_INPUT;
        }

        if (!UNITS.contains(unit)) {
            return ServiceResult.INVALID_INPUT;
        }

        HormoneLog log = new HormoneLog(0, round.getId(), dateTime, hormoneType, numericValue, unit);

        hormoneLogMapper.save(log);

        return ServiceResult.OK;
    }

    // Målinger i den runde, der er i gang, nyeste først. Ingen runde giver bare en tom liste
    public List<HormoneLog> getLogs(int patientId) {
        try {
            Round round = roundService.findActiveRound(patientId);
            return hormoneLogMapper.findByRound(round.getId());
        } catch (NoActiveJourneyException | NoActiveRoundException e) {
            return new ArrayList<>();
        }
    }

    // Sletter kun patientens egen måling (tjekkes i SQL'en). Ellers NOT_FOUND
    public ServiceResult deleteLog(int patientId, int logId) {
        return hormoneLogMapper.delete(logId, patientId) ? ServiceResult.OK : ServiceResult.NOT_FOUND;
    }

    // SVG'ens størrelse – skal passe med viewBox i skabelonen
    public static final int CHART_WIDTH = 320;
    public static final int CHART_HEIGHT = 140;
    private static final int PADDING = 20;   // luft til tekst i kanterne

    // Bygger kurven for et hormon. type = null betyder hormonet fra den nyeste måling.
    // x er jævnt fordelt (ældste til venstre), y skaleres efter største værdi. Ingen målinger giver en tom kurve, ikke null
    public HormoneCurve getCurve(int patientId, HormoneType type) {
        List<HormoneLog> all = getLogs(patientId);

        if (type == null && !all.isEmpty()) {
            type = all.get(0).getHormoneType();
        }

        // enheden tages fra den nyeste måling. Den afhænger af laboratoriet, og to enheder kan ikke stå på samme akse
        String unit = "";
        for (HormoneLog log : all) {
            if (log.getHormoneType() == type) {
                unit = log.getUnit();
                break;
            }
        }

        // kun målinger i den enhed, ældste først. Dem i en anden enhed tæller vi, så siden kan sige, at de mangler på kurven
        List<HormoneLog> logs = new ArrayList<>();
        int skipped = 0;
        for (int i = all.size() - 1; i >= 0; i--) {
            HormoneLog log = all.get(i);
            if (log.getHormoneType() != type) {
                continue;
            }
            if (log.getUnit().equals(unit)) {
                logs.add(log);
            } else {
                skipped++;
            }
        }

        // største værdi bestemmer skalaen på y-aksen
        double max = 0;
        for (HormoneLog log : logs) {
            if (log.getValue() > max) {
                max = log.getValue();
            }
        }
        HormoneCurve curve = new HormoneCurve(type, unit, max);
        curve.setSkipped(skipped);

        // et punkt per måling – er der kun ét, sættes det i midten
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

        // gennemsnit med én decimal, regnet om til y på samme måde som punkterne
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
}
