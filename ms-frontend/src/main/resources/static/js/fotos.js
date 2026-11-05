/* Fotos del problema en el formulario de contacto.
   Muestra una vista previa de cada foto, deja quitar las que sobran y reduce las fotos del celular
   (que pueden pesar 5 MB o más) antes de enviarlas: máximo 1600 px de lado, en JPG.
   Sin librerías y sin estilos en línea: la política de seguridad (CSP) solo permite scripts propios. */
(function () {
    'use strict';
    var caja = document.querySelector('[data-subir]');
    if (!caja || !window.FileReader || !window.DataTransfer) return;   // sin soporte: el campo funciona igual, sin vista previa

    var entrada = caja.querySelector('input[type=file]');
    var lista = caja.querySelector('[data-subir-lista]');
    var estado = caja.querySelector('[data-subir-estado]');
    var form = entrada.form;
    var enviar = form ? form.querySelector('button[type=submit]') : null;
    var MAX = parseInt(caja.getAttribute('data-max'), 10) || 4;
    var MAX_BYTES = (parseInt(caja.getAttribute('data-max-mb'), 10) || 6) * 1024 * 1024;
    var LADO = 1600;
    var TIPOS = ['image/jpeg', 'image/png', 'image/webp'];
    var fotos = [];       // archivos ya listos, en el orden en que se eligieron
    var pendientes = 0;   // fotos que todavía se están reduciendo

    function mensaje(texto, error) {
        estado.textContent = texto;
        estado.classList.toggle('error', !!error);
    }

    function resumen() {
        if (!fotos.length) return mensaje('');
        mensaje(fotos.length + (fotos.length === 1 ? ' foto lista' : ' fotos listas') + ' de ' + MAX + '. Puedes quitar o agregar otras.');
    }

    // Lo que se envía es lo que está en "fotos": se vuelve a poner en el campo del formulario.
    function sincronizar() {
        var dt = new DataTransfer();
        fotos.forEach(function (f) { dt.items.add(f); });
        entrada.files = dt.files;
        caja.classList.toggle('llena', fotos.length >= MAX);
    }

    function dibujar() {
        lista.textContent = '';
        fotos.forEach(function (f, i) {
            var li = document.createElement('li');
            var img = document.createElement('img');
            img.alt = 'Vista previa de la foto ' + (i + 1);
            var lector = new FileReader();
            lector.onload = function () { img.src = lector.result; };
            lector.readAsDataURL(f);
            var quitar = document.createElement('button');
            quitar.type = 'button';
            quitar.className = 'sgp-subir-quitar';
            quitar.setAttribute('aria-label', 'Quitar la foto ' + (i + 1));
            var x = document.createElement('i');
            x.className = 'bi bi-x-lg';
            quitar.appendChild(x);
            quitar.addEventListener('click', function () {
                fotos.splice(i, 1);
                sincronizar(); dibujar(); resumen();
            });
            li.appendChild(img);
            li.appendChild(quitar);
            lista.appendChild(li);
        });
    }

    // Reduce la foto si es grande; si algo falla, se usa tal cual.
    function reducir(archivo) {
        return new Promise(function (listo) {
            if (!window.createImageBitmap) return listo(archivo);
            createImageBitmap(archivo).then(function (bm) {
                var escala = Math.min(1, LADO / Math.max(bm.width, bm.height));
                if (escala === 1 && archivo.size <= 800 * 1024 && archivo.type === 'image/jpeg') { if (bm.close) bm.close(); return listo(archivo); }
                var c = document.createElement('canvas');
                c.width = Math.round(bm.width * escala);
                c.height = Math.round(bm.height * escala);
                var g = c.getContext('2d');
                g.fillStyle = '#fff';
                g.fillRect(0, 0, c.width, c.height);
                g.drawImage(bm, 0, 0, c.width, c.height);
                if (bm.close) bm.close();
                c.toBlob(function (b) {
                    if (!b || (escala === 1 && b.size >= archivo.size)) return listo(archivo);
                    var nombre = archivo.name.replace(/\.[^.]+$/, '') + '.jpg';
                    listo(new File([b], nombre, { type: 'image/jpeg' }));
                }, 'image/jpeg', 0.82);
            }).catch(function () { listo(archivo); });
        });
    }

    function agregar(archivos) {
        var nuevos = Array.prototype.slice.call(archivos);
        var problema = '';
        var aceptados = [];
        nuevos.forEach(function (f) {
            if (TIPOS.indexOf(f.type) === -1) { problema = 'Solo se aceptan fotos JPG, PNG o WebP.'; return; }
            if (fotos.length + aceptados.length >= MAX) { problema = 'Máximo ' + MAX + ' fotos. Quita alguna para agregar otra.'; return; }
            aceptados.push(f);
        });
        if (!aceptados.length) { sincronizar(); return mensaje(problema, true); }
        pendientes += aceptados.length;
        if (enviar) enviar.disabled = true;
        mensaje('Preparando ' + (aceptados.length === 1 ? 'la foto…' : 'las fotos…'));
        aceptados.forEach(function (f) {
            reducir(f).then(function (listo) {
                pendientes--;
                if (listo.size > MAX_BYTES) problema = '«' + f.name + '» pesa más de ' + (MAX_BYTES / 1048576) + ' MB.';
                else fotos.push(listo);
                if (pendientes === 0) {
                    if (enviar) enviar.disabled = false;
                    sincronizar(); dibujar();
                    if (problema) mensaje(problema, true); else resumen();
                }
            });
        });
    }

    entrada.addEventListener('change', function () { agregar(entrada.files); });

    ['dragenter', 'dragover'].forEach(function (n) {
        caja.addEventListener(n, function (e) { e.preventDefault(); caja.classList.add('arrastrando'); });
    });
    ['dragleave', 'drop'].forEach(function (n) {
        caja.addEventListener(n, function (e) { e.preventDefault(); caja.classList.remove('arrastrando'); });
    });
    caja.addEventListener('drop', function (e) { if (e.dataTransfer && e.dataTransfer.files.length) agregar(e.dataTransfer.files); });
})();
