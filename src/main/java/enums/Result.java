package enums;

// Resultat af en afsluttet runde. Ingen PENDING — en runde uden resultat har result = null.
// label = den danske tekst, der vises på siderne (Thymeleaf: ${r.result.label}) – samme mønster som AppointmentType
public enum Result {
    POSITIVE("Positiv"),
    NEGATIVE("Negativ");

    private final String label;

    // enum-konstruktør: kaldes én gang per værdi ovenfor med teksten i parentesen
    Result(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
