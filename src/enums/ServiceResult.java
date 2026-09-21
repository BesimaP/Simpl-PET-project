package enums;

// Udfaldet af en handling i en service. Controlleren bruger det til at vælge, hvor brugeren sendes hen.
public enum ServiceResult {
    OK,                 // det lykkedes
    INVALID_INPUT,      // manglende eller ugyldige felter
    ALREADY_EXISTS,     // noget findes allerede (fx brugernavnet er optaget)
    NO_ACTIVE_JOURNEY,  // patienten har intet aktivt forløb
    NO_ACTIVE_ROUND,    // patienten har ingen aktiv runde
    ROUND_IN_PROGRESS   // der er allerede en runde i gang
}