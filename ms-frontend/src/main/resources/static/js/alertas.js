/* Alerta de solicitudes nuevas del panel interno.
   Cada 15 segundos pregunta al servidor si llegó algo después de la última solicitud que ya conoce.
   Si llegó: muestra un aviso con los datos básicos y un enlace a la ficha, suena un pitido corto
   y actualiza el contador rojo del menú y el título de la pestaña.
   Sin librerías y sin estilos en línea: la política de seguridad (CSP) solo permite scripts propios. */
(function () {
    'use strict';
    var menu = document.getElementById('panelMenu');
    if (!menu) return;
    var ultimo = parseInt(menu.getAttribute('data-ultimo-id'), 10);
    var url = menu.getAttribute('data-alertas-url');
    if (isNaN(ultimo) || !url) return;   // sin punto de partida no se avisa (evita falsas alertas)

    var tituloBase = document.title;
    var MAX_AVISOS = 3;
    var zona = document.createElement('div');
    zona.className = 'sgp-avisos';
    zona.setAttribute('role', 'status');
    zona.setAttribute('aria-live', 'polite');
    document.body.appendChild(zona);

    function pitar() {
        try {
            var Ctx = window.AudioContext || window.webkitAudioContext;
            if (!Ctx) return;
            var a = new Ctx(), o = a.createOscillator(), g = a.createGain(), t = a.currentTime;
            o.type = 'sine'; o.frequency.value = 880;
            g.gain.setValueAtTime(0.0001, t);
            g.gain.exponentialRampToValueAtTime(0.18, t + 0.03);
            g.gain.exponentialRampToValueAtTime(0.0001, t + 0.5);
            o.connect(g); g.connect(a.destination); o.start(t); o.stop(t + 0.5);
        } catch (e) { /* el navegador puede bloquear el sonido hasta que la persona toque la página */ }
    }

    function contador(n) {
        var enlace = menu.querySelector('a[href$="/panel/solicitudes"]');
        if (enlace) {
            var b = enlace.querySelector('.sgp-contador');
            if (n > 0) {
                if (!b) { b = document.createElement('b'); b.className = 'sgp-contador'; enlace.appendChild(b); }
                b.textContent = String(n);
                b.title = n + ' sin revisar';
            } else if (b) { b.remove(); }
        }
        document.title = n > 0 ? '(' + n + ') ' + tituloBase : tituloBase;
    }

    function texto(el, tag, clase, valor) {
        var n = document.createElement(tag);
        if (clase) n.className = clase;
        n.textContent = valor;
        el.appendChild(n);
        return n;
    }

    function aviso(s) {
        while (zona.children.length >= MAX_AVISOS) zona.removeChild(zona.firstChild);
        var caja = document.createElement('div');
        caja.className = 'sgp-aviso ' + (s.clase || 'ur-media');
        var cab = document.createElement('div');
        cab.className = 'sgp-aviso-cab';
        texto(cab, 'strong', '', 'Llegó una solicitud nueva');
        var cerrar = texto(cab, 'button', 'sgp-aviso-cerrar', '×');
        cerrar.type = 'button';
        cerrar.setAttribute('aria-label', 'Cerrar el aviso');
        cerrar.addEventListener('click', function () { caja.remove(); });
        caja.appendChild(cab);
        texto(caja, 'span', 'sgp-aviso-quien', (s.codigo ? s.codigo + ' · ' : '') + s.solicitante);
        texto(caja, 'span', 'sgp-aviso-que', s.servicio + (s.prioridad ? ' · Prioridad ' + s.prioridad.toLowerCase() : ''));
        var ver = texto(caja, 'a', 'btn btn-primary btn-sm', 'Ver los datos');
        ver.href = url.replace(/alertas$/, 'solicitudes/') + s.id;
        zona.appendChild(caja);
    }

    function consultar() {
        fetch(url + '?desde=' + ultimo, { credentials: 'same-origin', headers: { 'Accept': 'application/json' } })
            .then(function (r) { if (!r.ok) throw new Error('sin respuesta'); return r.json(); })
            .then(function (d) {
                contador(d.sinRevisar);
                if (d.nuevas && d.nuevas.length) {
                    d.nuevas.forEach(aviso);
                    pitar();
                }
                if (typeof d.ultimoId === 'number' && d.ultimoId > ultimo) ultimo = d.ultimoId;
            })
            .catch(function () { /* sin conexión o sesión vencida: se reintenta en la próxima vuelta */ });
    }

    setInterval(function () { if (!document.hidden) consultar(); }, 15000);
    document.addEventListener('visibilitychange', function () { if (!document.hidden) consultar(); });
})();
