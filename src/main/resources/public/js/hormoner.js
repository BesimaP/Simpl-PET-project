// hormoner.js – til hormoner.html (US9)

const dateField = document.getElementById("date");

// dagens dato som forslag (setTodayIn ligger i common.js)
setTodayIn(dateField);


// --- Foreslå en enhed, når man vælger et hormon ---
// så patienten ikke selv skal vide, hvilken enhed hormonet plejer at blive målt i

const hormonSelect = document.getElementById("hormone");
const unitSelect = document.getElementById("unit");

hormonSelect.addEventListener("change", function () {

    if (hormonSelect.value === "E2_OESTRADIOL") {
        unitSelect.value = "pmol/L";

    } else if (hormonSelect.value === "LH") {
        unitSelect.value = "IU/L";

    } else if (hormonSelect.value === "FSH") {
        unitSelect.value = "IU/L";

    } else if (hormonSelect.value === "PROGESTERONE") {
        unitSelect.value = "nmol/L";

    } else if (hormonSelect.value === "AMH") {
        unitSelect.value = "pmol/L";
    }
    // det er kun et forslag, enheden kan stadig rettes
});
