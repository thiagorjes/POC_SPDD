#!/usr/bin/env bash
# Healthcheck do provedor de identidade.
#
# A imagem oficial nao traz curl nem wget no runtime, e a norma
# (`infra/docker/architecture.md` secao 5) proibe healthcheck com ferramenta
# HTTP que a imagem nao tem — ele passa a falhar sempre ou e silenciosamente
# desligado. Sobra o `/dev/tcp` do bash, que a imagem tem.
#
# Sonda a porta de GERENCIAMENTO (9000), nunca a da aplicacao: a porta da
# aplicacao responde antes de o realm estar importado, e e o realm importado
# que o backend precisa.
set -euo pipefail

exec 3<>/dev/tcp/localhost/9000
printf 'GET /health/ready HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n' >&3
grep -q '"status": "UP"' <&3
