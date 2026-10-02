FROM maven:3.9.11-eclipse-temurin-21 AS build
RUN useradd --system --uid 10001 --create-home builder
WORKDIR /workspace
COPY --chown=builder:builder pom.xml ./
USER builder
RUN mvn -q -DskipTests dependency:go-offline
USER root
RUN chown -R builder:builder /workspace
USER builder
COPY --chown=builder:builder src ./src
RUN mvn -q verify

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
RUN useradd --system --uid 10001 --create-home securebiz
WORKDIR /app
COPY --from=build /workspace/target/securebiz-backend-0.1.0-SNAPSHOT.jar app.jar
USER securebiz
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
