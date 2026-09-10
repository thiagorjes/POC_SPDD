# Stack — Infra Docker

> Coleção da camada `infra`. Vale para todo sistema deste workspace que roda em
> contêiner, independentemente da linguagem de dentro da imagem.
> Transversais que esta coleção materializa:
> [`../../_shared/vulnerable-and-proprietary-libs.md`](../../_shared/vulnerable-and-proprietary-libs.md) (CVE de imagem base),
> [`../../_shared/git-workflow.md`](../../_shared/git-workflow.md) (versionamento dos arquivos de infra) e
> [`../../_shared/architecture-principles.md`](../../_shared/architecture-principles.md) (fronteira entre serviços).

**Fronteira desta coleção.** Ela decide o que é transversal ao *ambiente* — rede,
volumes, ordem de subida, healthcheck, segredo, proveniência de imagem. O que é
imagem de um serviço específico (qual JDK, qual gerenciador de pacotes, qual
comando de build) continua na coleção da stack correspondente, e é lá que se
verifica. Onde as duas se tocam, esta coleção manda no *contrato* e a stack manda
no *conteúdo*.

## Runtime

| Item | Versão mínima fixa | Observação |
|---|---|---|
| Docker Engine | 27.3 | `docker version` (linha `Server: Engine`) |
| Docker Compose | v2.29 | Plugin `compose`, não o binário `docker-compose` v1 |
| Especificação Compose | 2024-10 (sem chave `version:`) | A chave `version:` é obsoleta e proibida |
| BuildKit | habilitado por padrão no Engine 27 | `DOCKER_BUILDKIT=0` é proibido |

Não existe "latest" nesta coleção — nem em imagem, nem em ferramenta. Versão que
não está escrita é versão que muda sem ninguém decidir.

## Ferramental obrigatório

| Ferramenta | Versão | Para quê | Como roda |
|---|---|---|---|
| `hadolint` | 2.12.0 | Lint de `Dockerfile` | `docker run --rm -i hadolint/hadolint:2.12.0-alpine < Dockerfile` |
| `docker compose config` | — | Valida e resolve o `compose.yaml` inteiro | `docker compose -f docker/compose.yaml config -q` |
| `trivy` | 0.58.0 | Varredura de CVE da imagem construída | `docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy:0.58.0 image <ref>` |

As três rodam na máquina de quem desenvolve. Não dependem de CI, e é de propósito:
enquanto não houver CI (ver `definition-of-done.md`), norma que só um pipeline
inexistente verifica não é norma.

## Imagens base em uso no workspace

Fixadas por `nome:tag@sha256:<digest>` — ver [ADR-013](../../../../docs/decisions/ADR-013-proveniencia-de-imagem-por-digest-sem-registry.md).
A tabela registra a tag legível; o digest vive no `Dockerfile`, que é onde o build
lê.

| Papel | Imagem | Fonte da versão |
|---|---|---|
| Build Java | `maven:3.9-eclipse-temurin-25` | `backend/java/stack.md` |
| Runtime Java | `eclipse-temurin:25-jre` | `backend/java/stack.md` |
| Build e runtime Node | `node:24-alpine` | `frontend/nextjs/stack.md` (Node 24.x) |
| Banco | variável `POSTGRES_IMAGE`, valor único por sistema | ADR-002 do sistema; consumida pelo `compose` **e** pelos testes |
| Identidade | imagem oficial do Keycloak, versão fixa por sistema | ADR-003 do sistema |

`POSTGRES_IMAGE` é regra, não conveniência: o mesmo valor precisa alimentar o
`compose` e os testes que sobem banco por biblioteca. "A mesma imagem do compose"
escrito em prosa não é verificável, e divergir de versão reintroduz devagar o
problema que motivou tirar o banco em memória da suíte.

## Alvo de execução

Docker on-premise, orquestrado por Compose. **Não há Kubernetes nem OpenShift**
neste workspace hoje (ADR-009 do sistema IDSD desviou disso explicitamente), e não
há publicação de imagem em produção.

O que isso restringe agora: nada de recurso que só exista em orquestrador
(readiness gate de plataforma, sidecar, ConfigMap). O que isso obriga a preservar:
as regras de probe separado, migração fora do boot e segredo por arquivo montado
existem em parte porque são o que sobrevive a uma migração futura para
orquestrador — desfazê-las é barato hoje e caro depois.

## Estrutura de arquivos

```
docker/
  compose.yaml           ambiente de desenvolvimento
  compose.test.yaml      suíte automatizada
  <servico>/Dockerfile   uma imagem por serviço
  <provedor>/…           material de bootstrap (realm, seed) versionado
.dockerignore            na raiz de cada contexto de build
```

`docker/` fica versionado no repositório do sistema, sob as regras de
[`../../_shared/git-workflow.md`](../../_shared/git-workflow.md). Arquivo de infra
não é gerado nem editado fora do controle de versão.

## Checklist

1. `docker version` reporta Engine ≥ 27.3 e Compose ≥ v2.29.
2. Nenhum `compose.yaml` contém a chave `version:`.
3. Toda imagem base referenciada tem digest.
4. A versão do banco vem de variável única, consumida por `compose` e testes.
5. `hadolint` e `docker compose config -q` passam sem saída.
