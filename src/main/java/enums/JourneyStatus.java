package enums;

// Status på et forløb. En patient må højst have ét ACTIVE forløb ad gangen. Det tjekker vi i DashboardService,
// og databasen sikrer det også med et unikt indeks (one_active_journey_per_patient).
public enum JourneyStatus {
    ACTIVE,
    COMPLETED
}
