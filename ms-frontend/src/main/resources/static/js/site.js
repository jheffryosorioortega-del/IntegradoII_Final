// Efectos del portal. El carrusel, el menú móvil y los paneles desplegables los maneja Bootstrap.

// Barra de navegación: sombra en cuanto la página se desplaza un poco.
(function () {
    var nav = document.querySelector('.sgp-navbar');
    if (!nav) return;
    function actualizar() { nav.classList.toggle('scrolled', window.scrollY > 8); }
    window.addEventListener('scroll', actualizar, { passive: true });
    actualizar();
})();

// Filtros que se envían solos al cambiar la opción (data-autoenviar).
document.querySelectorAll('select[data-autoenviar]').forEach(function (s) {
    s.addEventListener('change', function () { s.form.submit(); });
});

// Aparición progresiva de secciones al hacer scroll (.reveal -> .in)
// y contadores animados (data-count="123", data-suffix="%" dentro de .num).
(function () {
    var revelables = document.querySelectorAll('.reveal');
    var contadores = document.querySelectorAll('.num[data-count]');
    if (!revelables.length && !contadores.length) return;

    function animarContador(el) {
        var destino = parseInt(el.getAttribute('data-count'), 10) || 0;
        var sufijo = el.getAttribute('data-suffix') || '';
        var duracion = 1200;
        var inicio = null;
        function paso(ts) {
            if (!inicio) inicio = ts;
            var avance = Math.min((ts - inicio) / duracion, 1);
            el.textContent = Math.round(destino * (1 - Math.pow(1 - avance, 3))) + sufijo;   // ease-out
            if (avance < 1) requestAnimationFrame(paso);
        }
        requestAnimationFrame(paso);
    }

    if (!('IntersectionObserver' in window)) {
        revelables.forEach(function (el) { el.classList.add('in'); });
        contadores.forEach(function (el) { el.textContent = el.getAttribute('data-count') + (el.getAttribute('data-suffix') || ''); });
        return;
    }

    var observador = new IntersectionObserver(function (entradas) {
        entradas.forEach(function (entrada) {
            if (!entrada.isIntersecting) return;
            if (entrada.target.classList.contains('num')) animarContador(entrada.target);
            else entrada.target.classList.add('in');
            observador.unobserve(entrada.target);
        });
    }, { threshold: 0.25 });

    revelables.forEach(function (el) { observador.observe(el); });
    contadores.forEach(function (el) { observador.observe(el); });
})();
