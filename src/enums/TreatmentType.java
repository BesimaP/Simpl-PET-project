package enums;

// Behandlingstype for en runde (ordlisten). Bruges i Round.treatmentType.
// Matcher CHECK-constraint på round.treatment_type i schema.sql.
public enum TreatmentType {
    IVF,   // in vitro-fertilisering
    ICSI,  // intracytoplasmatisk sædcelleinjektion
    IUI,   // intrauterin insemination
    FET    // frozen embryo transfer
}
