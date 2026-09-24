// dagbog.js — JavaScript kun til dagbog.html (US4)

// --- 1) Dagens dato i dato-feltet ---

// FIND dato-feltet
const datoFelt = document.getElementById("date");

// KØR opskriften fra common.js, der skriver dagens dato i feltet
setTodayIn(datoFelt);


// --- 2) Tæl noterne og skriv tallet under listen ---

// FIND alle noterne (hvert <li> inde i <ul class="notes"> er én note)
const noter = document.querySelectorAll(".notes li");

// tæl hvor mange der er
const count = noter.length;

// FIND teksten "x noter i denne runde"
const countText = document.getElementById("note-count");

// skriv tallet i teksten
countText.textContent = count + " noter i denne runde.";
