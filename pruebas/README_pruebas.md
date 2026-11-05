# Pruebas de sgp-microservicios

## Consulta DNI (sin consumir saldo ni consultar personas reales)

- `mvn -pl ms-clientes,ms-frontend test`: pruebas con respuestas simuladas de Decolecta y del Gateway.
- `node --test pruebas/dni_frontend.test.cjs`: validación, autocompletado, errores y respuestas tardías del formulario.
- Para verificar la integración real, iniciar Gateway, ms-clientes y ms-frontend con las variables
  del README configuradas, abrir `/contacto` e introducir un DNI propio o autorizado.
  Verificar que «Consultar DNI» completa el nombre, y que el formulario permite ingreso manual
  cuando el servicio no esté disponible. Esta consulta real puede consumir el saldo del proveedor.

## Batería original

- `ms_pruebas.py`: 51 pruebas reales (validación de nombre y teléfono contra la API y las pantallas, seguridad, login,
  concurrencia, recursos de Bootstrap, páginas de error). Resultado de la última ejecución: `resultados_ms.json` (51 aprobadas).
- `servicio_caido.py`: qué ve el usuario cuando `ms-clientes` está apagado (se apaga a mano antes de ejecutarlo).

## Cómo repetirlas
1. Levanta los 5 servicios (sin perfil, con H2) con bases NUEVAS: borra los archivos `data/sgp_*.mv.db` antes de empezar,
   porque algunas pruebas crean registros con teléfonos fijos y fallarían por duplicado en una segunda corrida.
2. En una terminal (Python 3 y `pip install requests`):

   ```
   set SGP_PW=<la misma contraseña que SGP_SEED_PASSWORD en tu .env>
   python pruebas/ms_pruebas.py
   ```

## Empresa / RUC
- Ejecutar: mvn -pl ms-clientes,ms-solicitudes,ms-frontend test
- Con el frontend de prueba iniciado en 18090: node pruebas/empresa_ui.cjs
  (requiere Playwright y Edge; SGP_TEST_URL permite elegir otra URL).
- El test de navegador simula respuestas RUC y comprueba ambos tipos de persona,
  errores de red, CSRF, autocompletado y diseño móvil. No consume el token.
- SGP_TEST_SCREENSHOTS permite indicar una carpeta de capturas opcional.
- La persistencia y conservación de solicitudes previas se verifican con H2 aislada.

## Clave compartida (SGP_API_KEY) y JWT (SGP_JWT_SECRET)
- `mvn test` incluye `ApiKeyFilterTest` (ms-clientes), `JwtServiceTest` (ms-usuarios) y `JwtFilterTest` (gateway):
  firma falsa, token vencido, token alterado, ataque `alg: none`, roles por ruta, denegación por defecto y borrado de
  las cabeceras `X-SGP-Usuario`/`X-SGP-Rol` enviadas desde afuera.
- `ms_pruebas.py` lee la variable `SGP_API_KEY` (valor del `.env`), inicia sesión como Administrador y Técnico por el Gateway
  y usa sus JWT. Agrega V-52 a V-60 (sin clave, clave mala, puertos internos directos, sin token, rol Técnico, token alterado,
  token sin clave, ruta pública). Ejecutar con `set SGP_API_KEY=<valor del .env>` y `set SGP_PW=...` antes de
  `python pruebas/ms_pruebas.py`.

## Flujo de estados, técnico asignado e historial
- `mvn -pl ms-solicitudes test`: `EstadoSolicitudTest` (reglas del flujo), `FlujoSolicitudTest` (servicio con H2 y las migraciones reales:
  transiciones, técnico obligatorio para programar, asignación validada contra ms-usuarios, historial en orden) y `MigracionHistorialTest`
  (V3 sobre datos previos). `JwtFilterTest` cubre las rutas nuevas del Gateway.
- La migración V3 de PostgreSQL se validó contra PostgreSQL 18 dentro de una transacción con `ROLLBACK`.
- Prueba de punta a punta hecha a mano (19 comprobaciones): portal público → panel como Coordinador y como Técnico → Gateway con JWT de Técnico,
  con ms-solicitudes sobre H2 en memoria y ms-clientes simulado, para no tocar las bases reales.

## Fotos de evidencia, cola de atención y alertas
- `mvn -pl ms-solicitudes test`: `FotosYOrdenTest` (tipo de imagen por contenido, nombre seguro, orden de atención) y `EvidenciaYColaTest`
  (servicio con H2 y las migraciones V1 a V4: límites de 4 fotos y 6 MB, archivo falso rechazado, ventana de 15 minutos, 404, una foto no se ve desde
  la solicitud de otro, cola sin cerradas, alertas). `JwtFilterTest` cubre las rutas nuevas del Gateway.
- La migración V4 de PostgreSQL se validó contra PostgreSQL 18 dentro de una transacción con `ROLLBACK`.
- Prueba de punta a punta (29 comprobaciones): formulario público con fotos, foto pesada y archivo falso, panel como Coordinador y como Técnico
  (lista, ficha, descarga de fotos, alertas, tablero), flujo con técnico e historial, y llamadas directas al Gateway. Se hizo con ms-solicitudes sobre
  H2 en memoria y ms-clientes simulado, para no tocar las bases reales.
