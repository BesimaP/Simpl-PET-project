// notifikationer.js – JavaScript kun til notifikationer.html

// FIND alle notifikationerne i listen
const notifikationer = document.querySelectorAll('.rounds li');
const markerAlleKnap = document.querySelector('.muted-link');
const klokke = document.querySelector('.iconbtn');

// Den røde prik findes ikke i HTML, så vi laver den her og sætter den på klokken
const dot = document.createElement('span');
dot.className = 'dot';
klokke.appendChild(dot);

// Markér én notifikation som læst
function markerSomLaest(notifikation) {
    // sæt en class "is-read" på den (css gør den grå)
    notifikation.classList.add('is-read');

    // fjern "Ny"-pillen, hvis den har en
    const pill = notifikation.querySelector('.pill');
    if (pill) {
        pill.remove();
    }
}

function opdaterDot() {
    // TÆL de ulæste (dem uden "is-read")
    const ulaeste = document.querySelectorAll('.rounds li:not(.is-read)').length;

    // HVIS der er 0 ulæste: skjul den røde prik - ellers vis den
    if (ulaeste === 0) {
        dot.style.display = 'none';
    } else {
        dot.style.display = 'block';
    }
}

// FOR HVER notification
notifikationer.forEach(function (notifikation) {
    // Notifikationer uden "Ny"-pille er allerede læst fra start
    if (!notifikation.querySelector('.pill')) {
        notifikation.classList.add('is-read');
    }

    // NÅR man klikker på den
    notifikation.addEventListener('click', function () {
        markerSomLaest(notifikation);
        opdaterDot();
    });
});

// "Markér alle som læst"
markerAlleKnap.addEventListener('click', function () {
    notifikationer.forEach(markerSomLaest);
    opdaterDot();
});

// Kør én gang ved load, så prikken er korrekt fra start
opdaterDot();