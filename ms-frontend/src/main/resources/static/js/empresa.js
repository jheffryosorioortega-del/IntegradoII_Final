(function () {
    'use strict';
    const tipo = document.getElementById('tipoSolicitante');
    if (!tipo) return;
    const tabs = [...document.querySelectorAll('[data-tipo]')];
    const persona = document.getElementById('datos-persona');
    const empresa = document.getElementById('datos-empresa');
    const extras = [...document.querySelectorAll('[data-empresa-extra]')];
    const ruc = document.getElementById('ruc');
    const razon = document.getElementById('razonSocial');
    const fiscal = document.getElementById('direccionFiscal');
    const boton = document.getElementById('consultar-ruc');
    const mensaje = document.getElementById('ruc-mensaje');
    let revision = 0;
    let pendiente = null;
    function activar(value) {
        revision++;
        if (pendiente) pendiente.abort();
        const esEmpresa = value === 'EMPRESA';
        tipo.value = esEmpresa ? 'EMPRESA' : 'PERSONA';
        document.getElementById('nombre-grupo').className = esEmpresa ? 'col-md-7' : 'col-12';
        persona.hidden = esEmpresa;
        persona.querySelectorAll('input, button').forEach(el => { el.disabled = esEmpresa; });
        empresa.hidden = !esEmpresa;
        empresa.disabled = !esEmpresa;
        extras.forEach(el => { el.hidden = !esEmpresa; el.disabled = !esEmpresa; });
        document.getElementById('nombre-etiqueta').textContent = esEmpresa ? 'Persona de contacto' : 'Nombre completo';
        document.getElementById('correo').required = esEmpresa;
        const indicacion = document.getElementById('correo-indicacion');
        indicacion.textContent = esEmpresa ? '*' : '(opcional)';
        indicacion.className = esEmpresa ? 'text-danger' : 'text-body-secondary';
        tabs.forEach(tab => {
            const selected = tab.dataset.tipo === tipo.value;
            tab.classList.toggle('active', selected);
            tab.setAttribute('aria-selected', String(selected));
            tab.tabIndex = selected ? 0 : -1;
        });
        // Invalida también una consulta DNI en curso al cambiar de pestaña.
        document.getElementById('nombre').dispatchEvent(new Event('sgp:tipo-solicitante'));
        mensaje.textContent = 'Consulta tu RUC para completar la razón social y la dirección fiscal.';
        mensaje.classList.remove('text-danger');
    }
    tabs.forEach((tab, index) => {
        tab.addEventListener('click', () => activar(tab.dataset.tipo));
        tab.addEventListener('keydown', e => {
            if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(e.key)) return;
            e.preventDefault();
            const next = e.key === 'Home' ? 0 : e.key === 'End' ? tabs.length - 1 : (index + 1) % tabs.length;
            tabs[next].click(); tabs[next].focus();
        });
    });
    activar(tipo.value);
    [ruc, razon, fiscal].forEach(el => el.addEventListener('input', () => {
        revision++;
        if (pendiente) pendiente.abort();
        if (el === ruc) ruc.value = ruc.value.replace(/\D/g, '').slice(0, 11);
        mensaje.textContent = 'Puedes consultar el RUC o completar los datos manualmente.';
        mensaje.classList.remove('text-danger');
    }));
    ruc.addEventListener('keydown', e => {
        if (e.key === 'Enter') { e.preventDefault(); boton.click(); }
    });
    boton.addEventListener('click', async () => {
        if (tipo.value !== 'EMPRESA') return;
        if (!/^[0-9]{11}$/.test(ruc.value)) {
            mensaje.textContent = 'Escribe un RUC de 11 dígitos.';
            mensaje.classList.add('text-danger');
            ruc.focus(); return;
        }
        const actual = revision;
        const numero = ruc.value;
        const controller = new AbortController();
        pendiente = controller;
        const timeout = setTimeout(() => controller.abort(), 12000);
        boton.disabled = true;
        boton.textContent = 'Consultando…';
        mensaje.textContent = 'Consultando los datos de la empresa…';
        mensaje.classList.remove('text-danger');
        try {
            const headers = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
            headers[boton.dataset.csrfHeader] = boton.dataset.csrf;
            const res = await fetch(boton.dataset.url, {
                method: 'POST', headers, credentials: 'same-origin',
                body: JSON.stringify({ ruc: numero }), signal: controller.signal
            });
            if (actual !== revision) return;
            if (res.status === 403) throw new Error('Tu sesión venció. Recarga la página para consultar o completa los datos manualmente.');
            const data = await res.json();
            if (actual !== revision) return;
            if (!res.ok) throw new Error(data.mensaje || 'La consulta RUC no está disponible. Completa los datos manualmente.');
            if (data.ruc !== numero || typeof data.razonSocial !== 'string' || !data.razonSocial.trim()
                || data.razonSocial.length > 250 || /[<>]/.test(data.razonSocial)
                || (data.direccionFiscal != null && (typeof data.direccionFiscal !== 'string'
                    || data.direccionFiscal.length > 300 || /[<>]/.test(data.direccionFiscal)))) {
                throw new Error('No recibimos datos válidos. Puedes completar los datos manualmente.');
            }
            razon.value = data.razonSocial.trim();
            fiscal.value = data.direccionFiscal === '-' ? '' : (data.direccionFiscal || '').trim();
            razon.classList.remove('is-invalid');
            fiscal.classList.remove('is-invalid');
            mensaje.textContent = 'Datos completados. Revisa la razón social y la dirección fiscal.';
        } catch (e) {
            if (actual !== revision) return;
            mensaje.textContent = e.name === 'AbortError' || e instanceof TypeError || e instanceof SyntaxError
                ? 'No pudimos conectar con la consulta RUC. Puedes completar los datos manualmente.' : e.message;
            mensaje.classList.add('text-danger');
        } finally {
            clearTimeout(timeout);
            if (pendiente === controller) pendiente = null;
            boton.disabled = false;
            boton.textContent = 'Consultar RUC';
        }
    });
})();
