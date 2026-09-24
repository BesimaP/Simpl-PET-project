// aftaler.js — JavaScript kun til aftaler.html (US3)

// FIND dato-feltet i "ny aftale"-formularen
const dateField = document.getElementById("date");

// KØR setTodayIn (fra common.js): skriver dagens dato i feltet, så man slipper for at vælge den
setTodayIn(dateField);

// (sortering "nærmeste først" laves i backend med ORDER BY date_time – ikke i js)
