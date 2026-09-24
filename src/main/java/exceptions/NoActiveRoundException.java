package exceptions;

// Egen exception: kastes af RoundService.findActiveRound, når der ikke er nogen runde i gang.
// Bruges af de services, der skal hænge noget på runden (hormoner, medicin, tidslinje) – de fanger den og svarer NO_ACTIVE_ROUND.
public class NoActiveRoundException extends Exception {
    public NoActiveRoundException(String message) {
        super(message);
    }
}
