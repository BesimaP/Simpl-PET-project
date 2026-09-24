package services;

import dao.DatabaseConnection;
import dao.EventDAO;
import entities.Event;
import entities.Round;
import exceptions.NoActiveJourneyException;
import exceptions.NoActiveRoundException;

import java.util.ArrayList;
import java.util.List;

// Forretningslogik for tidslinjen (US2). Kender IKKE Javalin.
// Tidslinjen viser kun – patienten tilføjer ikke selv trin (systemet opretter dem, fx ved start/afslut runde).
public class TimelineService {

    // Henter rundens trin til tidslinjen – tom liste, hvis der ikke er forløb/runde (ikke en fejl, bare ingenting at vise)
    public List<Event> getEvents(int patientId) {
        // 1. find den runde, der er i gang – trinene hænger på runden. findActiveRound kaster, hvis der ikke er forløb eller runde
        Round round;
        try {
            round = new RoundService().findActiveRound(patientId);
        } catch (NoActiveJourneyException | NoActiveRoundException e) {
            return new ArrayList<>(); // intet forløb eller ingen runde = ingen trin at vise
        }

        // 2. hent rundens trin fra databasen, ældste først
        return new EventDAO(DatabaseConnection.getConnection()).findByRound(round.getId());
    }
}
