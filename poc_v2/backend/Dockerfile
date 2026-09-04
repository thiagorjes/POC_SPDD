# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
# Sem -o: go-offline nao pre-resolve intervalos de versao, que exigem metadata remoto.
RUN mvn -B -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --uid 10001 --create-home kanban
COPY --from=build /app/target/*.jar /app/kanban.jar
USER kanban
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/kanban.jar"]
