# syntax=docker/dockerfile:1
#
# Dockerfile COMPARTIDO por los 5 servicios (ms-usuarios, ms-clientes, ms-solicitudes, gateway, ms-frontend).
# Cada servicio se construye con SU carpeta como contexto (ver docker-compose.yml), así que aquí "pom.xml" y "src"
# son los del servicio. Dos etapas: Maven compila el .jar y una imagen mínima de Java lo ejecuta.
#
# No se copia ningún .env: las claves entran en tiempo de ejecución como variables de entorno, nunca dentro de la imagen.

ARG JAVA_VERSION=21

# ---------- Etapa 1: compilar ----------
FROM maven:3.9-eclipse-temurin-${JAVA_VERSION} AS build
WORKDIR /build

# Primero solo el pom: mientras no cambien las dependencias, esta capa (y la caché de Maven) se reutiliza
# y recompilar tras editar código tarda segundos en vez de minutos.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

COPY src src
# Las pruebas se corren aparte (mvn test); aquí solo se arma el .jar.
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package \
    && cp target/*.jar /build/app.jar

# ---------- Etapa 2: ejecutar ----------
FROM eclipse-temurin:${JAVA_VERSION}-jre AS runtime
ARG PORT
# No se ejecuta como root: si alguien lograra ejecutar código dentro del contenedor, no tendría permisos de administrador.
RUN useradd --system --uid 10001 --no-create-home --shell /usr/sbin/nologin sgp
WORKDIR /app
COPY --from=build /build/app.jar app.jar
USER sgp
EXPOSE ${PORT}
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
