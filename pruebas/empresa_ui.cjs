const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
(async () => {
    const browser = await chromium.launch({ channel: 'msedge', headless: true });
    try {
        const page = await browser.newPage({ viewport: { width: 1440, height: 1100 } });
        const errors = [];
        page.on('pageerror', err => errors.push(err.message));
        await page.goto((process.env.SGP_TEST_URL || 'http://127.0.0.1:18090') + '/contacto');
        await page.waitForSelector('#tab-empresa');
        assert.equal(await page.locator('#datos-empresa').isVisible(), false);
        assert.equal(await page.locator('#correo').getAttribute('required'), null);
        await page.locator('#tab-empresa').click();
        assert.equal(await page.locator('#datos-empresa').isVisible(), true);
        assert.equal(await page.locator('#datos-persona').isVisible(), false);
        assert.equal(await page.locator('#correo').evaluate(el => el.required), true);
        assert.equal(await page.locator('#nombre-etiqueta').textContent(), 'Persona de contacto');
        let requests = 0;
        await page.route('**/contacto/consultar-ruc', async route => {
            requests++;
            assert.ok(route.request().headers()['x-csrf-token']);
            await route.fulfill({ contentType: 'application/json',
                body: JSON.stringify({ ruc: '20000000001', razonSocial: 'EMPRESA 123 & HIJOS S.A.C.', direccionFiscal: 'Av. Fiscal 456' }) });
        });
        await page.locator('#ruc').fill('123');
        await page.locator('#consultar-ruc').click();
        assert.match(await page.locator('#ruc-mensaje').textContent(), /11 dígitos/);
        assert.equal(requests, 0);
        await page.locator('#direccion').fill('Av. Servicio 789');
        await page.locator('#ruc').fill('20000000001');
        await page.locator('#consultar-ruc').click();
        await page.waitForFunction(() => document.getElementById('razonSocial').value === 'EMPRESA 123 & HIJOS S.A.C.');
        assert.equal(await page.locator('#direccionFiscal').inputValue(), 'Av. Fiscal 456');
        assert.equal(await page.locator('#direccion').inputValue(), 'Av. Servicio 789');
        await page.locator('#nombre').fill('Ana Prueba');
        await page.locator('#cargo').fill('Jefa de mantenimiento');
        await page.locator('#requiereFactura').check();
        const out = process.env.SGP_TEST_SCREENSHOTS;
        if (out) {
            fs.mkdirSync(out, { recursive: true });
            await page.locator('form.needs-validation').screenshot({ style: '.sgp-navbar { visibility: hidden !important; }', path: path.join(out, 'empresa-desktop.png') });
        }
        await page.unroute('**/contacto/consultar-ruc');
        await page.route('**/contacto/consultar-ruc', route => route.abort('failed'));
        await page.locator('#consultar-ruc').click();
        await page.waitForFunction(() => document.getElementById('ruc-mensaje').textContent.includes('manualmente'));
        assert.equal(await page.locator('#razonSocial').inputValue(), 'EMPRESA 123 & HIJOS S.A.C.');
        assert.equal(await page.locator('#consultar-ruc').isEnabled(), true);
        await page.locator('#tab-persona').click();
        assert.equal(await page.locator('#datos-empresa').isVisible(), false);
        assert.equal(await page.locator('#ruc').isDisabled(), true);
        assert.equal(await page.locator('#correo').evaluate(el => el.required), false);
        assert.equal(await page.locator('#nombre-etiqueta').textContent(), 'Nombre completo');
        await page.locator('#tab-persona').press('ArrowRight');
        assert.equal(await page.locator('#tab-empresa').getAttribute('aria-selected'), 'true');
        await page.setViewportSize({ width: 390, height: 844 });
        assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth), true);
        if (out) await page.locator('form.needs-validation').screenshot({ style: '.sgp-navbar { visibility: hidden !important; }', path: path.join(out, 'empresa-mobile.png') });
        assert.deepEqual(errors, []);
        console.log('OK: pestañas, teclado, campos condicionales, validación RUC, CSRF, autocompletado, dirección independiente, error de red e interfaz móvil.');
    } finally { await browser.close(); }
})().catch(err => { console.error(err); process.exitCode = 1; });
