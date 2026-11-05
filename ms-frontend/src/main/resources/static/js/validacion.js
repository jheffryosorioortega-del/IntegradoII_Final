/* ===================================================================
   Validación en el navegador (primera barrera; la que cuenta está en los microservicios).
   - Los campos con data-filtro NO dejan escribir caracteres no permitidos:
       nombre    -> solo letras, espacios, ' . -   (los números no se pueden escribir)
       telefono  -> solo dígitos
       direccion -> letras, números, espacios y . , # ° / -
       distrito  -> solo letras y espacios
       correo    -> sin espacios
       descripcion -> sin los símbolos < >
   - Al pegar (Ctrl+V), autocompletar o escribir texto de golpe, primero se LIMPIA y luego se inserta;
     así "+51 987 654 321" queda como 987654321 y no se pierden dígitos por el límite de caracteres.
   - Los formularios .needs-validation se revisan antes de enviarse (estilos de Bootstrap).
   =================================================================== */
(function () {
    'use strict';

    var L = 'A-Za-zÁÉÍÓÚÜÑáéíóúüñ';
    var FILTROS = {
        nombre:      { sobra: new RegExp("[^" + L + "'. \\-]", 'g'), inicio: 'letra', aviso: 'Solo letras y espacios: los números no están permitidos.' },
        telefono:    { sobra: /\D/g, aviso: 'Solo números.',
                       // "+51 987 654 321" -> 987654321 (se descarta el prefijo de país)
                       post: function (d) { return /^51\d{9}$/.test(d) ? d.slice(2) : d; } },
        direccion:   { sobra: new RegExp("[^" + L + "0-9 .,#°/\\-]", 'g'), inicio: 'alfanumerico', aviso: 'Solo letras, números y . , # ° / -' },
        distrito:    { sobra: new RegExp("[^" + L + " .'\\-]", 'g'), inicio: 'letra', aviso: 'Solo letras y espacios.' },
        correo:      { sobra: /\s/g, aviso: 'El correo no lleva espacios.' },
        descripcion: { sobra: /[<>]/g, aviso: 'No se permiten los símbolos < ni >.' }
    };

    var INICIO_NO_LETRA = new RegExp('^[^' + L + ']+');
    var INICIO_NO_ALNUM = new RegExp('^[^' + L + '0-9]+');

    /** Limpia un valor completo: quita lo no permitido, exige empezar bien y colapsa espacios repetidos. */
    function limpiar(valor, f) {
        var n = valor.replace(f.sobra, '');
        if (f.inicio === 'letra') n = n.replace(INICIO_NO_LETRA, '');
        else if (f.inicio === 'alfanumerico') n = n.replace(INICIO_NO_ALNUM, '');
        return n.replace(/ {2,}/g, ' ');
    }

    // Aviso que NO mueve la página: el campo parpadea en rojo y se resalta su texto de ayuda (.form-text).
    // (Insertar una línea nueva haría "saltar" el formulario mientras la persona escribe.)
    var temporizadores = new WeakMap();
    function avisar(el, texto) {
        var nota = el.parentElement.querySelector('.form-text');
        el.classList.add('sgp-rechazo');
        if (nota) nota.classList.add('text-danger', 'fw-semibold');
        el.setAttribute('title', texto);
        clearTimeout(temporizadores.get(el));
        temporizadores.set(el, setTimeout(function () {
            el.classList.remove('sgp-rechazo');
            if (nota) nota.classList.remove('text-danger', 'fw-semibold');
        }, 1400));
    }

    /** Inserta texto en la posición del cursor, ya limpio y respetando el máximo de caracteres. */
    function insertar(el, texto, f) {
        var frag = texto.replace(f.sobra, '');
        if (f.post) frag = f.post(frag);
        var v = el.value;
        var ini = null, fin = null;
        try { ini = el.selectionStart; fin = el.selectionEnd; } catch (e) { /* type=email */ }
        if (ini === null || ini === undefined) { ini = fin = v.length; }
        var nuevo = limpiar(v.slice(0, ini) + frag + v.slice(fin), f);
        if (el.maxLength > 0) nuevo = nuevo.slice(0, el.maxLength);
        el.value = nuevo;
        try { var pos = Math.min(ini + frag.length, nuevo.length); el.setSelectionRange(pos, pos); } catch (e) { /* ignorar */ }
        if (frag.length !== texto.length) avisar(el, f.aviso);
        el.dispatchEvent(new Event('input', { bubbles: true }));
    }

    function enlazar(el) {
        var f = FILTROS[el.getAttribute('data-filtro')];
        if (!f) return;

        // Una sola tecla no permitida: se bloquea antes de que aparezca.
        // Texto de varios caracteres (autocompletar, escritura de golpe): se limpia y se inserta.
        el.addEventListener('beforeinput', function (e) {
            if (e.inputType !== 'insertText' || typeof e.data !== 'string' || !e.data.length) return;
            if (e.data.length === 1) {
                f.sobra.lastIndex = 0;
                if (f.sobra.test(e.data)) { e.preventDefault(); avisar(el, f.aviso); }
            } else {
                e.preventDefault();
                insertar(el, e.data, f);
            }
        });

        // Pegar (Ctrl+V / menú contextual): se limpia el texto del portapapeles antes de insertarlo.
        el.addEventListener('paste', function (e) {
            var cb = e.clipboardData || window.clipboardData;
            if (!cb) return;
            e.preventDefault();
            insertar(el, cb.getData('text') || '', f);
        });

        // Último recurso (arrastrar texto, autocompletado del navegador): se limpia lo que haya entrado.
        el.addEventListener('input', function () {
            var antes = el.value, despues = limpiar(antes, f);
            if (despues !== antes) { el.value = despues; avisar(el, f.aviso); }
            el.classList.remove('is-invalid');
        });

        // Al salir del campo: espacios sobrantes fuera.
        el.addEventListener('blur', function () {
            var t = el.value.replace(/\s+/g, ' ').trim();
            if (t !== el.value) el.value = t;
        });

        var inicial = limpiar(el.value, f);
        if (inicial !== el.value) el.value = inicial;
    }

    document.querySelectorAll('[data-filtro]').forEach(enlazar);

    // Contador de caracteres (data-contador-de="id")
    document.querySelectorAll('[data-contador-de]').forEach(function (span) {
        var campo = document.getElementById(span.getAttribute('data-contador-de'));
        if (!campo) return;
        var pintar = function () { span.textContent = campo.value.length; };
        campo.addEventListener('input', pintar);
        pintar();
    });

    // Revisión previa al envío (Bootstrap: .needs-validation + .was-validated)
    document.querySelectorAll('form.needs-validation').forEach(function (form) {
        form.addEventListener('submit', function (e) {
            if (!form.checkValidity()) {
                e.preventDefault();
                e.stopPropagation();
                var primero = form.querySelector(':invalid');
                if (primero) primero.focus();
            }
            form.classList.add('was-validated');
        });
    });
})();
