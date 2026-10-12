package enums;

// Aftaletyper. Navnene matcher tabellen appointment_type i schema_postgres.sql.
// label er den danske tekst, vi viser på siderne.
public enum AppointmentType {
    CONSULTATION("Konsultation"),
    SCANNING("Scanning"),
    BLOOD_TEST("Blodprøve"),
    EGG_RETRIEVAL("Ægudtagning"),
    EMBRYO_TRANSFER("Oplægning"),
    PREGNANCY_TEST("Graviditetstest");

    private final String label;

    AppointmentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
