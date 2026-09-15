#!/bin/sh
# A senha do banco chega por arquivo montado, nunca por FLYWAY_PASSWORD:
# variavel de ambiente vaza em `docker inspect`, e a secao 7 de
# `infra/docker/architecture.md` nao abre excecao por ferramenta.
#
# O valor entra como argumento de linha de comando de um processo que vive
# dentro deste contêiner e sai em seguida. Nao aparece na inspecao do
# contêiner, nao entra na historia de camadas da imagem e nao fica em arquivo
# versionado.
set -eu

SENHA_ARQUIVO="${FLYWAY_PASSWORD_FILE:-/run/secrets/banco-senha}"

# Senha da role de aplicacao. Quem a ATRIBUI e a migracao, no callback
# `afterMigrate`, porque atribuir senha de role e ato de dono e a migracao e o
# unico servico que conecta como dono (ADR-011). O backend so a apresenta.
APP_SENHA_ARQUIVO="${APP_PASSWORD_FILE:-/run/secrets/app-senha}"

if [ ! -r "$SENHA_ARQUIVO" ]; then
  echo "migracao: segredo ausente em $SENHA_ARQUIVO" >&2
  exit 1
fi

if [ ! -r "$APP_SENHA_ARQUIVO" ]; then
  # Falha ruidosa de proposito: sem isto a role de aplicacao existiria sem
  # senha, o backend nao conectaria, e o sintoma apareceria a um servico de
  # distancia da causa.
  echo "migracao: segredo da aplicacao ausente em $APP_SENHA_ARQUIVO" >&2
  exit 1
fi

exec flyway \
  -connectRetries=10 \
  -password="$(cat "$SENHA_ARQUIVO")" \
  -placeholders.senha_aplicacao="$(cat "$APP_SENHA_ARQUIVO")" \
  migrate
