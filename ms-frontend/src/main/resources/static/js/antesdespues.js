/* Comparador "Antes y después": el cliente arrastra la barra (mouse o dedo) o usa las flechas del teclado.
   Sin librerías. Marcado esperado: <div class="sgp-ba" data-ba> ... <input type="range"> </div> */
(function () {
    'use strict';
    document.querySelectorAll('[data-ba]').forEach(function (caja) {
        var rango = caja.querySelector('input[type="range"]');
        var arrastrando = false;

        function poner(valor) {
            valor = Math.max(0, Math.min(100, valor));
            caja.style.setProperty('--pos', valor + '%');
            rango.value = String(Math.round(valor));
        }
        function desdeEvento(e) {
            var r = caja.getBoundingClientRect();
            poner(((e.clientX - r.left) / r.width) * 100);
        }

        caja.addEventListener('pointerdown', function (e) {
            arrastrando = true;
            caja.classList.add('arrastrando');
            if (caja.setPointerCapture) { caja.setPointerCapture(e.pointerId); }
            desdeEvento(e);
        });
        caja.addEventListener('pointermove', function (e) { if (arrastrando) { desdeEvento(e); } });
        ['pointerup', 'pointercancel', 'lostpointercapture'].forEach(function (ev) {
            caja.addEventListener(ev, function () { arrastrando = false; caja.classList.remove('arrastrando'); });
        });

        // Teclado y lectores de pantalla: el input range oculto mueve la barra.
        rango.addEventListener('input', function () { poner(Number(rango.value)); });
        poner(Number(rango.value));
    });
})();
