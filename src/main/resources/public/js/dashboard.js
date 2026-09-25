// dashboard.js — JavaScript kun til dashboard (templates/dashboard.html)

// --- "Afslut runde": spørg først i vores egen dialog (samme mønster som slet konto på min profil) ---

// FIND "Afslut runde"-knappen og dialogen nederst i html'en
const endRoundButton = document.getElementById("end-round");
const endRoundDialog = document.getElementById("confirm-end");

// knappen og dialogen findes KUN, når der er en runde i gang (th:if i skabelonen) – ellers er de null, og vi gør ingenting
if (endRoundButton && endRoundDialog) {

    // NÅR man klikker på knappen:
    endRoundButton.addEventListener("click", function () {

        // åbn dialogen (modal = resten af siden låses imens)
        endRoundDialog.showModal();
    });
}

// Der er IKKE en "close"-handler mere: "Afslut runde"-knappen i dialogen har formmethod="post" formaction="/afslut-runde",
// så browseren sender selv formularen til serveren, og serveren sender videre til rundehistorik.
// Et window.location.href her ville afbryde den afsendelse.

// --- Notifikationer: klokken øverst åbner pop-op'en #notif-dialog (samme mønster som "Afslut runde" ovenfor) ---

// FIND klokke-knappen og dialogen nederst i html'en
const notifButton = document.getElementById("open-notifications");
const notifDialog = document.getElementById("notif-dialog");

// begge findes altid på dashboard, men vi tjekker alligevel (så koden ikke fejler, hvis html'en ændres)
if (notifButton && notifDialog) {

    // NÅR man klikker på klokken: åbn dialogen (modal = resten af siden låses imens)
    notifButton.addEventListener("click", function () {
        notifDialog.showModal();
    });
}

// "Luk" lukker via method="dialog", og "Markér alle som læst" sender selv til serveren via formaction – ingen mere JS nødvendig.
