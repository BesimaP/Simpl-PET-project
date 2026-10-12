package enums;

// Behandlingstype for en runde. Navnene matcher tabellen treatment_type i schema_postgres.sql.
public enum TreatmentType {
    IVF("IVF"),                               // in vitro-fertilisering
    ICSI("ICSI"),                             // intracytoplasmatisk sædcelleinjektion
    IUI("Insemination (IUI)"),                // intrauterin insemination
    FET("Frossen ægoplægning (FET)");         // frozen embryo transfer

    private final String label;   // teksten i dropdownen på start-runde.html

    TreatmentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
