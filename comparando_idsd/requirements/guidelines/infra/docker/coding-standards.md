# Padrões de escrita — Infra Docker

> Materializa [`../../_shared/git-workflow.md`](../../_shared/git-workflow.md) (o que é
> versionado e como muda) e
> [`../../_shared/logging-and-levels.md`](../../_shared/logging-and-levels.md) (saída do
> contêiner). Decisões de topologia: [`architecture.md`](architecture.md).

## 1. Nomenclatura

| Elemento | Forma | Exemplo |
|---|---|---|
| Serviço do `compose` | `kebab-case`, papel e não tecnologia | `backend`, `backend-test`, `db-migrate` |
| Rede | `<sistema>-net`, `<sistema>-test-net` | `idsd-net` |
| Volume nomeado | `<sistema>-<dado>` | `idsd-pgdata` |
| Variável de ambiente | `SCREAMING_SNAKE_CASE` | `POSTGRES_IMAGE` |
| Arquivo | `compose.yaml`, `compose.test.yaml`, `<servico>/Dockerfile` | — |

Serviço nomeado pela tecnologia (`postgres17`, `keycloak26`) envelhece na troca de
versão e obriga a alterar todos os consumidores. Verificador: revisor humano.

## 2. Dockerfile

| Regra | Verificador |
|---|---|
| **Multi-stage obrigatório** quando há etapa de build. A imagem final não contém compilador, gerenciador de pacotes de build nem código-fonte | `hadolint` + revisor |
| **Usuário não-root no estágio final**, declarado por `USER` antes do `CMD` | `docker run --rm <imagem> id -u` ≠ `0` |
| Imagem base por `nome:tag@sha256:<digest>` | `grep -L '@sha256:' */Dockerfile` sem saída |
| `COPY` de arquivo específico, nunca `COPY . .` no estágio final | `hadolint` (DL3045) + revisor |
| `.dockerignore` existe em todo contexto de build e exclui `.git`, dependências instaladas e artefatos locais | Revisor humano |
| Camadas ordenadas do mais estável ao mais volátil: manifesto de dependências antes do código | Revisor humano — build que reinstala dependência a cada mudança de código está errado |
| `HEALTHCHECK` da aplicação declarado no `compose`, não no `Dockerfile` | Revisor humano — a mesma imagem serve ambientes com prontidão diferente |
| Sem `RUN apt-get upgrade`, sem instalação de pacote não fixado | `hadolint` (DL3005, DL3008, DL3018) |
| Sem `ENTRYPOINT` em forma de shell quando o processo precisa receber sinal | `hadolint` (DL3025) — forma exec, para o processo ser PID 1 e receber `SIGTERM` |

**Supressão de regra de lint é proibida sem justificativa no próprio arquivo.**
`# hadolint ignore=` só entra com o motivo na linha anterior e o nome de quem
autorizou. Verificador: `grep -n 'hadolint ignore' */Dockerfile` — cada ocorrência
precisa de comentário adjacente.

## 3. Compose

| Regra | Verificador |
|---|---|
| Sem a chave `version:` | `grep -c '^version:' docker/*.yaml` = 0 |
| Todo serviço declara `restart:` explicitamente | Revisor humano |
| Nenhum valor duplicado entre `compose.yaml` e `compose.test.yaml` que possa divergir: o comum sai por variável ou por arquivo de override | Revisor humano |
| `env_file` aponta para arquivo de exemplo versionado (`.env.example`); o `.env` real nunca é versionado | `git check-ignore .env` retorna 0 |
| Comando longo em `command:` vira script versionado em `docker/`, não linha de shell dentro do YAML | Revisor humano |
| Toda imagem de infraestrutura vem de variável fixada uma única vez | `docker compose config` exibe o mesmo valor em todos os pontos de uso |

`docker compose -f docker/compose.yaml config -q` precisa passar sem saída antes de
qualquer commit que toque em `docker/`.

## 4. Comentário

Comentário em arquivo de infra explica **por que**, nunca o que a linha faz. As
três coisas que sempre merecem comentário, porque a próxima pessoa vai desfazê-las
de boa-fé achando que simplifica:

- `condition: service_started` em vez de `service_healthy` — qual é o mecanismo de
  reconexão do consumidor;
- `start_period` de valor alto — o que ele está esperando;
- montagem do socket do daemon — o escopo em que é aceito ([ADR-012](../../../../docs/decisions/ADR-012-testcontainers-por-socket-do-host.md)).

Verificador: revisor humano.

## 5. Versionamento

Alteração em `docker/` é mudança de comportamento de ambiente e segue
[`../../_shared/git-workflow.md`](../../_shared/git-workflow.md). Duas regras
próprias:

- Mudança de imagem base ou de digest é **commit próprio**, com a razão na mensagem
  (atualização de CVE, salto de versão). Misturada a mudança de aplicação, ela
  desaparece na revisão e reaparece na quebra.
- `compose.yaml` e `compose.test.yaml` que precisam mudar juntos mudam no mesmo
  commit. Divergência entre os dois é o defeito que só aparece na suíte.

Verificador: revisor humano no histórico.

## Checklist

1. Multi-stage e `USER` não-root no estágio final.
2. Imagem base com digest; nenhuma supressão de lint sem justificativa nomeada.
3. `.dockerignore` presente e efetivo.
4. `hadolint` e `docker compose config -q` sem saída.
5. Nenhum `.env` real versionado.
6. Comentário presente nos três casos do §4.
7. Mudança de imagem base em commit próprio.
