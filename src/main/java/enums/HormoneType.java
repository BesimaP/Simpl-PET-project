package enums;

// Hormontyper (ordlisten). Matcher CHECK på hormone_log.hormone_type.
// label = den danske tekst, der vises på siderne (Thymeleaf: ${h.hormoneType.label})
public enum HormoneType {
    FSH("FSH"),
    LH("LH"),
    E2_OESTRADIOL("Østradiol"),
    PROGESTERONE("Progesteron"),
    AMH("AMH");

    private final String label;

    // enum-konstruktør: kaldes én gang per værdi ovenfor med teksten i parentesen
    HormoneType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
