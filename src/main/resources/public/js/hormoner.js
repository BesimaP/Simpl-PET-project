// hormoner.js — JavaScript kun til hormoner.html (US9)
// TODO senere: kurve over målinger (når backend kan hente rigtige data)

// --- 1) Dagens dato i dato-feltet ---

// FIND dato-feltet
const dateField = document.getElementById("date");

// KØR opskriften fra common.js, der skriver dagens dato i feltet
setTodayIn(dateField);


// --- 2) Vælg enheden automatisk, når man vælger et hormon ---
// (østradiol måles i pmol/L, LH og FSH i IU/L osv. - det skal patienten ikke selv vide)

// FIND de to dropdowns: hormon og enhed
const hormonSelect = document.getElementById("hormone");
const unitSelect = document.getElementById("unit");

// NÅR man vælger noget nyt i hormon-dropdown'en:
hormonSelect.addEventListener("change", function () {

    // HVIS hormonet er østradiol → enhed pmol/L
    // (hormonSelect.value = value fra den valgte <option> = databasens navn)
    if (hormonSelect.value === "E2_OESTRADIOL") {
        unitSelect.value = "pmol/L";

    // ELLERS HVIS LH → IU/L
    } else if (hormonSelect.value === "LH") {
        unitSelect.value = "IU/L";

    // ELLERS HVIS FSH → IU/L
    } else if (hormonSelect.value === "FSH") {
        unitSelect.value = "IU/L";

    // ELLERS HVIS progesteron → nmol/L
    } else if (hormonSelect.value === "PROGESTERONE") {
        unitSelect.value = "nmol/L";

    // ELLERS HVIS AMH → pmol/L
    } else if (hormonSelect.value === "AMH") {
        unitSelect.value = "pmol/L";
    }
    // man kan stadig rette enheden bagefter - vi vælger bare et godt bud
});
