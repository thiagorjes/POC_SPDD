# Servico de migracao dedicado (ADR-011). Roda ate o fim e sai; a aplicacao
# sobe validando o schema, sem permissao de migrar.
FROM flyway/flyway:11-alpine@sha256:df06535dfd6559ae5f0628c4761013dfa4ea6a30cf6e34294931d8091f55a18f

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
