package exceptions;

// Kastes af DashboardService.findActiveJourney, når patienten ikke har et aktivt forløb.
// Checked, så de services, der kalder, skal tage stilling til den (typisk svare NO_ACTIVE_JOURNEY).
public class NoActiveJourneyException extends Exception {
    public NoActiveJourneyException(String message) {
        super(message);
    }
}
