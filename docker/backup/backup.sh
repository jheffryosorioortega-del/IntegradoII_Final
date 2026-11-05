#!/usr/bin/env bash
# Copias de seguridad de las 3 bases de SGP. Corre dentro del contenedor "backup" (docker-compose.prod.yml).
#
# - Una copia por noche, a la hora SGP_BACKUP_HORA (hora de Lima). Además, una al arrancar si no hay ninguna de las últimas 12 horas.
# - Cada copia es una carpeta /backups/sgp-AAAAMMDD-HHMMSS con un archivo .dump por base (formato "custom" de pg_dump:
#   comprimido y restaurable con pg_restore). Las fotos de evidencia viven en la base, así que van incluidas.
# - Se comprueba que cada archivo se pueda leer (pg_restore --list) ANTES de dar la copia por buena; si algo falla, la
#   copia a medias se descarta y el siguiente intento lo vuelve a probar. Una copia mala nunca reemplaza a una buena.
# - Se borran las copias de más de SGP_BACKUP_DIAS días.
#
# ¡Una copia en el mismo servidor NO protege si el servidor se pierde! Copia la carpeta backups/ a otro lugar
# (ver docs/DESPLIEGUE_GCP.md: se sube a un bucket de Cloud Storage).
set -uo pipefail

DESTINO=/backups
HORA="${SGP_BACKUP_HORA:-03:00}"
DIAS="${SGP_BACKUP_DIAS:-14}"
BASES=(sgp_usuarios sgp_clientes sgp_solicitudes)
export PGPASSWORD="${POSTGRES_PASSWORD:?Falta POSTGRES_PASSWORD}"

registro() { echo "[backup $(date '+%F %T')] $*"; }

hacer_copia() {
  local marca tmp final bd
  marca="$(date +%Y%m%d-%H%M%S)"
  tmp="$DESTINO/.tmp-$marca"
  final="$DESTINO/sgp-$marca"
  mkdir -p "$tmp" || { registro "ERROR: no puedo escribir en $DESTINO"; return 1; }

  for bd in "${BASES[@]}"; do
    if ! pg_dump -h postgres -U postgres -d "$bd" --format=custom --file "$tmp/$bd.dump"; then
      registro "ERROR: falló pg_dump de $bd; se descarta esta copia"
      rm -rf "$tmp"; return 1
    fi
    if ! pg_restore --list "$tmp/$bd.dump" > /dev/null; then
      registro "ERROR: la copia de $bd no se puede leer; se descarta"
      rm -rf "$tmp"; return 1
    fi
  done

  mv "$tmp" "$final"
  registro "OK: $final ($(du -sh "$final" | cut -f1))"

  # Retención: fuera las carpetas de copias más viejas que SGP_BACKUP_DIAS días (y restos de copias a medias).
  find "$DESTINO" -maxdepth 1 -name 'sgp-*' -mtime +"$DIAS" -exec rm -rf {} + 2>/dev/null
  find "$DESTINO" -maxdepth 1 -name '.tmp-*' -mmin +120 -exec rm -rf {} + 2>/dev/null
  return 0
}

segundos_hasta_la_hora() {
  local ahora objetivo
  ahora="$(date +%s)"
  objetivo="$(date -d "today $HORA" +%s)"
  [ "$objetivo" -le "$ahora" ] && objetivo="$(date -d "tomorrow $HORA" +%s)"
  echo $((objetivo - ahora))
}

# Copia a pedido (por ejemplo, justo antes de actualizar el sistema):
#     docker compose exec backup /bin/bash /backup.sh ahora
if [ "${1:-}" = "ahora" ]; then
  hacer_copia
  exit $?
fi

registro "Servicio de copias iniciado: cada día a las $HORA (hora de Lima), se conservan $DIAS días."

# Al arrancar: si no hay copia reciente, hacer una ya (también sirve para comprobar que todo funciona).
if ! find "$DESTINO" -maxdepth 1 -name 'sgp-*' -mmin -720 2>/dev/null | grep -q .; then
  registro "No hay copia de las últimas 12 horas: haciendo una ahora."
  hacer_copia || registro "La copia inicial falló; se reintentará a la hora programada."
fi

while true; do
  espera="$(segundos_hasta_la_hora)"
  registro "Próxima copia en $((espera / 3600)) h $(((espera % 3600) / 60)) min."
  sleep "$espera"
  hacer_copia || registro "La copia programada falló; se reintentará mañana (revisa los registros)."
done
