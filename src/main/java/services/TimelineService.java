package services;

import dao.DatabaseConnection;
import dao.EventDAO;
import entities.Event;
import entities.Round;
import enums.EventType;
import exceptions.NoActiveJourneyException;
import exceptions.NoActiveRoundException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Forretningslogik for tidslinjen (US2). Kender IKKE Javalin.
// Tidslinjen viser kun – patienten tilføjer ikke selv trin. Systemet opretter dem via addEvent
// (RoundService ved start runde, AppointmentService ved ægudtagning/oplægning/graviditetstest).
public class TimelineService {

    // Lægger ét trin på en runde. Kaldes af andre services, ikke af en controller. description må være null
    public void addEvent(int roundId, LocalDateTime dateTime, EventType type, String description) {
        new EventDAO(DatabaseConnection.getConnection()).save(new Event(0, roundId, dateTime, type, description));
    }

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
