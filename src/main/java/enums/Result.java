package enums;

// Resultat af en afsluttet runde. Vi har ikke en PENDING – en runde uden resultat har bare result = null.
public enum Result {
    POSITIVE("Positiv"),
    NEGATIVE("Negativ");

    private final String label;

    Result(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
