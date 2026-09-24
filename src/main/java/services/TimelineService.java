package services;

import dao.DatabaseConnection;
import dao.EventDAO;
import dao.RoundDAO;
import entities.Event;
import entities.FertilityJourney;
import entities.Round;

import java.util.ArrayList;
import java.util.List;

// Forretningslogik for tidslinjen (US2). Kender IKKE Javalin.
// Tidslinjen viser kun – patienten tilføjer ikke selv trin (systemet opretter dem, fx ved start/afslut runde).
public class TimelineService {

    // Henter rundens trin til tidslinjen – tom liste, hvis der ikke er forløb/runde (ikke en fejl, bare ingenting at vise)
    public List<Event> getEvents(int patientId) {
        // 1. find patientens aktive forløb
        FertilityJourney journey = new DashboardService().findActiveJourney(patientId);
        if (journey == null) {
            return new ArrayList<>(); // intet forløb = ingen trin at vise
        }

        // 2. find den runde, der er i gang – trinene hænger på runden
        RoundDAO roundDao = new RoundDAO(DatabaseConnection.getConnection());
        Round round = roundDao.findActiveByJourney(journey.getId());
        if (round == null) {
            return new ArrayList<>(); // ingen runde = ingen trin
        }

        // 3. hent rundens trin fra databasen, ældste først
        return new EventDAO(DatabaseConnection.getConnection()).findByRound(round.getId());
    }
}
