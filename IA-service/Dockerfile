# Etapa 1: Build
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copiar solo pom.xml primero (cache de dependencias)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar código fuente y compilar
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Crear usuario no-root
RUN addgroup -S spring && adduser -S spring -G spring

# Crear directorio para credenciales de Google Cloud
RUN mkdir -p /app/credentials && chown spring:spring /app/credentials

USER spring:spring

# Copiar JAR desde etapa de build
COPY --from=build /app/target/IA-service-1.0-SNAPSHOT.jar app.jar

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:9093/actuator/health || exit 1

# Puerto
EXPOSE 9093

# Variables de entorno para Google Cloud (se configuran en runtime)
ENV GOOGLE_APPLICATION_CREDENTIALS=/app/credentials/credentials.json

# Comando de inicio
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
