package enums;

// Status på en runde (UC8/UC14). Den gemmes ikke i databasen: RoundMapper sætter COMPLETED, når end_date er udfyldt.
public enum RoundStatus {
    IN_PROGRESS,
    COMPLETED
}
