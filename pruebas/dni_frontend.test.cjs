const { test } = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
const path = require('node:path');
const source = fs.readFileSync(path.join(__dirname, '../ms-frontend/src/main/resources/static/js/dni.js'), 'utf8');

function setup(fetch) {
    const elements = {};
    for (const id of ['dni-consulta', 'consultar-dni', 'nombre', 'dni-mensaje']) {
        elements[id] = { value: '', handlers: {}, dataset: {}, classList: { remove() {} },
            addEventListener(event, handler) { this.handlers[event] = handler; }, focus() {} };
    }
    elements['consultar-dni'].dataset = { url: '/contacto/consultar-dni', csrf: 'csrf-test', csrfHeader: 'X-CSRF-TOKEN' };
    elements.nombre.maxLength = 60;
    elements.nombre.pattern = '[A-Z ]{2,60}';
    vm.runInNewContext(source, { document: { getElementById: id => elements[id] }, fetch, AbortController, setTimeout, clearTimeout });
    return elements;
}

test('valida antes de consultar y envía CSRF con el documento en el cuerpo', async () => {
    let calls = 0;
    const el = setup(async (url, options) => {
        calls++;
        assert.equal(url, '/contacto/consultar-dni');
        assert.equal(options.headers['X-CSRF-TOKEN'], 'csrf-test');
        assert.equal(JSON.parse(options.body).dni, '00000001');
        return { ok: true, status: 200, json: async () => ({ nombre: 'ANA PRUEBA' }) };
    });
    el['dni-consulta'].value = '123';
    await el['consultar-dni'].handlers.click();
    assert.equal(calls, 0);
    el['dni-consulta'].value = '00000001';
    await el['consultar-dni'].handlers.click();
    assert.equal(el.nombre.value, 'ANA PRUEBA');
    assert.equal(el['consultar-dni'].disabled, false);
});

test('una respuesta tardía no reemplaza un nombre editado por el usuario', async () => {
    let resolve;
    const el = setup(() => new Promise(r => { resolve = r; }));
    el['dni-consulta'].value = '00000001';
    const pending = el['consultar-dni'].handlers.click();
    el.nombre.value = 'NOMBRE MANUAL';
    el.nombre.handlers.input();
    resolve({ ok: true, status: 200, json: async () => ({ nombre: 'ANA PRUEBA' }) });
    await pending;
    assert.equal(el.nombre.value, 'NOMBRE MANUAL');
    assert.equal(el['consultar-dni'].disabled, false);
});

test('errores del servicio mantienen el ingreso manual disponible', async () => {
    const el = setup(async () => ({ ok: false, status: 503,
        json: async () => ({ mensaje: 'Puedes escribir tu nombre manualmente.' }) }));
    el['dni-consulta'].value = '00000001';
    el.nombre.value = 'NOMBRE MANUAL';
    await el['consultar-dni'].handlers.click();
    assert.equal(el.nombre.value, 'NOMBRE MANUAL');
    assert.match(el['dni-mensaje'].textContent, /manualmente/);
    assert.equal(el['consultar-dni'].disabled, false);
});
