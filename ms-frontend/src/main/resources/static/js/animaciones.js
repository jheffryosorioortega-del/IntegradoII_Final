/* Animaciones al hacer scroll del portal (sin librerías).
   Atributos que reconoce:
     data-anim="up|left|right|zoom"   el elemento entra con movimiento al aparecer en pantalla
     data-delay="0.2"                 retraso en segundos
     data-stagger="3"                 en un contenedor: sus hijos entran escalonados (3 = columnas por fila)
     data-split                       el título entra palabra por palabra
     data-parallax="0.1"              el elemento se desplaza a distinta velocidad que el scroll
   Si el usuario prefiere menos movimiento, no se anima nada y todo se ve normal. */
(function () {
    'use strict';
    var html = document.documentElement;
    if (!html.classList.contains('anim-js')) return;   // la clase la pone el <head> solo si se permite el movimiento
    html.classList.add('anim-ready');                  // avisa al CSS que el script sí corrió (desactiva la red de seguridad)

    // ---------- Títulos palabra por palabra ----------
    document.querySelectorAll('[data-split]').forEach(function (el) {
        var texto = el.textContent.trim().replace(/\s+/g, ' ');
        el.setAttribute('aria-label', texto);
        el.textContent = '';
        texto.split(' ').forEach(function (palabra, i, todas) {
            var caja = document.createElement('span');
            caja.className = 'sgp-w';
            caja.setAttribute('aria-hidden', 'true');
            var interior = document.createElement('span');
            interior.textContent = palabra;
            interior.style.setProperty('--i', i);
            caja.appendChild(interior);
            el.appendChild(caja);
            if (i < todas.length - 1) el.appendChild(document.createTextNode(' '));
        });
        el.classList.add('anim-split');
    });

    // ---------- Escalonado de hijos ----------
    document.querySelectorAll('[data-stagger]').forEach(function (contenedor) {
        var columnas = parseInt(contenedor.getAttribute('data-stagger'), 10) || 3;
        Array.prototype.forEach.call(contenedor.children, function (hijo, i) {
            if (!hijo.hasAttribute('data-anim')) hijo.setAttribute('data-anim', 'up');
            if (!hijo.hasAttribute('data-delay')) hijo.setAttribute('data-delay', ((i % columnas) * 0.12).toFixed(2));
        });
    });

    // ---------- Entrada al aparecer en pantalla ----------
    var objetivos = document.querySelectorAll('[data-anim], [data-split]');
    objetivos.forEach(function (el) {
        var d = el.getAttribute('data-delay');
        if (d) el.style.setProperty('--d', d + 's');
    });
    if (!('IntersectionObserver' in window)) {
        objetivos.forEach(function (el) { el.classList.add('anim-in'); });
    } else {
        var obs = new IntersectionObserver(function (entradas) {
            entradas.forEach(function (e) {
                if (!e.isIntersecting) return;
                e.target.classList.add('anim-in');
                obs.unobserve(e.target);
            });
        }, { threshold: 0.15, rootMargin: '0px 0px -6% 0px' });
        objetivos.forEach(function (el) { obs.observe(el); });
    }

    // ---------- Parallax (al hacer scroll) ----------
    var hero = document.querySelector('.sgp-hero');
    var textosHero = hero ? hero.querySelectorAll('.sgp-slide-inner > .container') : [];
    var paralelos = document.querySelectorAll('[data-parallax]');
    if (!textosHero.length && !paralelos.length) return;

    var pendiente = false;
    function actualizar() {
        pendiente = false;
        var vh = window.innerHeight;
        var y = window.scrollY;
        if (hero) {
            var alto = hero.offsetHeight;
            if (y < alto) {
                var avance = y / alto;
                textosHero.forEach(function (t) {
                    t.style.translate = '0 ' + Math.round(y * 0.28) + 'px';
                    t.style.opacity = String(Math.max(0, 1 - avance * 1.25));
                });
            }
        }
        paralelos.forEach(function (el) {
            var r = el.getBoundingClientRect();
            if (r.bottom < -100 || r.top > vh + 100) return;
            var factor = parseFloat(el.getAttribute('data-parallax')) || 0.1;
            var centro = r.top + r.height / 2 - (el._dy || 0) - vh / 2;   // sin contar el desplazamiento ya aplicado
            el._dy = Math.round(-centro * factor);
            el.style.translate = '0 ' + el._dy + 'px';
        });
    }
    function pedir() { if (!pendiente) { pendiente = true; requestAnimationFrame(actualizar); } }
    window.addEventListener('scroll', pedir, { passive: true });
    window.addEventListener('resize', pedir);
    actualizar();
})();
