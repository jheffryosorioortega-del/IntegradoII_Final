(function () {
    'use strict';
    const dni = document.getElementById('dni-consulta');
    const boton = document.getElementById('consultar-dni');
    const nombre = document.getElementById('nombre');
    const mensaje = document.getElementById('dni-mensaje');
    if (!dni || !boton || !nombre) return;
    let revision = 0;
    dni.addEventListener('input', function () {
        dni.value = dni.value.replace(/\D/g, '').slice(0, 8);
        revision++;
        mensaje.textContent = 'Pulsa Consultar DNI para completar tu nombre.';
    });
    nombre.addEventListener('input', function () { revision++; });
    nombre.addEventListener('sgp:tipo-solicitante', function () { revision++; });
    dni.addEventListener('keydown', function (e) {
        if (e.key === 'Enter') { e.preventDefault(); boton.click(); }
    });
    boton.addEventListener('click', async function () {
        if (!/^[0-9]{8}$/.test(dni.value)) {
            mensaje.textContent = 'Escribe un DNI de 8 dígitos.';
            dni.focus();
            return;
        }
        const actual = revision;
        const controller = new AbortController();
        const timeout = setTimeout(() => controller.abort(), 12000);
        boton.disabled = true;
        boton.textContent = 'Consultando…';
        mensaje.textContent = 'Consultando los datos del DNI…';
        try {
            const headers = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
            headers[boton.dataset.csrfHeader] = boton.dataset.csrf;
            const respuesta = await fetch(boton.dataset.url, {
                method: 'POST', headers: headers, credentials: 'same-origin',
                body: JSON.stringify({ dni: dni.value }), signal: controller.signal
            });
            if (actual !== revision) return;
            if (respuesta.status === 403) throw new Error('Tu sesión venció. Recarga la página para consultar o escribe tu nombre manualmente.');
            const datos = await respuesta.json();
            if (actual !== revision) return;
            if (!respuesta.ok) throw new Error(datos.mensaje || 'No se pudo consultar el DNI. Puedes escribir tu nombre manualmente.');
            if (typeof datos.nombre !== 'string' || !datos.nombre.trim()) throw new Error('No se recibió un nombre válido. Escríbelo manualmente.');
            if (datos.nombre.length > nombre.maxLength || !(new RegExp('^(?:' + nombre.pattern + ')$')).test(datos.nombre)) {
                throw new Error('El nombre recibido no cumple el formato del formulario. Escríbelo manualmente.');
            }
            nombre.value = datos.nombre;
            nombre.classList.remove('is-invalid');
            mensaje.textContent = 'Nombre completado. Revisa tus datos antes de enviar.';
        } catch (e) {
            if (actual === revision) mensaje.textContent = e instanceof SyntaxError || e.name === 'AbortError' || e instanceof TypeError
                ? 'No pudimos consultar el DNI. Puedes escribir tu nombre manualmente.' : e.message;
        } finally {
            clearTimeout(timeout);
            boton.disabled = false;
            boton.textContent = 'Consultar DNI';
        }
    });
})();
