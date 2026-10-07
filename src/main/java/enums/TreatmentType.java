package enums;

// Behandlingstype for en runde (ordlisten). Bruges i Round.treatmentType.
// Matcher navnene i tabellen treatment_type i schema_postgres.sql.
public enum TreatmentType {
    IVF("IVF"),                               // in vitro-fertilisering
    ICSI("ICSI"),                             // intracytoplasmatisk sædcelleinjektion
    IUI("Insemination (IUI)"),                // intrauterin insemination
    FET("Frossen ægoplægning (FET)");         // frozen embryo transfer

    private final String label;   // den danske tekst i dropdownen på start-runde.html

    TreatmentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
