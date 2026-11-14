// Prueba de carga SGP Conecta: clientes reales registrando solicitudes en /contacto.
// Cada usuario virtual (VU) hace lo que haria una persona: abre la pagina, lee el formulario,
// lo llena (pausa) y lo envia. Se mide el tiempo de cada paso y si la solicitud quedo registrada.
//
// Perfiles (variable PERFIL):  humo = 2 usuarios 20 s | carga = 100 usuarios | carga300 = 300 usuarios | estres = hasta 250
import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

const BASE = __ENV.BASE_URL || 'http://localhost:8090';
const PERFIL = __ENV.PERFIL || 'carga';
const RUN = Math.floor(Math.random() * 100);            // distingue una corrida de otra (telefonos unicos)

const perfiles = {
  humo: { executor: 'constant-vus', vus: 2, duration: '20s' },
  carga: {
    executor: 'ramping-vus', startVUs: 0, gracefulRampDown: '15s',
    stages: [
      { duration: '1m', target: 100 },   // sube de 0 a 100 clientes
      { duration: '3m', target: 100 },   // mantiene 100 clientes a la vez
      { duration: '30s', target: 0 },    // baja
    ],
  },
  carga300: {
    executor: 'ramping-vus', startVUs: 0, gracefulRampDown: '15s',
    stages: [
      { duration: '2m', target: 300 },   // sube de 0 a 300 clientes
      { duration: '3m', target: 300 },   // mantiene 300 clientes a la vez
      { duration: '30s', target: 0 },    // baja
    ],
  },
  estres: {
    executor: 'ramping-vus', startVUs: 0, gracefulRampDown: '15s',
    stages: [
      { duration: '1m', target: 100 }, { duration: '2m', target: 150 },
      { duration: '2m', target: 200 }, { duration: '2m', target: 250 }, { duration: '1m', target: 0 },
    ],
  },
};

export const options = {
  scenarios: { clientes: perfiles[PERFIL] },
  thresholds: {
    // Criterios de aprobado: si no se cumplen, k6 termina con error (util para el informe).
    http_req_failed: ['rate<0.01'],                    // menos de 1 % de errores HTTP
    solicitud_registrada: ['rate>0.99'],               // mas del 99 % de solicitudes quedan guardadas
    'http_req_duration{paso:enviar}': ['p(95)<2000'],  // 95 % de los envios en menos de 2 s
    'http_req_duration{paso:formulario}': ['p(95)<1000'],
  },
};

const solicitudRegistrada = new Rate('solicitud_registrada');
const tiempoEnvio = new Trend('tiempo_envio_solicitud', true);
const registradas = new Counter('solicitudes_creadas');
const bloqueados = new Counter('envios_bloqueados_por_limite');   // el antiabuso por IP rechazo el envio

const categorias = ['GASFITERIA', 'CISTERNAS', 'BOMBAS', 'AGUA_CALIENTE', 'ELECTRICIDAD', 'MANTENIMIENTO'];
const urgencias = ['BAJA', 'MEDIA', 'ALTA'];
const pad = (n, d) => String(n).padStart(d, '0');

export default function () {
  // 1) Entra al sitio y abre el formulario
  group('1. Pagina de inicio', () => {
    const r = http.get(`${BASE}/`, { tags: { paso: 'inicio' } });
    check(r, { 'inicio 200': (x) => x.status === 200 });
  });
  sleep(1 + Math.random() * 2);

  let csrf = null;
  group('2. Abrir formulario', () => {
    const r = http.get(`${BASE}/contacto`, { tags: { paso: 'formulario' } });
    check(r, { 'formulario 200': (x) => x.status === 200 });
    const campo = r.html().find('input[name=_csrf]');
    csrf = campo.size() ? campo.first().attr('value') : null;
    check(csrf, { 'token CSRF encontrado': (t) => !!t });
  });
  if (!csrf) { solicitudRegistrada.add(false); return; }

  sleep(5 + Math.random() * 10);                        // la persona escribe sus datos (5 a 15 s)

  // 2) Envia la solicitud. Telefono unico: 9 + corrida(2) + VU(3) + iteracion(3) = 9 digitos
  group('3. Enviar solicitud', () => {
    const telefono = `9${pad(RUN, 2)}${pad(__VU % 1000, 3)}${pad(__ITER % 1000, 3)}`;
    const cuerpo = {
      _csrf: csrf,
      tipoSolicitante: 'PERSONA',
      nombre: 'Cliente Carga',
      telefono,
      correo: '',
      direccion: 'Av Los Pinos 123',
      distrito: 'Miraflores',
      categoria: categorias[Math.floor(Math.random() * categorias.length)],
      urgencia: urgencias[Math.floor(Math.random() * urgencias.length)],
      descripcion: 'Prueba de carga automatica: revision de instalacion en el domicilio',
    };
    const r = http.post(`${BASE}/contacto`, cuerpo, { tags: { paso: 'enviar' } });   // sigue la redireccion a /contacto/enviado
    const ok = check(r, {
      'envio 200': (x) => x.status === 200,
      'muestra codigo SOL-': (x) => typeof x.body === 'string' && x.body.includes('SOL-'),
    });
    if (typeof r.url === 'string' && r.url.indexOf('limite') >= 0) bloqueados.add(1);
    solicitudRegistrada.add(ok);
    tiempoEnvio.add(r.timings.duration);
    if (ok) registradas.add(1);
  });

  sleep(2 + Math.random() * 3);
}
