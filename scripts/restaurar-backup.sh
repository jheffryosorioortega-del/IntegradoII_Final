#!/usr/bin/env bash
# Restaura una copia de seguridad hecha por el servicio "backup". ¡REEMPLAZA los datos actuales de las bases restauradas!
#
# Uso (desde la carpeta del proyecto, en el servidor):
#     ./scripts/restaurar-backup.sh backups/sgp-20261105-030000            # las 3 bases
#     ./scripts/restaurar-backup.sh backups/sgp-20261105-030000 sgp_usuarios   # solo una
#
# Qué hace: apaga los servicios que usan las bases, restaura con pg_restore (--clean: borra y recrea las tablas
# desde la copia, con sus dueños y permisos) y vuelve a encender todo. Pide que escribas SI antes de tocar nada.
set -euo pipefail

CARPETA="${1:?Falta la carpeta de la copia. Ejemplo: ./scripts/restaurar-backup.sh backups/sgp-20261105-030000}"
SOLO="${2:-}"
cd "$(dirname "$0")/.."

# Mismos archivos de Compose que en producción, salvo que el .env o el entorno ya definan COMPOSE_FILE.
export COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.yml:docker-compose.prod.yml}"
export COMPOSE_PATH_SEPARATOR="${COMPOSE_PATH_SEPARATOR:-:}"

[ -d "$CARPETA" ] || { echo "No existe la carpeta $CARPETA"; exit 1; }
BASES=(sgp_usuarios sgp_clientes sgp_solicitudes)
[ -n "$SOLO" ] && BASES=("$SOLO")
for bd in "${BASES[@]}"; do
  [ -f "$CARPETA/$bd.dump" ] || { echo "Falta $CARPETA/$bd.dump"; exit 1; }
done

echo "Se van a restaurar: ${BASES[*]}"
echo "Desde:               $CARPETA"
echo "Esto REEMPLAZA los datos actuales de esas bases y apaga el sistema unos minutos."
read -r -p "Escribe SI para continuar: " respuesta
[ "$respuesta" = "SI" ] || { echo "Cancelado, no se tocó nada."; exit 1; }

echo ">> Apagando los servicios que usan las bases..."
docker compose stop ms-frontend gateway ms-solicitudes ms-clientes ms-usuarios

for bd in "${BASES[@]}"; do
  echo ">> Restaurando $bd ..."
  docker compose exec -T postgres pg_restore -U postgres -d "$bd" --clean --if-exists --exit-on-error < "$CARPETA/$bd.dump"
done

echo ">> Encendiendo todo de nuevo..."
docker compose up -d
echo "Listo. Revisa con: docker compose ps"
