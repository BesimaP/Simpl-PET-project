// tidslinje.js – JavaScript kun til tidslinje.html (US2)

// Tidslinjen viser rundens trin (events). Patienten tilføjer dem IKKE selv:
// systemet opretter dem (fx STIMULATION_START når runden starter, PREGNANCY_TEST når den afsluttes).
// Derfor er der ingen formular på siden – listen fyldes fra databasen via EventDAO, når backend er på.

// OPSKRIFT formaterDato: gør en dato som "2026-09-05" pæn: "5. september"
// (gemt til senere – bruges når datoerne kommer fra databasen)
function formaterDato(iso) {
    return new Date(iso).toLocaleDateString('da-DK', { day: 'numeric', month: 'long' });
}
