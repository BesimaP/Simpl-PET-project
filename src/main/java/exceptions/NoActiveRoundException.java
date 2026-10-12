package exceptions;

// Kastes af RoundService.findActiveRound, når der ikke er nogen runde i gang.
// Servicen, der kalder, fanger den og svarer NO_ACTIVE_ROUND.
public class NoActiveRoundException extends Exception {
    public NoActiveRoundException(String message) {
        super(message);
    }
}
