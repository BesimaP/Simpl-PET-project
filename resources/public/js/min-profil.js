// min-profil.js — JavaScript kun til min-profil.html (US6b)

// --- 1) Skift adgangskode: de to nye koder skal være ens ---

// FIND de to felter og det tomme <p id="password-error"> under dem
const newPasswordField = document.getElementById("new-password");
const repeatPasswordField = document.getElementById("repeat-password");
const passwordError = document.getElementById("password-error");

// FIND den rigtige formular (der er tre på siden): den nærmeste <form> udenom feltet
const passwordForm = newPasswordField.closest("form");

// NÅR formularen sendes (tryk på "Skift adgangskode"):
passwordForm.addEventListener("submit", function (event) {

    // HVIS de to koder ikke er ens:
    if (newPasswordField.value !== repeatPasswordField.value) {

        // vis fejl under feltet
        passwordError.textContent = "De to adgangskoder er ikke ens.";

        // og stop formularen i at blive sendt
        event.preventDefault();
    }
});


// --- 2) Slet konto: spørg først, så man ikke sletter ved en fejl (Nielsen #5) ---

// FIND "Slet konto"-linket og vores egen <dialog> nederst i html'en
const deleteLink = document.getElementById("delete-account");
const deleteDialog = document.getElementById("confirm-delete");

// NÅR man klikker på linket:
deleteLink.addEventListener("click", function (event) {

    // linket skal ikke gå nogen steder af sig selv
    event.preventDefault();

    // åbn dialogen (modal = resten af siden låses imens)
    deleteDialog.showModal();
});

// NÅR dialogen lukkes (uanset hvilken knap man trykkede):
deleteDialog.addEventListener("close", function () {

    // HVIS det var "Slet konto"-knappen (value="confirm"), der lukkede den:
    if (deleteDialog.returnValue === "confirm") {

        // TODO: her skal backend slette kontoen. Indtil da: gå til login
        window.location.href = "login.html";
    }
});
