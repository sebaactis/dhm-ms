# ==========================================
# Stage 1: Build con Maven
# ==========================================
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Copiar solo los archivos necesarios para el build
COPY pom.xml .
COPY user-service/pom.xml ./user-service/
COPY user-service/src ./user-service/src

# Descargar dependencias (caché de capas)
RUN mvn dependency:go-offline -B -f user-service/pom.xml

# Build del servicio
RUN mvn clean package -DskipTests -f user-service/pom.xml

# ==========================================
# Stage 2: Runtime Image
# ==========================================
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="Digital Money House Team"
LABEL description="User Service Microservice"

WORKDIR /app

# Crear usuario no-root para seguridad
RUN addgroup -S spring && adduser -S spring -G spring

# Copiar JAR del stage de build
COPY --from=builder --chown=spring:spring /app/user-service/target/*.jar app.jar

# Cambiar a usuario no-root
USER spring:spring

# Healthcheck
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Exponer puerto
EXPOSE 8081

# JVM optimizaciones
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

# Ejecutar aplicación
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
