# Build ---------------------------------------------------------------------
# O go-offline roda antes de copiar o codigo para que a camada de dependencias
# seja reaproveitada entre builds em que so o fonte mudou.
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
# Sem `-o`: o go-offline nao pre-resolve os intervalos de versao do Cucumber (`[24.0.0,25.0.0)`),
# que exigem consultar o metadata remoto. O cache da camada anterior continua sendo aproveitado.
RUN mvn -B package -DskipTests

# Runtime -------------------------------------------------------------------
FROM eclipse-temurin:25-jre
RUN useradd --system --uid 10001 --create-home kanban
WORKDIR /app
COPY --from=build --chown=kanban:kanban /build/target/*.jar app.jar
USER kanban
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
