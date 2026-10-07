package enums;

// Behandlingstype for en runde (ordlisten). Bruges i Round.treatmentType.
// Matcher navnene i tabellen treatment_type i schema_postgres.sql.
public enum TreatmentType {
    IVF,   // in vitro-fertilisering
    ICSI,  // intracytoplasmatisk sædcelleinjektion
    IUI,   // intrauterin insemination
    FET    // frozen embryo transfer
}
