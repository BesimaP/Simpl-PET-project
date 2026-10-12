// common.js – fælles kode til alle sider. Skal indlæses før sidens egen fil

// Skriver dagens dato i et datofelt. Bruges på flere sider, så den ligger her.
function setTodayIn(dateField) {

    // feltet findes ikke altid, fx er der ingen formular på et afsluttet forløb
    if (dateField === null) {
        return;
    }

    const now = new Date();

    // vi bygger datoen selv ud fra lokal tid. toISOString bruger UTC og gav gårsdagens dato mellem kl. 00 og 02
    // getMonth() starter på 0, derfor + 1
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, "0");
    const day = String(now.getDate()).padStart(2, "0");
    const today = year + "-" + month + "-" + day;

    dateField.value = today;
}

// Alle slet-formularer har class="delete-form". Vi spørger, før der bliver slettet (Nielsen #5)
const deleteForms = document.querySelectorAll(".delete-form");
deleteForms.forEach(function (form) {
    form.addEventListener("submit", function (event) {
        const sure = confirm("Vil du slette dette? Det kan ikke fortrydes.");
        if (!sure) {
            event.preventDefault();
        }
    });
});
