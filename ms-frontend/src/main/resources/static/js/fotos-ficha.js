/* Visor de las fotos del cliente en la ficha de una solicitud: ampliar, pasar con las flechas y cerrar con Esc.
   Sin librerías y sin estilos en línea (CSP). */
(function () {
    'use strict';
    var visor = document.getElementById('visor');
    if (!visor || typeof visor.showModal !== 'function') return;
    var botones = Array.prototype.slice.call(document.querySelectorAll('[data-visor] .sgp-foto'));
    if (!botones.length) return;
    var img = document.getElementById('visor-img');
    var pie = document.getElementById('visor-pie');
    var actual = 0;

    visor.classList.toggle('una', botones.length === 1);

    function ver(n) {
        actual = (n + botones.length) % botones.length;
        var b = botones[actual];
        img.src = b.getAttribute('data-src');
        img.alt = 'Foto ' + (actual + 1) + ' de ' + botones.length + ' que envió el cliente';
        pie.textContent = 'Foto ' + (actual + 1) + ' de ' + botones.length + ' · ' + b.getAttribute('data-nombre');
    }

    botones.forEach(function (b, n) {
        b.addEventListener('click', function () { ver(n); visor.showModal(); });
    });
    visor.querySelector('[data-visor-cerrar]').addEventListener('click', function () { visor.close(); });
    visor.querySelector('[data-visor-ant]').addEventListener('click', function () { ver(actual - 1); });
    visor.querySelector('[data-visor-sig]').addEventListener('click', function () { ver(actual + 1); });
    visor.addEventListener('click', function (e) { if (e.target === visor) visor.close(); });   // clic fuera de la foto
    visor.addEventListener('keydown', function (e) {
        if (e.key === 'ArrowLeft') { e.preventDefault(); ver(actual - 1); }
        if (e.key === 'ArrowRight') { e.preventDefault(); ver(actual + 1); }
    });
    visor.addEventListener('close', function () { img.removeAttribute('src'); });
})();
