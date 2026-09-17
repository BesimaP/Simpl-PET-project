// diagnoser.js — JavaScript kun til diagnoser.html (US7)
// Tilføjer en ny diagnose til listen med det samme, når man trykker Gem.
// (indtil backend er på, gemmes den ikke - den forsvinder, når siden genindlæses)

// FIND formularen på siden
const diagnoseForm = document.querySelector("form");

// FIND navn-feltet og beskrivelse-feltet
const diagnoseField = document.getElementById("diagnosis");
const descriptionField = document.getElementById("description");

// FIND listen "Dine diagnoser" (<ul class="notes">)
const diagnosisList = document.querySelector(".notes");

// FIND teksten "x diagnoser registreret"
const countText = document.getElementById("diagnosis-count");


// OPSKRIFT updateCount (kører ikke nu - kun når den kaldes)
function updateCount() {

    // tæl hvor mange <li> der er i listen
    const count = diagnosisList.querySelectorAll("li").length;

    // skriv tallet i tælle-teksten
    countText.textContent = count + " diagnoser registreret.";
}


// NÅR formularen sendes (tryk på Gem):
diagnoseForm.addEventListener("submit", function (event) {

    // stop siden i at genindlæse (ellers forsvinder det, vi tilføjer)
    event.preventDefault();

    // lav et nyt tomt <li> (findes kun i hukommelsen endnu)
    const item = document.createElement("li");

    // lav et <strong> med navnet fra feltet
    const name = document.createElement("strong");
    name.textContent = diagnoseField.value;

    // put <strong> ind i <li>
    item.appendChild(name);

    // HVIS beskrivelse-feltet ikke er tomt:
    if (descriptionField.value !== "") {

        // lav et <span> med beskrivelsen, og put det i <li>
        const description = document.createElement("span");
        description.textContent = descriptionField.value;
        item.appendChild(description);
    }

    // put <li> ind i listen - NU kan man se den på siden
    diagnosisList.appendChild(item);

    // tæl igen
    updateCount();

    // tøm felterne, og sæt markøren i navn-feltet, klar til næste
    diagnoseForm.reset();
    diagnoseField.focus();
});


// KØR updateCount én gang, når siden er loadet (så tallet passer fra start)
updateCount();
