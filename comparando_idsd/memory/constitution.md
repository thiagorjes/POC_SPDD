# Constituição — IDSD
_Criada em: 2026-09-03_

> Princípios estáveis, ADRs e decisões de design do workspace.
> Atualizado apenas quando os fundamentos mudarem.

---

## Contexto do Workspace

- **IDSD** — cenário: Novo (greenfield)
- **Propósito:** _(preencher após /guidelines)_
- **Idioma:** pt_BR

---

## Decision Records

> Ao criar uma nova DR, adicione uma linha na tabela do tipo correspondente.
> Coluna **ID** deve ser um link relativo para o arquivo em `docs/decisions/`:
> `[ADR-001](../docs/decisions/ADR-001-titulo-curto.md)`.
> O próximo NNN é o maior já usado no tipo + 1 — os contadores são
> independentes por tipo. ID atribuído é imutável; DR superada é marcada como
> `superseded-by`, nunca apagada nem renumerada.

### ADR

| ID | Título | Status |
|----|--------|--------|
| [ADR-001](../docs/decisions/ADR-001-stack-backend-java-spring.md) | Stack de backend: Java 25 + Spring Boot (LTS) | Aceito |
| [ADR-002](../docs/decisions/ADR-002-postgresql-sem-cache-tempo-real.md) | PostgreSQL como único armazenamento; sem cache/broker nesta fase | Aceito |
| [ADR-003](../docs/decisions/ADR-003-rbac-hibrido-keycloak.md) | RBAC híbrido: Keycloak autentica, permissões modeladas na aplicação | Aceito |
| [ADR-004](../docs/decisions/ADR-004-broadcast-listen-notify.md) | Broadcast de eventos multi-pod via PostgreSQL LISTEN/NOTIFY | Aceito |
| [ADR-005](../docs/decisions/ADR-005-flyway-migrations.md) | Flyway para versionamento de schema | Aceito |
| [ADR-006](../docs/decisions/ADR-006-sem-fallback-auth-keycloak.md) | Sem fallback de autenticação local quando Keycloak indisponível | Aceito |
| [ADR-007](../docs/decisions/ADR-007-bootstrap-admin-global.md) | Bootstrap do primeiro admin via flag `adminGlobal` + e-mail configurado | Aceito |
| [ADR-008](../docs/decisions/ADR-008-dockerizacao-backend-frontend.md) | Dockerização de backend e frontend | Aceito |

### BDR

| ID | Título | Status |
|----|--------|--------|
| [BDR-001](../docs/decisions/BDR-001-rbac-por-projeto.md) | RBAC por projeto com papéis acumuláveis | Aceito |

### SDR

| ID | Título | Status |
|----|--------|--------|

### DDR

| ID | Título | Status |
|----|--------|--------|
| [DDR-001](../docs/decisions/DDR-001-design-tokens-base.md) | Tokens base de design: cores, tipografia e espaçamento | Aceito |
| [DDR-002](../docs/decisions/DDR-002-drag-and-drop-board.md) | Interação do board: drag-and-drop com destaque de colunas válidas + menu alternativo | Aceito |
| [DDR-003](../docs/decisions/DDR-003-feedback-async-acessibilidade.md) | Padrões de feedback, loading assíncrono e nível de acessibilidade | Aceito |

---

## Princípios Estáveis

_(preencher após /guidelines)_
