-- SGP Conecta - PostgreSQL: crea los 3 usuarios del sistema y sus 3 bases de datos.
-- Un solo motor (PostgreSQL), una base por microservicio:
--   ms-usuarios   -> sgp_usuarios
--   ms-clientes   -> sgp_clientes
--   ms-solicitudes-> sgp_solicitudes
-- Ejecutar conectado como "postgres" (administrador). Las claves deben ser las mismas del archivo .env.
-- En pgAdmin: ejecuta cada bloque por separado (CREATE DATABASE no puede ir dentro de una transaccion),
-- o usa psql:  psql -U postgres -p 5432 -f postgres_crear_bases.sql

CREATE ROLE sgp_usuarios_app    LOGIN PASSWORD 'CAMBIAR_CLAVE_USUARIOS';
CREATE ROLE sgp_clientes_app    LOGIN PASSWORD 'CAMBIAR_CLAVE_CLIENTES';
CREATE ROLE sgp_solicitudes_app LOGIN PASSWORD 'CAMBIAR_CLAVE_SOLICITUDES';

-- Cada base pertenece al usuario de su servicio: Flyway crea las tablas dentro de ella al arrancar.
CREATE DATABASE sgp_usuarios    OWNER sgp_usuarios_app    ENCODING 'UTF8' TEMPLATE template0;
CREATE DATABASE sgp_clientes    OWNER sgp_clientes_app    ENCODING 'UTF8' TEMPLATE template0;
CREATE DATABASE sgp_solicitudes OWNER sgp_solicitudes_app ENCODING 'UTF8' TEMPLATE template0;

-- Nadie mas (rol PUBLIC) puede conectarse a estas bases.
REVOKE ALL ON DATABASE sgp_usuarios    FROM PUBLIC;
REVOKE ALL ON DATABASE sgp_clientes    FROM PUBLIC;
REVOKE ALL ON DATABASE sgp_solicitudes FROM PUBLIC;
GRANT CONNECT ON DATABASE sgp_usuarios    TO sgp_usuarios_app;
GRANT CONNECT ON DATABASE sgp_clientes    TO sgp_clientes_app;
GRANT CONNECT ON DATABASE sgp_solicitudes TO sgp_solicitudes_app;