// opretprofil.js — JavaScript kun til opretprofil.html (US6a)

// --- 1. Vis/skjul "Forløbet startede" alt efter Ja/Nej ---

// FIND de to radio-knapper og div'en med datofeltet
const journeyRadios = document.querySelectorAll('input[name="hasJourney"]');
const journeyFields = document.getElementById("journey-fields");
const journeyStart = document.getElementById("journeyStart");

// NÅR man vælger Ja eller Nej ("change" = værdien blev ændret):
journeyRadios.forEach(function (radio) {
    radio.addEventListener("change", function () {

        // er det "Ja", der er valgt nu?
        const yes = radio.value === "yes" && radio.checked;

        // hidden = true skjuler div'en, hidden = false viser den
        journeyFields.hidden = !yes;

        // datoen skal kun udfyldes, når "Ja" er valgt – så sætter vi required til/fra
        journeyStart.required = yes;

        // ved "Ja": skriv dagens dato i feltet som forslag (opskriften fra common.js)
        if (yes && journeyStart.value === "") {
            setTodayIn(journeyStart);
        }
    });
});


// --- 2. Fejlbesked fra serveren ---
// Serveren sender tilbage til fx /opretprofil.html?fejl=brugernavn, når noget gik galt.
// URLSearchParams læser det, der står efter ? i adressen

const params = new URLSearchParams(window.location.search);
const fejl = params.get("fejl");                       // "brugernavn", "felter" … eller null
const formError = document.getElementById("form-error");

// ordbog: fejlkode -> den tekst, brugeren skal se
const fejlTekster = {
    "brugernavn": "Brugernavnet er optaget – vælg et andet.",
    "felter": "Udfyld alle felter, og tjek at datoerne er gyldige.",
    "ukendt": "Noget gik galt. Prøv igen."
};

// hvis der ER en fejl, og vi kender den: skriv teksten i <p id="form-error">
if (fejl !== null && fejlTekster[fejl] !== undefined) {
    formError.textContent = fejlTekster[fejl];
}
