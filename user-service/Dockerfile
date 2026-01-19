
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
COPY user-service/pom.xml ./user-service/
COPY user-service/src ./user-service/src

RUN mvn dependency:go-offline -B -f user-service/pom.xml

RUN mvn clean package -DskipTests -f user-service/pom.xml

FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="Digital Money House Team"
LABEL description="User Service Microservice"

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder --chown=spring:spring /app/user-service/target/*.jar app.jar

USER spring:spring

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

EXPOSE 8081

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
