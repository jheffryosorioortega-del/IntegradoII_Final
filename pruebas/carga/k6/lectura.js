// Prueba de carga SOLO LECTURA: no envia formularios ni crea datos. Sirve para medir el sitio publicado.
// Cada usuario virtual navega como un visitante: inicio, servicios y formulario de contacto.
import http from 'k6/http';
import { check, group, sleep } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8090';
const USUARIOS = Number(__ENV.USUARIOS || 100);

export const options = {
  scenarios: {
    visitantes: {
      executor: 'ramping-vus', startVUs: 0, gracefulRampDown: '10s',
      stages: [
        { duration: '30s', target: USUARIOS },   // sube
        { duration: '90s', target: USUARIOS },   // mantiene
        { duration: '15s', target: 0 },          // baja
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<2000'],
  },
};

export default function () {
  group('Inicio', () => {
    const r = http.get(`${BASE}/`, { tags: { pagina: 'inicio' } });
    check(r, { 'inicio 200': (x) => x.status === 200 });
  });
  sleep(1 + Math.random() * 2);
  group('Servicios', () => {
    const r = http.get(`${BASE}/servicios`, { tags: { pagina: 'servicios' } });
    check(r, { 'servicios 200': (x) => x.status === 200 });
  });
  sleep(1 + Math.random() * 2);
  group('Formulario', () => {
    const r = http.get(`${BASE}/contacto`, { tags: { pagina: 'contacto' } });
    check(r, { 'contacto 200': (x) => x.status === 200 });
  });
  sleep(2 + Math.random() * 3);
}
