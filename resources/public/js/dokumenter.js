// dokumenter.js — JavaScript kun til dokumenter.html (US11)

// --- 1) Tjek filen, når man har valgt den: kun PDF/JPG/PNG og højst 10 MB ---

// FIND fil-feltet og det tomme <p id="file-error"> under det (fejlbeskeden skrives derind)
const fileInput = document.getElementById("file");
const fileError = document.getElementById("file-error");

// NÅR man har valgt en fil:
fileInput.addEventListener("change", function () {

    // slet en evt. gammel fejlbesked
    fileError.textContent = "";

    // tag den valgte fil (.files er en liste, [0] = den første)
    const file = fileInput.files[0];

    // HVIS filen hverken er pdf, jpeg eller png:
    // (file.type er fx "application/pdf" - eller "text/html" for en forkert fil. !== = "er ikke", && = "og")
    if (file.type !== "application/pdf" && file.type !== "image/jpeg" && file.type !== "image/png") {

        // vis fejl under feltet (rød via .field-error i base.css)
        fileError.textContent = "Kun PDF, JPG og PNG.";

        // tøm feltet, så den forkerte fil ikke bliver uploadet
        fileInput.value = "";
    }

    // 10 MB regnet om til bytes (det er bytes, file.size er i)
    const maxSize = 10 * 1024 * 1024;

    // HVIS filen er større end 10 MB:
    if (file.size > maxSize) {

        // vis fejl, og tøm feltet
        fileError.textContent = "Filen må højst være 10 MB.";
        fileInput.value = "";
    }
});


// --- 2) "Fjern fil"-knappen: tøm feltet, hvis man fortryder sit valg ---

// FIND knappen
const removeButton = document.getElementById("remove-file");

// NÅR man klikker på den:
removeButton.addEventListener("click", function () {

    // tøm fil-feltet (viser "Ingen fil valgt" igen) og slet en evt. fejlbesked
    fileInput.value = "";
    fileError.textContent = "";
});
