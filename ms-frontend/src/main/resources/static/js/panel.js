/* Gráficos del tablero. Sin librerías: la política de seguridad solo permite scripts propios.
   Lee los números que el servidor dejó en atributos data-* y los dibuja:
   data-tuberia (segmentos con data-valor), data-pct (barras) y data-segs (dona). */
(function () {
    'use strict';
    var anima = document.documentElement.classList.contains('anim-js');

    function dos(f) { requestAnimationFrame(function () { requestAnimationFrame(f); }); }

    // Tubería de etapas: cada tramo crece según cuántas solicitudes hay en esa etapa.
    document.querySelectorAll('[data-tuberia]').forEach(function (tubo) {
        function aplicar() {
            var vacia = tubo.classList.contains('vacia');
            tubo.querySelectorAll('[data-valor]').forEach(function (s) {
                s.style.setProperty('--g', s.getAttribute('data-valor'));
                if (!vacia && s.getAttribute('data-valor') === '0') s.hidden = true;
            });
            tubo.classList.add('lista');
        }
        if (anima) dos(aplicar); else aplicar();
    });

    // Barras horizontales.
    var barras = document.querySelectorAll('[data-pct]');
    function pintarBarras() { barras.forEach(function (b) { b.style.setProperty('--w', b.getAttribute('data-pct') + '%'); }); }
    if (anima) dos(pintarBarras); else pintarBarras();

    // Dona: un arco por cada valor.
    document.querySelectorAll('[data-segs]').forEach(function (svg) {
        var valores = svg.getAttribute('data-segs').split(',').map(Number);
        var clases = svg.getAttribute('data-clases').split(',');
        var total = valores.reduce(function (a, b) { return a + b; }, 0);
        var ns = 'http://www.w3.org/2000/svg';
        function circulo(clase) {
            var c = document.createElementNS(ns, 'circle');
            c.setAttribute('cx', '21'); c.setAttribute('cy', '21'); c.setAttribute('r', '15.9155');
            c.setAttribute('class', clase);
            svg.appendChild(c);
            return c;
        }
        circulo('pista');
        if (!total) return;
        var acumulado = 0;
        valores.forEach(function (v, i) {
            if (!v) return;
            var largo = v / total * 100;
            var visible = Math.max(largo - (valores.filter(Boolean).length > 1 ? 1.2 : 0), 0.5);
            var arco = circulo('arco ' + clases[i]);
            arco.setAttribute('stroke-dashoffset', String(-acumulado));
            arco.setAttribute('stroke-dasharray', anima ? '0 100' : visible + ' ' + (100 - visible));
            if (anima) dos(function () { arco.setAttribute('stroke-dasharray', visible + ' ' + (100 - visible)); });
            acumulado += largo;
        });
    });
})();