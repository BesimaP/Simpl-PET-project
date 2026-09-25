package enums;

// Dokumenttyper (ordlisten). Matcher CHECK på document.document_type.
// label = den danske tekst, der vises på siden (Thymeleaf: ${d.documentType.label})
public enum DocumentType {
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
