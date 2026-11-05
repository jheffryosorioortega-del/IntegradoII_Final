#!/usr/bin/env bash
# Copia la carpeta backups/ a un bucket de Google Cloud Storage, para que las copias NO dependan de este servidor.
#
# Uso:  ./scripts/subir-backups-gcs.sh nombre-de-tu-bucket
# Para que corra solo cada día (4:30 a. m., después de la copia de las 3:00), agrégalo al cron del servidor:
#     crontab -e
#     30 4 * * * /home/TU_USUARIO/sgp-microservicios/scripts/subir-backups-gcs.sh nombre-de-tu-bucket >> /home/TU_USUARIO/subida-backups.log 2>&1
#
# Requisitos (ver docs/DESPLIEGUE_GCP.md): gcloud instalado en el servidor y la VM creada con permiso de ESCRITURA en
# Cloud Storage. El bucket debe ser privado.
set -euo pipefail

BUCKET="${1:?Falta el nombre del bucket. Ejemplo: ./scripts/subir-backups-gcs.sh sgp-backups-mi-proyecto}"
cd "$(dirname "$0")/.."

[ -d backups ] || { echo "No existe la carpeta backups/ todavía."; exit 1; }

# rsync sube lo nuevo. Sin --delete-unmatched-destination-objects: aunque aquí se borren copias viejas, en el bucket se conservan
# (ahí defines una regla de ciclo de vida para borrarlas, por ejemplo a los 60 días).
gcloud storage rsync --recursive backups "gs://${BUCKET}/sgp-backups"
echo "[$(date '+%F %T')] Copias subidas a gs://${BUCKET}/sgp-backups"
