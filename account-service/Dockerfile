# ==========================================
# Stage 1: Build con Maven
# ==========================================
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Copiar solo los archivos necesarios para el build
COPY pom.xml .
COPY account-service/pom.xml ./account-service/
COPY account-service/src ./account-service/src

# Descargar dependencias (caché de capas)
RUN mvn dependency:go-offline -B -f account-service/pom.xml

# Build del servicio
RUN mvn clean package -DskipTests -f account-service/pom.xml

# ==========================================
# Stage 2: Runtime Image
# ==========================================
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="Digital Money House Team"
LABEL description="Account Service Microservice"

WORKDIR /app

# Crear usuario no-root para seguridad
RUN addgroup -S spring && adduser -S spring -G spring

# Copiar JAR del stage de build
COPY --from=builder --chown=spring:spring /app/account-service/target/*.jar app.jar

# Copiar archivo de palabras para generación de alias (si existe)
COPY --chown=spring:spring account-service/src/main/resources/words.txt ./words.txt

# Cambiar a usuario no-root
USER spring:spring

# Healthcheck
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

# Exponer puerto
EXPOSE 8082

# JVM optimizaciones
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

# Ejecutar aplicación
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
