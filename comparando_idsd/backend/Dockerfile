# Multi-stage: a imagem final nao contem compilador, Maven nem codigo-fonte.
FROM maven:3.9-eclipse-temurin-25@sha256:31618505df21177d2baa3dc574be2d0b0b32614c8539baca1f23a9136b766eb0 AS construcao

WORKDIR /construcao

# Camadas do mais estavel ao mais volatil: o manifesto de dependencias antes do
# codigo, para que mudanca de codigo nao rebaixe o mundo inteiro de novo.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

# So o codigo de producao entra na imagem. A suite roda no servico
# `backend-test` de compose.test.yaml, que e quem tem o socket do daemon —
# Testcontainers nao funciona dentro de um build, e compilar a suite aqui
# amarraria a construcao da imagem ao Red que ela mede.
COPY src/main ./src/main
RUN mvn -B -q -Dmaven.test.skip=true package

FROM eclipse-temurin:25-jre@sha256:15090d159279e5c158473eccb48cd87f57b3e3a47511a797eb5a7a7ea6f86b0f AS runtime

# curl existe aqui por um motivo unico: o healthcheck de readiness que o
# compose declara. A imagem base nao traz nenhuma ferramenta HTTP, e a norma
# proibe healthcheck com ferramenta ausente. Versao fixada — imagem base por
# digest com pacote nao fixado ainda muda sozinha.
# hadolint ignore=DL3008
RUN apt-get update \
  && apt-get install --no-install-recommends -y curl=8.18.0-1ubuntu2.5 \
  && rm -rf /var/lib/apt/lists/*

RUN useradd --system --create-home --uid 10001 kanban
WORKDIR /app

COPY --from=construcao /construcao/target/kanban-0.1.0-SNAPSHOT.jar /app/kanban.jar

USER kanban
EXPOSE 8080

# HEALTHCHECK fica no compose, nao aqui: a mesma imagem serve ambientes com
# criterio de prontidao diferente.
# Forma exec, para o processo ser PID 1 e receber SIGTERM.
ENTRYPOINT ["java", "-jar", "/app/kanban.jar"]
