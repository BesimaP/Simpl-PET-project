package services;

// Fælles tjek af tekst fra formularerne. Stod før som en kopi i hver service – nu ét sted (SRP).
// Kun static metoder: kaldes direkte på klassen, fx InputCheck.isBlank(name), uden new
public class InputCheck {

    // ingen objekter af klassen – den er kun en samling hjælpemetoder
    private InputCheck() {
    }

    // null eller kun mellemrum tæller som tomt
    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    // længere end kolonnen i databasen (VARCHAR(max))? Så ville INSERT fejle med en 500-fejl
    public static boolean isTooLong(String s, int max) {
        return s != null && s.length() > max;
    }
}
