// opretprofil.js – til opretprofil.html (US6a)

// --- Vis datofeltet kun, når man har svaret Ja ---

const journeyRadios = document.querySelectorAll('input[name="hasJourney"]');
const journeyFields = document.getElementById("journey-fields");
const journeyStart = document.getElementById("journeyStart");

journeyRadios.forEach(function (radio) {
    radio.addEventListener("change", function () {

        const yes = radio.value === "yes" && radio.checked;

        journeyFields.hidden = !yes;

        // datoen er kun påkrævet ved Ja
        journeyStart.required = yes;

        // dagens dato som forslag
        if (yes && journeyStart.value === "") {
            setTodayIn(journeyStart);
        }
    });
});


// I HTML'en er feltet synligt, så siden virker uden JavaScript. Her skjuler vi det fra start, hvis Nej er valgt
const yesChecked = document.querySelector('input[name="hasJourney"][value="yes"]').checked;
journeyFields.hidden = !yesChecked;

