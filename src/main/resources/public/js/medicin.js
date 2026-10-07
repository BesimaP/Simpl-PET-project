// medicin.js — JavaScript kun til medicin.html (US8)

// FIND dato-feltet i "ny dosis"-formularen
const dateField = document.getElementById("date");

// KØR setTodayIn (fra common.js): skriver dagens dato i feltet, så man slipper for at vælge den
setTodayIn(dateField);
