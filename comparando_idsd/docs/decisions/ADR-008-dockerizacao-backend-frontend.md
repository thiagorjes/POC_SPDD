# ADR-008 — Dockerização de backend e frontend

_Status: Aceito — emendado em 2026-09-09 (achado INC-15) | Data: 2026-08-26 | Feature: kanban-tarefas_

> **Revisão de 2026-09-09 (achado INC-15).** A decisão central deste DR não
> mudou: backend e frontend rodam em contêiner, com Dockerfile multi-stage e
> orquestração declarativa por Compose. O que mudou foi o **detalhe operacional**,
> que este DR fixou em agosto quando não havia norma de containerização e que
> passou a ser governado pela coleção `infra/docker`, elaborada em 2026-09-09.
> Onde os dois divergirem, **a coleção manda** — este DR não é fonte de norma de
> ambiente. As divergências conhecidas estão marcadas em linha abaixo.
>
> A emenda foi feita no lugar, e não por superação, porque superar registraria
> decisão nova onde houve conformação a normas decididas em outro artefato.

## Contexto

RNF-004 (PRD) e `architecture.md`/`stack.md` já exigiam empacotamento em container (Docker/OpenShift/Kubernetes), e `stack.md` já documentava a convenção de `backend/Dockerfile` (multi-stage `maven:3.9-eclipse-temurin-25` → `eclipse-temurin:25-jre`). Na prática, `systems/CRUDAO/docker-compose.yml` só sobe a infra (`postgres`, `keycloak`) — backend e frontend rodam local via `mvnw spring-boot:run`/`npm run dev`, quebrando a homologação/portabilidade prevista. Requisito reforçado explicitamente pelo usuário em 2026-08-26: back e front devem rodar em Docker.

## Decisão

Fechar o gap com dois Dockerfiles multi-stage e a extensão do `docker-compose.yml` existente:

- **`backend/Dockerfile`** — stage de build `maven:3.9-eclipse-temurin-25` (cache de dependências via `mvn dependency:go-offline` antes de copiar o código), stage de runtime `eclipse-temurin:25-jre` rodando o jar como usuário não-root.
  **Emendado em 2026-09-09:** a redação original dizia que o Flyway continuava
  aplicando migrations no boot. Isso deixou de valer — por
  [ADR-011](ADR-011-migracao-de-schema-em-servico-dedicado.md) a migração roda em
  serviço dedicado do Compose, que executa e sai, e a aplicação sobe com
  `ddl-auto=validate` e **sem permissão de migrar**. ADR-005 continua valendo
  para *o quê* versiona o schema; ADR-011 decide *quem* o executa. A frase
  antiga era operante e perigosa: o teste de RNF-002 sobe três instâncias
  simultâneas, que disputariam o lock de migração.
- **`frontend/Dockerfile`** — stage de build `node:20-alpine` *(versão superada:
  a imagem base vigente é a da tabela de `infra/docker/stack.md`, fixada por
  digest — ver [ADR-013](ADR-013-proveniencia-de-imagem-por-digest-sem-registry.md))* (`npm ci && npm run build`), stage de runtime servindo via `next start` (ou `output: standalone` do Next.js, reduzindo a imagem final). Variáveis de ambiente (`NEXT_PUBLIC_BACKEND_URL`, `SESSION_SECRET`, URLs do Keycloak) injetadas via `environment:` do compose, nunca hardcoded na imagem.
  *Emendado: o que é **credencial** não entra por variável nem por `.env`
  versionado — entra por arquivo montado (`infra/docker/architecture.md` §7).
  Variável de ambiente vaza em `docker inspect` e em log de crash.*
- **`docker-compose.yml`** ganha os serviços `backend` (porta 8081, `depends_on: postgres` com `condition: service_healthy`, `depends_on: keycloak`) e `frontend` (porta 3000, `depends_on: backend`). `postgres`/`keycloak` inalterados.
- Rede padrão do compose (bridge) — serviços se resolvem por nome (`postgres`, `keycloak`, `backend`), URLs internas trocam `localhost` por nome do serviço nas variáveis de ambiente do container.
- `docker compose up -d` sobe a stack completa (infra + app); `docker compose up -d postgres keycloak` continua válido para quem quiser rodar backend/frontend local durante desenvolvimento ativo (hot reload).

## Alternativas consideradas

| Opção | Prós | Contras |
|---|---|---|
| **Dockerfiles + extensão do compose existente (escolhida)** | Reaproveita a infra já validada (ADR-005, ADR-006 e o então vigente ADR-007, realm Keycloak); zero mudança de arquitetura de runtime; alinhado ao que `stack.md`/RNF-004 já previam | Imagem de dev não é a mesma de produção |
| Multi-stage único orquestrando tudo via script (sem compose) | — | Perde orquestração declarativa, healthcheck e dependência entre serviços que o compose já oferece |
| Manter backend/frontend fora do Docker, só documentar como rodar local | Zero esforço | Não atende ao requisito explícito do usuário nem ao RNF-004; quebra paridade dev/homologação |

> **Emenda de 2026-09-09 (achado INC-17).** Duas referências desta tabela
> envelheceram e ficam registradas em vez de apagadas: ADR-007 foi **superado por
> [ADR-010](ADR-010-bootstrap-do-admin-global-por-subject-verificado.md)**, e a
> ressalva original previa que "produção real usaria manifests OpenShift/K8s".
> Não usará: [ADR-009](ADR-009-desvio-da-colecao-backend-java.md) desviou disso
> explicitamente e `infra/docker/stack.md` registra que não há Kubernetes nem
> OpenShift neste workspace. O alvo é Docker on-premise orquestrado por Compose.

## Consequências

- Homologação e onboarding passam a ser `docker compose up -d` único, sem instalar JDK 25/Node localmente.
- `quickstart.md` e a Seção 5 da TechSpec principal atualizados para refletir o setup via Docker como caminho padrão (mantendo o setup local como alternativa de desenvolvimento ativo).
- Gera task nova de implementação (Dockerfiles + compose) — não coberta pelas tasks do Epic 08 já escritas; adicionar ao `/tasks` antes de fechar o hardening final.
- CI/CD (`git-workflow.md`/`devops`) pode reusar as mesmas imagens em pipelines futuros — fora de escopo desta ADR.

## Referências

RNF-004, `stack.md` (convenção de Dockerfile já documentada), `architecture.md` (deploy containerizado on-premise via Docker).

Desde 2026-09-09, a norma de ambiente é a coleção
`requirements/guidelines/infra/docker/` — topologia, rede, volumes, ordem de
subida, healthcheck, segredo, proveniência de imagem e ambiente de teste —,
com [ADR-011](ADR-011-migracao-de-schema-em-servico-dedicado.md),
[ADR-012](ADR-012-testcontainers-por-socket-do-host.md) e
[ADR-013](ADR-013-proveniencia-de-imagem-por-digest-sem-registry.md).
