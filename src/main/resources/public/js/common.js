// common.js — kode der bruges på ALLE sider (indlæses før sidens egen fil)

// OPSKRIFT setTodayIn: skriver dagens dato i det datofelt, man giver den.
// Bruges på dagbog, hormoner og start-runde, så koden kun står ét sted.
// dateField (i parentesen) = feltet, som den side der kalder opskriften har fundet med getElementById
function setTodayIn(dateField) {

    // spørg computeren: hvad er dato og klokkeslæt lige nu?
    const now = new Date();

    // lav det om til tekst, fx "2026-09-10T13:45:00.000Z"
    const text = now.toISOString();

    // behold kun de første 10 tegn: "2026-09-10" (formatet et datofelt forstår)
    const today = text.slice(0, 10);

    // skriv datoen i feltet
    dateField.value = today;
}

// TODO senere: en opskrift til at vise en fejlbesked under et felt (bruges af flere sider)

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
