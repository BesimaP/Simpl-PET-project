package enums;

// Hormontyper. Navnene matcher tabellen hormone_type i schema_postgres.sql.
// label er den danske tekst, vi viser på siderne.
public enum HormoneType {
    FSH("FSH"),
    LH("LH"),
    E2_OESTRADIOL("Østradiol"),
    PROGESTERONE("Progesteron"),
    AMH("AMH");

    private final String label;

    HormoneType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
