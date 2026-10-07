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


// --- 2. Skjul datofeltet, når siden åbner ---
// I HTML'en er feltet synligt, så siden også virker uden JavaScript.
// Her (med JavaScript) skjuler vi det fra start, hvis "Nej" er valgt – og viser det igen ved "Ja" (punkt 1 ovenfor)
const yesChecked = document.querySelector('input[name="hasJourney"][value="yes"]').checked;
journeyFields.hidden = !yesChecked;

// (Fejlbeskeden fra serveren, fx "Brugernavnet er optaget", skrives nu af Thymeleaf i opretprofil.html – ingen JavaScript nødvendig)
