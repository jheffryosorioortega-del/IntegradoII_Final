#!/usr/bin/env bash
# Crea los 3 usuarios y las 3 bases de SGP la PRIMERA vez que arranca PostgreSQL con un volumen vacío.
# Hace lo mismo que docs/sql/postgres_crear_bases.sql, pero leyendo las claves de las variables de entorno
# (las del .env), así no hay que ejecutar nada a mano.
#
# Si cambias una clave del .env después, esto NO se vuelve a ejecutar (los datos ya existen): hay que cambiarla
# en PostgreSQL con ALTER ROLE o reiniciar de cero con "docker compose down -v" (borra los datos).
set -euo pipefail

: "${USUARIOS_DB_PASSWORD:?Falta USUARIOS_DB_PASSWORD}"
: "${CLIENTES_DB_PASSWORD:?Falta CLIENTES_DB_PASSWORD}"
: "${SOLICITUDES_DB_PASSWORD:?Falta SOLICITUDES_DB_PASSWORD}"

# Las claves entran como variables de psql (:'nombre' las escapa y las entrecomilla): no se pegan en el texto del SQL.
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
     -v pw_usuarios="$USUARIOS_DB_PASSWORD" \
     -v pw_clientes="$CLIENTES_DB_PASSWORD" \
     -v pw_solicitudes="$SOLICITUDES_DB_PASSWORD" <<'EOSQL'

CREATE ROLE sgp_usuarios_app    LOGIN PASSWORD :'pw_usuarios';
CREATE ROLE sgp_clientes_app    LOGIN PASSWORD :'pw_clientes';
CREATE ROLE sgp_solicitudes_app LOGIN PASSWORD :'pw_solicitudes';

-- Cada base pertenece al usuario de su servicio: Flyway crea las tablas dentro de ella al arrancar.
CREATE DATABASE sgp_usuarios    OWNER sgp_usuarios_app    ENCODING 'UTF8' TEMPLATE template0;
CREATE DATABASE sgp_clientes    OWNER sgp_clientes_app    ENCODING 'UTF8' TEMPLATE template0;
CREATE DATABASE sgp_solicitudes OWNER sgp_solicitudes_app ENCODING 'UTF8' TEMPLATE template0;

-- Nadie más (rol PUBLIC) puede conectarse a estas bases: cada servicio solo ve la suya.
REVOKE ALL ON DATABASE sgp_usuarios    FROM PUBLIC;
REVOKE ALL ON DATABASE sgp_clientes    FROM PUBLIC;
REVOKE ALL ON DATABASE sgp_solicitudes FROM PUBLIC;
GRANT CONNECT ON DATABASE sgp_usuarios    TO sgp_usuarios_app;
GRANT CONNECT ON DATABASE sgp_clientes    TO sgp_clientes_app;
GRANT CONNECT ON DATABASE sgp_solicitudes TO sgp_solicitudes_app;
EOSQL

echo "SGP: bases sgp_usuarios, sgp_clientes y sgp_solicitudes creadas."
