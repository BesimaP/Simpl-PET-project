package enums;

// Dokumenttyper. Navnene matcher tabellen document_type i schema_postgres.sql.
// label er den danske tekst, vi viser på siden.
public enum DocumentType {
    REFERRAL("Henvisning"),
    BLOOD_TEST_RESULT("Blodprøvesvar"),
    TREATMENT_PLAN("Behandlingsplan"),
    OTHER("Andet");

    private final String label;

    DocumentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
