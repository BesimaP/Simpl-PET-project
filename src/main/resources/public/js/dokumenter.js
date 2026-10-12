// dokumenter.js – til dokumenter.html (US11)

// --- Tjek filen med det samme: PDF, JPG eller PNG og højst 10 MB. Serveren tjekker også ---

const fileInput = document.getElementById("file");
const fileError = document.getElementById("file-error");

fileInput.addEventListener("change", function () {

    fileError.textContent = "";

    const file = fileInput.files[0];

    // ingen fil, hvis man trykkede Annullér i filvælgeren
    if (!file) {
        return;
    }

    if (file.type !== "application/pdf" && file.type !== "image/jpeg" && file.type !== "image/png") {

        fileError.textContent = "Kun PDF, JPG og PNG.";

        // tøm feltet, så filen ikke bliver uploadet
        fileInput.value = "";
        return;   // ellers overskriver næste tjek fejlbeskeden
    }

    // file.size er i bytes
    const maxSize = 10 * 1024 * 1024;

    if (file.size > maxSize) {

        fileError.textContent = "Filen må højst være 10 MB.";
        fileInput.value = "";
    }
});


// --- "Fjern fil": hvis man fortryder sit valg ---

const removeButton = document.getElementById("remove-file");

removeButton.addEventListener("click", function () {

    fileInput.value = "";
    fileError.textContent = "";
});
