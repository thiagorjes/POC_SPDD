# Servico de migracao dedicado (ADR-011). Roda ate o fim e sai; a aplicacao
# sobe validando o schema, sem permissao de migrar.
#
# Linha 13, e nao 11: a 11 parou de receber build a montante, e por isso a base
# Alpine dela envelheceu com openssl, musl, zlib e gnutls vulneraveis (5
# CRITICAL e 56 HIGH — ACH-01 da revisao de TASK-01.2). Nao ha digest mais novo
# da tag `11-alpine` para onde ir; a correcao e a linha viva.
#
# A troca de major nao alcanca a aplicacao: ela nao migra em perfil nenhum
# (`flyway.enabled: false`), e o Flyway de biblioteca que a suite usa roda
# contra banco criado do zero pelo Testcontainers. As duas historias de
# migracao nunca se encontram.
FROM flyway/flyway:13.6.0-alpine@sha256:fb7f326765649205574551e25ba774e432ea840b126983d6ec3bbda8f21b137c

# Pacote de sistema da base ao dia. A base e reconstruida a montante em cadencia
# propria, e sem isto a imagem carrega o intervalo entre a correcao publicada no
# Alpine e o proximo build de terceiro.
#
# E saem os tres drivers onde viviam TODOS os achados Java da varredura:
# `couchbase/core-io` (netty, 1 CRITICAL e 5 HIGH), `databricks-jdbc` (jackson,
# httpcore5) e `mssql-jdbc`. Este sistema fala com PostgreSQL e com mais nada
# (ADR-002), entao sao codigo que nunca executa — remover e correcao, nao
# supressao: ele deixa de existir na imagem.
#
# Removidos por nome, e nao por "tudo menos postgresql": o registro de plugins
# do Flyway percorre os diretorios de driver na subida, e apagar um deles
# inteiro derruba o CLI antes da primeira migration
# (`ClassNotFoundException: com.datastax...`, medido). O que os tres tem em
# comum e serem jar solto, sem plugin que os procure.
# hadolint ignore=DL3018
RUN apk --no-cache upgrade \
  && rm -f /flyway/drivers/couchbase/core-io-*.jar \
           /flyway/drivers/databricks-jdbc-*.jar \
           /flyway/drivers/mssql-jdbc-*.jar

# As migrations sao as mesmas que a aplicacao carrega no classpath — uma fonte
# so. COPY de diretorio especifico, nunca `COPY . .`.
COPY backend/src/main/resources/db/migration /flyway/sql

COPY docker/migracao/entrypoint.sh /flyway/entrypoint.sh

# A imagem oficial roda como root e nao traz usuario proprio. Criar um aqui e o
# minimo: o servico que aplica migration e o unico do ambiente com permissao de
# alterar o schema, e nao ha razao para ele tambem ter root no contêiner.
RUN addgroup -S migracao \
  && adduser -S -u 10001 -G migracao migracao \
  && chown -R migracao:migracao /flyway

USER migracao

ENTRYPOINT ["/bin/sh", "/flyway/entrypoint.sh"]
