package exceptions;

// Egen exception: kastes af DashboardService.findActiveJourney, når patienten ikke har et aktivt forløb.
// Bruges af de services, der skal hænge noget på forløbet (runde, dagbog, aftaler …) – de fanger den og svarer NO_ACTIVE_JOURNEY.
// extends Exception = "checked": den, der kalder, SKAL fange den eller sende den videre
public class NoActiveJourneyException extends Exception {
    public NoActiveJourneyException(String message) {
        super(message);
    }
}
