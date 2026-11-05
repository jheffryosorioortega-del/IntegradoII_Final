# Registro de cambios: tipografía del sistema web (ms-frontend)

Proyecto: Integrador II, SGP Conecta (`sgp-microservicios`).
Alcance: cambio de la tipografía del portal público y del panel interno.
Cada cambio se anota aquí con archivo, antes/después y motivo.

---

## Cambio 1: fuente base Inter (2026-10-20)

**Motivo:** reemplazar la pila de fuentes del sistema (Segoe UI) por Inter, más legible en tablas, formularios y paneles.

### 1.1 `ms-frontend/src/main/resources/templates/fragments/layout.html`
Se agregaron, dentro del `<head th:fragment="head(titulo)">` y antes del CSS de Bootstrap, tres líneas para cargar Inter desde Google Fonts.

Antes:
```html
<link rel="stylesheet" th:href="@{/webjars/bootstrap/5.3.3/css/bootstrap.min.css}">
```

Después:
```html
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap">
<link rel="stylesheet" th:href="@{/webjars/bootstrap/5.3.3/css/bootstrap.min.css}">
```
Pesos cargados: 400, 500, 600, 700 y 800, los mismos que usa `site.css`.

### 1.2 `ms-frontend/src/main/resources/static/css/site.css` (línea 22)
Antes:
```css
--bs-body-font-family: "Segoe UI", system-ui, -apple-system, Roboto, "Helvetica Neue", Arial, sans-serif;
```

Después:
```css
--bs-body-font-family: "Inter", "Segoe UI", system-ui, -apple-system, Roboto, "Helvetica Neue", Arial, sans-serif;
```
Las fuentes anteriores quedan como respaldo si Inter no carga.

**Verificación:** en el navegador, `body` y `h1` usan Inter y `document.fonts.check('16px Inter')` devuelve `true`.

**Nota:** al cargar desde Google Fonts, hace falta internet al correr el sistema.

**Pendiente de revisar:** `login.html` y `error.html`, por si tienen su propio `<head>` y no usan el fragmento común.

---

## Ajuste local (no es parte del código del equipo)

**Archivo:** `sgp-microservicios/.env` (ignorado por git, no se sube al repositorio).
**Problema:** `SGP_BD_CARPETA` apuntaba a la carpeta de otro integrante (`C:/Users/jheff/OneDrive/...`), que no existe en esta PC, y los servicios de base de datos no arrancaban.
**Cambio:** ahora apunta a `C:/Users/USER/Desktop/INTEGRADOR_2/sgp-microservicios/data`.
**Respaldo:** `.env.bak-jheff` (contiene el valor original).
Cada integrante debe ajustar esta variable a su propia ruta.

---

## Próximos cambios
(se agregan aquí a medida que se hagan)
