// tidslinje.js – JavaScript kun til tidslinje.html

// FIND tidslinjen (listen af hændelser)
const tidslinje = document.querySelector('.rounds');
const sektion = tidslinje.closest('.card');

// Lav en formular og sæt den ind over listen
const formular = document.createElement('form');
formular.innerHTML = `
  <label>Dato <input type="date" name="dato" required></label>
  <label>Type <input type="text" name="type" placeholder="fx Blodprøve" required></label>
  <label>Beskrivelse <input type="text" name="beskrivelse" placeholder="fx Østradiol 450 pmol/L"></label>
  <button type="submit">Tilføj begivenhed</button>
`;
sektion.insertBefore(formular, tidslinje);

// Gør en dato som "2026-09-05" pæn: "5. september"
function formaterDato(iso) {
    return new Date(iso).toLocaleDateString('da-DK', { day: 'numeric', month: 'long' });
}

// NÅR en ny hændelse tilføjes
formular.addEventListener('submit', function (event) {
    // stop siden i at genindlæse
    event.preventDefault();

    const dato = formular.elements.dato.value;
    const type = formular.elements.type.value;
    const beskrivelse = formular.elements.beskrivelse.value;

    // lav et nyt <li> med dato, type og beskrivelse
    const nytLi = document.createElement('li');
    nytLi.dataset.dato = dato;

    const info = document.createElement('div');
    info.className = 'round__info';

    const titel = document.createElement('strong');
    titel.textContent = type;

    const meta = document.createElement('span');
    meta.textContent = formaterDato(dato) + (beskrivelse ? ' · ' + beskrivelse : '');

    info.appendChild(titel);
    info.appendChild(meta);
    nytLi.appendChild(info);

    // put det ind i listen på det rigtige sted (sorteret efter dato, nyeste øverst)
    const eksisterende = Array.from(tidslinje.querySelectorAll('li'));
    const foerste_aeldre = eksisterende.find(function (li) {
        return li.dataset.dato < dato;
    });

    if (foerste_aeldre) {
        tidslinje.insertBefore(nytLi, foerste_aeldre);
    } else {
        tidslinje.appendChild(nytLi);
    }

    formular.reset();
});