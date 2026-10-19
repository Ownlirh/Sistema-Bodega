# ============================================================================
# Imagen de la aplicacion.
#
# No hace falta tener Java ni Maven instalados en la PC: todo se compila
# dentro del contenedor.
#
#   Etapa "dev"      -> modo desarrollo, recarga al guardar (docker compose --profile dev)
#   Etapa "runtime"  -> JAR compilado, es la que usa "docker compose up"
# ============================================================================

# ---------------------------------------------------------------------------
# Etapa 1: dependencias y compilacion
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# El pom se copia solo primero: asi Docker cachea la descarga de dependencias
# y no vuelve a bajar todo cada vez que alguien toca una clase.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------------------------------------------------------------------------
# Etapa 2: desarrollo (recarga automatica con spring-boot-devtools)
#
# El codigo fuente se monta desde la PC (ver docker-compose.yml), asi que lo
# que edites en tu editor se recompila dentro del contenedor.
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS dev
WORKDIR /app
EXPOSE 8080
CMD ["mvn", "-B", "spring-boot:run"]

# ---------------------------------------------------------------------------
# Etapa 3: ejecucion (imagen final, solo el JRE y el JAR)
# ---------------------------------------------------------------------------
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app
COPY --from=build /build/target/bodega-app-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
