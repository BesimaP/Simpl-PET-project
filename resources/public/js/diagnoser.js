// diagnoser.js — JavaScript kun til diagnoser.html (US7)
// Tæller diagnoserne i listen og skriver tallet under den.

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


// Formularen sendes til serveren (POST /diagnoser) – der er IKKE længere en submit-handler her.
// Før backend var på, stoppede vi siden med event.preventDefault() og tilføjede et <li> selv.
// Det ville nu betyde, at diagnosen aldrig blev gemt. Listen kommer fra databasen, når templates er på.


// KØR updateCount én gang, når siden er loadet (så tallet passer fra start)
updateCount();
