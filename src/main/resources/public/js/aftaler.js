// aftaler.js – til aftaler.html (US3)

const dateField = document.getElementById("date");

// dagens dato som forslag, så man ikke selv skal vælge den
setTodayIn(dateField);

// sorteringen klares i SQL med ORDER BY date_time
