// medicin.js – JavaScript kun til medicin.html (US8)

// FIND alle "markér som taget"-cirkler i medicinlisten
const cirkler = document.querySelectorAll('.med__check');

// FOR HVER cirkel
cirkler.forEach(function (cirkel) {
    // NÅR man klikker på den
    cirkel.addEventListener('click', function () {
        // find det <li> cirklen ligger i
        const li = cirkel.closest('.med');

        // sæt class "med--taken" på det <li> (css gør den grøn)
        li.classList.add('med--taken');

        // skift teksten "Planlagt" til "Taget"
        const status = li.querySelector('.med__status');
        status.textContent = 'Taget';

        // flueben inde i cirklen (css styler svg'en)
        cirkel.innerHTML =
            '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m5 12.5 4.5 4.5L19 7.5" fill="none" stroke="currentColor"/></svg>';
    });
});