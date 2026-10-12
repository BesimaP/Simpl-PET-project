package enums;

// Trin i en runde, som vises på tidslinjen. Navnene matcher tabellen event_type i schema_postgres.sql.
public enum EventType {
    STIMULATION_START("Runde startet"),
    EGG_RETRIEVAL("Ægudtagning"),
    FERTILISATION("Befrugtning"),
    EMBRYO_TRANSFER("Oplægning"),
    PREGNANCY_TEST("Graviditetstest");

    private final String label;

    EventType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
