package enums;

// Aftaletyper (ordlisten). Matcher CHECK på appointment.appointment_type.
// label = den danske tekst, der vises på siderne (Thymeleaf: ${a.appointmentType.label})
public enum AppointmentType {
    CONSULTATION("Konsultation"),
    SCANNING("Scanning"),
    BLOOD_TEST("Blodprøve"),
    EGG_RETRIEVAL("Ægudtagning"),
    EMBRYO_TRANSFER("Oplægning"),
    PREGNANCY_TEST("Graviditetstest");

    private final String label;

    // enum-konstruktør: kaldes én gang per værdi ovenfor med teksten i parentesen
    AppointmentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
