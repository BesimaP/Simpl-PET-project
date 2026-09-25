package enums;

// Trin i en runde, som vises på tidslinjen (ordlisten). Matcher CHECK på event.event_type.
// label = den danske tekst, der vises på tidslinjen (Thymeleaf: ${e.eventType.label})
public enum EventType {
    STIMULATION_START("Runde startet"),
    EGG_RETRIEVAL("Ægudtagning"),
    FERTILISATION("Befrugtning"),
    EMBRYO_TRANSFER("Oplægning"),
    PREGNANCY_TEST("Graviditetstest");

    private final String label;

    // enum-konstruktør: kaldes én gang per værdi ovenfor med teksten i parentesen
    EventType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
