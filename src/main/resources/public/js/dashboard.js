// dashboard.js – til dashboard.html

// --- Afslut runde: spørg først i en dialog (samme som slet konto på min profil) ---

const endRoundButton = document.getElementById("end-round");
const endRoundDialog = document.getElementById("confirm-end");

// de findes kun, når der er en runde i gang
if (endRoundButton && endRoundDialog) {

    endRoundButton.addEventListener("click", function (event) {

        // linket til /afslut-runde er til dem uden JavaScript, her viser vi dialogen i stedet
        event.preventDefault();

        endRoundDialog.showModal();
    });
}

// Vi har ikke en close-handler, fordi knappen i dialogen selv sender til /afslut-runde via formaction.
// Et window.location.href her ville afbryde afsendelsen.

// --- Notifikationer: klokken åbner pop-op'en ---

const notifButton = document.getElementById("open-notifications");
const notifDialog = document.getElementById("notif-dialog");

// de findes altid, men vi tjekker alligevel, hvis HTML'en bliver ændret
if (notifButton && notifDialog) {

    notifButton.addEventListener("click", function (event) {
        event.preventDefault();   // uden JavaScript er klokken bare et link
        notifDialog.showModal();
    });
}

// Luk og "Markér alle som læst" klares af method="dialog" og formaction, så der skal ikke mere JS til.
