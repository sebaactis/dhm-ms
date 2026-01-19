
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
COPY account-service/pom.xml ./account-service/
COPY account-service/src ./account-service/src

RUN mvn dependency:go-offline -B -f account-service/pom.xml

RUN mvn clean package -DskipTests -f account-service/pom.xml

FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="Digital Money House Team"
LABEL description="Account Service Microservice"

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder --chown=spring:spring /app/account-service/target/*.jar app.jar

COPY --chown=spring:spring account-service/src/main/resources/words.txt ./words.txt

USER spring:spring

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

EXPOSE 8082

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
s
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
