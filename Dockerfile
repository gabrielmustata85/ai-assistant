# syntax=docker/dockerfile:1

# ---- Build stage (JDK 21 compiles the Java 21 target) ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# Cache dependencies first (pom only), then build the app
COPY pom.xml .
RUN mvn -q -B -DskipTests dependency:go-offline || true
COPY src ./src
RUN mvn -q -B -DskipTests clean package

# ---- Run stage (lightweight JRE) ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
