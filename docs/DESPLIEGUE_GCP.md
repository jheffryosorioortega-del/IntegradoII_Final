# Publicar SGP en Google Cloud (una máquina virtual + Docker Compose + HTTPS)

Esta guía deja SGP en internet, con HTTPS automático y copias de seguridad diarias. Usa **una sola máquina virtual (Compute Engine)**:
la misma configuración de Docker que ya probaste en tu PC, más `docker-compose.prod.yml` (Caddy para el HTTPS y el servicio de copias).

```
Internet ──443──▶ Caddy (HTTPS) ──▶ ms-frontend ──▶ gateway ──▶ ms-usuarios / ms-clientes / ms-solicitudes ──▶ PostgreSQL
                                                                                                  backup ──▶ ./backups ──▶ bucket
```
Solo Caddy (puertos 80 y 443) está abierto a internet. La base de datos, el Gateway y los microservicios no se publican.

## Qué vas a necesitar
- Una cuenta de Google Cloud con facturación activada (pide tarjeta; las cuentas nuevas suelen tener un crédito de prueba: verifica la oferta vigente).
- Un **dominio o subdominio**. Gratis: [duckdns.org](https://www.duckdns.org) (por ejemplo `sgp-polanco.duckdns.org`). Caddy no puede sacar un certificado para una IP sola.
- Unos 45 minutos la primera vez.

## Costo aproximado (por mes, revisa los precios vigentes)
| Concepto | Aproximado |
|---|---|
| VM `e2-medium` (2 vCPU, 4 GB) 24/7 | 25–30 USD |
| VM `e2-standard-2` (2 vCPU, 8 GB), más cómoda | 50–55 USD |
| Disco de 30 GB | 2–3 USD |
| IP externa fija | 3–4 USD |
| Bucket de copias (unos GB) | menos de 1 USD |

Si solo es para una exposición, **apaga la VM** cuando no la uses (Compute Engine → Detener): dejas de pagar el cómputo; el disco y la IP siguen cobrando poco.

---

## Paso 1. Proyecto y alerta de presupuesto
1. En [console.cloud.google.com](https://console.cloud.google.com) crea un proyecto (por ejemplo `sgp-polanco`) y vincula tu cuenta de facturación.
2. **Facturación → Presupuestos y alertas → Crear presupuesto**: pon un monto bajo (por ejemplo 20 USD) con alertas al 50 %, 90 % y 100 %. Hazlo ya: es lo que evita sustos.

## Paso 2. Crear la máquina virtual
**Compute Engine → Instancias de VM → Crear instancia**:
- **Región:** `southamerica-west1` (Santiago) o `southamerica-east1` (São Paulo): las más cercanas a Lima.
- **Tipo:** `e2-medium` como mínimo (`e2-standard-2` si quieres holgura).
- **Disco de arranque:** Ubuntu **24.04 LTS**, 30 GB, "Balanced persistent disk".
- **Firewall:** marca **Permitir tráfico HTTP** y **Permitir tráfico HTTPS** (abre los puertos 80 y 443; Caddy necesita ambos).
- **Identidad y acceso a la API → Establecer acceso para cada API → Cloud Storage: Lectura y escritura.** (Hace falta para subir las copias al bucket del paso 8; solo se puede cambiar con la VM apagada.)
- Crear.

## Paso 3. IP fija y dominio
1. **Red de VPC → Direcciones IP → Reservar dirección IP externa estática**, y asígnala a tu VM (o, en la VM, "Interfaz de red → IP externa → Reservar"). Sin esto la IP cambia al reiniciar y el dominio deja de apuntar.
2. En DuckDNS: crea el subdominio y escribe la IP de la VM. Comprueba desde tu PC: `nslookup sgp-polanco.duckdns.org` debe devolver esa IP.
   **Hazlo antes de arrancar el sistema:** si Caddy pide el certificado con el dominio mal apuntado, Let's Encrypt rechaza y limita los reintentos por un tiempo.

## Paso 4. Instalar Docker en la VM
En la lista de VM, botón **SSH** (abre una terminal en el navegador):
```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
exit          # cierra la terminal y vuelve a abrirla con SSH para que el grupo surta efecto
docker compose version     # debe ser 2.24 o más nuevo
```
**Si elegiste 4 GB de RAM**, agrega memoria de apoyo (swap) y limita las compilaciones simultáneas: compilar 5 servicios Java a la vez puede quedarse sin memoria.
```bash
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

## Paso 5. Subir el proyecto
Lo más cómodo: un repositorio **privado** en GitHub (el `.gitignore` ya deja fuera `.env`, `backups/` y los compilados).
```bash
git clone https://github.com/TU_USUARIO/TU_REPO.git sgp-microservicios
cd sgp-microservicios
```
Si no usas Git: en la terminal del navegador, el menú ⚙ → **Subir archivo** acepta un `.zip` del proyecto (sin `.env`, `target/`, `.idea/` ni `data/`); luego `unzip`.
**No copies el `.env` de tu PC al servidor**: las claves de producción deben ser nuevas (paso 6).

## Paso 6. Crear el `.env` del servidor
```bash
cp .env.example .env
nano .env
```
Rellena así (en `nano`: `Ctrl+O` guarda, `Ctrl+X` sale):

1. **Claves aleatorias nuevas.** Genéralas con este comando y pega cada línea en el `.env`:
   ```bash
   for v in USUARIOS_DB_PASSWORD CLIENTES_DB_PASSWORD SOLICITUDES_DB_PASSWORD POSTGRES_PASSWORD SGP_API_KEY SGP_JWT_SECRET SGP_DNI_INTERNAL_KEY; do echo "$v=$(openssl rand -hex 24)"; done
   ```
2. `SGP_DB_HOST` y `SGP_DB_PORT` no hace falta tocarlos: dentro de Docker, el compose los fija solo (`postgres:5432`).
3. `SGP_SEED_PASSWORD=` una contraseña **larga y única** (12+ caracteres, sin `$`, `#`, comillas ni espacios). Es la contraseña de `admin@sgp.local`, `coordinador@sgp.local` y `tecnico@sgp.local`, y se aplica solo en el primer arranque, con la base vacía. **No reutilices la de pruebas ni una que hayas escrito en algún chat.**
4. `DECOLECTA_TOKEN=` tu token (cópialo de tu `.env` local, a mano; sin él, el DNI/RUC se escribe a mano).
5. Las líneas de producción (quítales el `#`):
   ```
   SGP_DOMINIO=sgp-polanco.duckdns.org
   COMPOSE_FILE=docker-compose.yml:docker-compose.prod.yml
   ```
6. Protege el archivo: `chmod 600 .env`.

## Paso 7. Arrancar
```bash
export COMPOSE_PARALLEL_LIMIT=2      # compila de a 2 servicios (recomendado con 4 GB)
docker compose up -d --build         # la primera vez tarda 10–15 minutos
docker compose ps                    # todos "healthy" (caddy no tiene chequeo: debe decir "Up")
docker compose logs caddy | grep -i -E "certificate|error"    # confirma que obtuvo el certificado
```
Abre `https://TU_DOMINIO` desde tu celular con datos móviles (otra red): debe cargar con candado.
Panel: `https://TU_DOMINIO/panel` · usuarios: los 3 de arriba con tu `SGP_SEED_PASSWORD`.

## Paso 8. Copias de seguridad
**Ya funcionan solas:** el servicio `backup` guarda cada noche a las 3:00 (hora de Lima) las 3 bases en `./backups/sgp-AAAAMMDD-HHMMSS/` y conserva 14 días. Las fotos de evidencia viven en la base, así que van incluidas.
```bash
docker compose logs backup                                  # historial de copias
docker compose exec backup /bin/bash /backup.sh ahora       # hacer una copia YA (antes de cada actualización)
ls -lh backups/
```
`docker compose ps` marca `backup` como *unhealthy* si pasan más de 26 horas sin copia nueva.

**Una copia en el mismo servidor no te salva si el servidor se pierde.** Súbelas a un bucket:
1. **Cloud Storage → Crear bucket**: nombre único (`sgp-backups-TU_PROYECTO`), misma región, "Prevención de acceso público: activada" (privado). Opcional: regla de ciclo de vida "eliminar objetos con más de 60 días".
2. En la VM: `gcloud --version` (normalmente ya viene instalado), prueba `./scripts/subir-backups-gcs.sh sgp-backups-TU_PROYECTO`.
3. Hazlo automático cada día (4:30, después de la copia de las 3:00): `crontab -e` y agrega
   ```
   30 4 * * * /home/TU_USUARIO/sgp-microservicios/scripts/subir-backups-gcs.sh sgp-backups-TU_PROYECTO >> /home/TU_USUARIO/subida-backups.log 2>&1
   ```
   (usa `pwd` dentro del proyecto para ver la ruta exacta).

**Restaurar** (¡reemplaza los datos actuales!):
```bash
ls backups/                                           # elige la carpeta de la copia
./scripts/restaurar-backup.sh backups/sgp-20261105-030000          # las 3 bases; pide escribir SI
./scripts/restaurar-backup.sh backups/sgp-20261105-030000 sgp_usuarios   # solo una base
```
Si perdiste el servidor entero: crea otra VM (pasos 2 a 7) y, antes de usarla, restaura desde el bucket
(`gcloud storage cp -r gs://TU_BUCKET/sgp-backups/sgp-… backups/`).
**Prueba una restauración en tu PC al menos una vez**: una copia que nunca se probó no es una garantía.

## Paso 9. Actualizar el sistema cuando cambies código
```bash
cd ~/sgp-microservicios
docker compose exec backup /bin/bash /backup.sh ahora    # copia de seguridad antes de tocar nada
git pull
docker compose up -d --build                             # reconstruye solo lo que cambió; Flyway aplica las migraciones nuevas
docker compose ps
```

## Lista de comprobación antes de compartir el enlace
- [ ] `https://TU_DOMINIO` abre con candado y `http://` redirige a `https://`.
- [ ] `SGP_SEED_PASSWORD` es larga y única; el `.env` del servidor tiene permisos `600` y claves nuevas.
- [ ] Desde fuera: `https://TU_DOMINIO:8090`, `:8080` y `:5432` **no** responden (solo 80 y 443 abiertos).
- [ ] Enviaste una solicitud de prueba con fotos desde el celular y la viste en el panel.
- [ ] Hay una copia en `backups/` y ya probaste subirla al bucket.
- [ ] Alerta de presupuesto creada.

## Problemas comunes
| Síntoma | Causa y solución |
|---|---|
| El navegador dice "no es seguro" / Caddy no obtiene certificado | El dominio no apunta a la IP de la VM, o los puertos 80/443 no están abiertos. Revisa `nslookup`, el firewall del paso 2 y `docker compose logs caddy`. |
| `docker compose up` se detiene diciendo "Falta XXX" | Falta esa variable en el `.env` del servidor. |
| La compilación se corta con "Killed" o sin memoria | Agrega el swap del paso 4 y usa `COMPOSE_PARALLEL_LIMIT=2`, o una VM de 8 GB. |
| Todos los visitantes quedan bloqueados a la vez por "demasiados intentos" | Los límites cuentan por IP: confirma que el `.env` tiene `COMPOSE_FILE` con `docker-compose.prod.yml` (así Caddy pasa la IP real). Sin él, todos comparten la IP del proxy. |
| Quieres mirar la base de datos | No se publica a propósito. Entra con `docker compose exec postgres psql -U postgres -d sgp_solicitudes`. |
| El volumen de datos se llenó | Las fotos de evidencia crecen la base: revisa `docker system df` y el disco con `df -h`; amplía el disco en la consola si hace falta. |

## Lo que esta configuración NO cubre (para ser honestos)
- **Una sola máquina:** si se cae, el sistema está caído hasta que la reinicies o restaures. Para una exposición y un negocio pequeño es razonable; alta disponibilidad requeriría más piezas.
- **Sin monitoreo ni alertas** (Prometheus/Grafana o UptimeRobot): te enteras de una caída cuando alguien avisa.
- **Sin CAPTCHA** en el formulario público; solo los límites por IP.
- **El panel es público** en la misma dirección; está protegido por login, roles y el freno de intentos. Para restringirlo aún más se puede limitar `/panel` por IP en el `Caddyfile`.
- **Tokens sin revocación** y microservicios internos que confían en el Gateway para los roles (ver README, «Seguridad aplicada»).
