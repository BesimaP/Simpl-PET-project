package services;

// Fælles tjek af tekst fra formularerne, så det ikke skal kopieres ind i hver service.
public class InputCheck {

    // klassen har kun static metoder, så den skal ikke kunne laves med new
    private InputCheck() {
    }

    // null eller kun mellemrum tæller som tomt
    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    // længere end kolonnen (VARCHAR(max))? Så ville INSERT fejle med en 500-fejl
    public static boolean isTooLong(String s, int max) {
        return s != null && s.length() > max;
    }
}
