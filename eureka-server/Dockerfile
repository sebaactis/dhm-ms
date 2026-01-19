FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
COPY eureka-server/pom.xml ./eureka-server/
COPY eureka-server/src ./eureka-server/src

RUN mvn dependency:go-offline -B -f eureka-server/pom.xml

RUN mvn clean package -DskipTests -f eureka-server/pom.xml

FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="Digital Money House Team"
LABEL description="Eureka Server Microservice"

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder --chown=spring:spring /app/eureka-server/target/*.jar app.jar

USER spring:spring

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8761/actuator/health || exit 1

EXPOSE 8761

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
