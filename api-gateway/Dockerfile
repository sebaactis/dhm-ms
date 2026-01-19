FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
COPY api-gateway/pom.xml ./api-gateway/
COPY api-gateway/src ./api-gateway/src

RUN mvn dependency:go-offline -B -f api-gateway/pom.xml

RUN mvn clean package -DskipTests -f api-gateway/pom.xml

FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="Digital Money House Team"
LABEL description="API Gateway Microservice"

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder --chown=spring:spring /app/api-gateway/target/*.jar app.jar

USER spring:spring

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
