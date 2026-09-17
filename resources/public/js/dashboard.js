// dashboard.js — JavaScript kun til dashboard.html

// --- "Afslut runde": spørg først i vores egen dialog (samme mønster som slet konto på min profil) ---

// FIND "Afslut runde"-knappen og dialogen nederst i html'en
const endRoundButton = document.getElementById("end-round");
const endRoundDialog = document.getElementById("confirm-end");

// NÅR man klikker på knappen:
endRoundButton.addEventListener("click", function () {

    // åbn dialogen (modal = resten af siden låses imens)
    endRoundDialog.showModal();
});

// NÅR dialogen lukkes (uanset hvilken knap man trykkede):
endRoundDialog.addEventListener("close", function () {

    // HVIS det var "Afslut runde"-knappen (value="confirm"), der lukkede den:
    if (endRoundDialog.returnValue === "confirm") {

        // TODO: her skal backend afslutte runden (US10a). Indtil da: gå til rundehistorik
        window.location.href = "rundehistorik.html";
    }
});
