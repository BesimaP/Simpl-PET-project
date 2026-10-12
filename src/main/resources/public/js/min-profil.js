// min-profil.js – til min-profil.html (US6b)

// --- Skift adgangskode: de to nye koder skal være ens ---

const newPasswordField = document.getElementById("new-password");
const repeatPasswordField = document.getElementById("repeat-password");
const passwordError = document.getElementById("password-error");

// der er tre formularer på siden, så vi tager den, feltet ligger i
const passwordForm = newPasswordField.closest("form");

passwordForm.addEventListener("submit", function (event) {

    if (newPasswordField.value !== repeatPasswordField.value) {

        passwordError.textContent = "De to adgangskoder er ikke ens.";

        event.preventDefault();
    }
});


// --- Slet konto: spørg først, så man ikke sletter ved en fejl (Nielsen #5) ---

const deleteLink = document.getElementById("delete-account");
const deleteDialog = document.getElementById("confirm-delete");

deleteLink.addEventListener("click", function (event) {

    // linket til /slet-konto er til dem uden JavaScript, her viser vi dialogen i stedet
    event.preventDefault();

    deleteDialog.showModal();
});

// Vi har ikke en close-handler, fordi knappen i dialogen selv sender til /slet-konto via formaction.
// Et window.location.href her ville afbryde afsendelsen.
