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

if [ ! -r "$SENHA_ARQUIVO" ]; then
  echo "migracao: segredo ausente em $SENHA_ARQUIVO" >&2
  exit 1
fi

exec flyway \
  -connectRetries=10 \
  -password="$(cat "$SENHA_ARQUIVO")" \
  migrate
