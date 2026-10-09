// common.js — kode der bruges på ALLE sider (indlæses før sidens egen fil)

// OPSKRIFT setTodayIn: skriver dagens dato i det datofelt, man giver den.
// Bruges på dagbog, hormoner og start-runde, så koden kun står ét sted.
// dateField (i parentesen) = feltet, som den side der kalder opskriften har fundet med getElementById
function setTodayIn(dateField) {

    // findes feltet ikke på siden (fx ingen formular på et afsluttet forløb), så gør ingenting – ellers kommer der en fejl
    if (dateField === null) {
        return;
    }

    // spørg computeren: hvad er dato og klokkeslæt lige nu?
    const now = new Date();

    // byg "2026-09-10" af den LOKALE dato. (toISOString giver UTC-tid, og så er datoen gårsdagens mellem kl. 00 og 02 i Danmark)
    // getMonth() starter på 0, derfor + 1. padStart(2, "0") = "9" bliver til "09"
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, "0");
    const day = String(now.getDate()).padStart(2, "0");
    const today = year + "-" + month + "-" + day;

    // skriv datoen i feltet
    dateField.value = today;
}

// BEKRÆFT SLETNING: alle slet-formularer har class="delete-form" (dagbog, hormoner, medicin, dokumenter).
// Før formularen sendes, spørger browseren "Er du sikker?". Trykker man Annullér, stoppes afsendelsen.
const deleteForms = document.querySelectorAll(".delete-form");
deleteForms.forEach(function (form) {
    form.addEventListener("submit", function (event) {
        const sure = confirm("Vil du slette dette? Det kan ikke fortrydes.");
        if (!sure) {
            event.preventDefault();   // preventDefault = stop formularen, så intet sendes
        }
    });
});
