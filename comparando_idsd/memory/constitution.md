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
| [ADR-004](../docs/decisions/ADR-004-broadcast-listen-notify.md) | Broadcast de eventos multi-pod via PostgreSQL LISTEN/NOTIFY | Aceito — contador de `seq` por pod superado por SDR-004 |
| [ADR-005](../docs/decisions/ADR-005-flyway-migrations.md) | Flyway para versionamento de schema | Aceito |
| [ADR-006](../docs/decisions/ADR-006-sem-fallback-auth-keycloak.md) | Sem fallback de autenticação local quando Keycloak indisponível | Aceito |
| [ADR-007](../docs/decisions/ADR-007-bootstrap-admin-global.md) | Bootstrap do primeiro admin via flag `adminGlobal` + e-mail configurado | Superado por ADR-010 |
| [ADR-008](../docs/decisions/ADR-008-dockerizacao-backend-frontend.md) | Dockerização de backend e frontend | Aceito — emendado em 2026-09-09 (INC-15, INC-17) |
| [ADR-009](../docs/decisions/ADR-009-desvio-da-colecao-backend-java.md) | Desvio declarado da coleção `backend/java`: Java 25, PostgreSQL, Docker on-premise | Aceito |
| [ADR-010](../docs/decisions/ADR-010-bootstrap-do-admin-global-por-subject-verificado.md) | Bootstrap do admin global por `subject_id` verificado, com promoção única e auditada | Aceito |
| [ADR-011](../docs/decisions/ADR-011-migracao-de-schema-em-servico-dedicado.md) | Migração de schema em serviço dedicado, fora do boot da aplicação | Aceito |
| [ADR-012](../docs/decisions/ADR-012-testcontainers-por-socket-do-host.md) | Suíte em contêiner acessa o daemon do host por socket montado; DinD descartado | Aceito |
| [ADR-013](../docs/decisions/ADR-013-proveniencia-de-imagem-por-digest-sem-registry.md) | Proveniência de imagem por digest, sem registry enquanto não houver publicação | Aceito |

### BDR

| ID | Título | Status |
|----|--------|--------|
| [BDR-001](../docs/decisions/BDR-001-rbac-por-projeto.md) | RBAC por projeto com papéis acumuláveis | Aceito — numeração herdada anotada em 2026-09-10 (INC-21) |
| [BDR-002](../docs/decisions/BDR-002-permissao-de-desbloqueio-em-papeis-existentes.md) | Desbloqueio como permissão de `product_owner` e `project_admin`, sem papel novo | Aceito |

### SDR

| ID | Título | Status |
|----|--------|--------|
| [SDR-001](../docs/decisions/SDR-001-eventos-imutaveis-com-projecao-de-intervalos.md) | Eventos imutáveis como verdade, com projeção de intervalos para leitura | Aceito |
| [SDR-002](../docs/decisions/SDR-002-concorrencia-por-estado-de-origem-declarado.md) | Concorrência e idempotência por estado de origem declarado | Aceito |
| [SDR-003](../docs/decisions/SDR-003-testcontainers-e-playwright.md) | Testcontainers no lugar do H2 e Playwright para os cenários E2E | Aceito |
| [SDR-004](../docs/decisions/SDR-004-seq-no-banco-e-publicador-unico.md) | Sequência de eventos gerada no banco e publicador único do broadcast | Aceito |
| [SDR-005](../docs/decisions/SDR-005-substituicao-de-fluxo-serializada-no-projeto.md) | Substituição de fluxo serializada por bloqueio pessimista do projeto | Aceito |
| [SDR-006](../docs/decisions/SDR-006-reconstrucao-total-dos-intervalos-parcial-do-estado.md) | Reconstrução total da série de tempo, parcial e declarada do estado | Aceito |
| [SDR-007](../docs/decisions/SDR-007-recorte-de-terminais-no-board-por-instante-projetado.md) | Recorte de terminais no board por instante projetado em `tarefa` | Aceito |

### DDR

| ID | Título | Status |
|----|--------|--------|
| [DDR-001](../docs/decisions/DDR-001-design-tokens-base.md) | Tokens base de design: cores, tipografia e espaçamento | Superado por DDR-004 |
| [DDR-002](../docs/decisions/DDR-002-drag-and-drop-board.md) | Interação do board: drag-and-drop com destaque de colunas válidas + menu alternativo | Aceito |
| [DDR-003](../docs/decisions/DDR-003-feedback-async-acessibilidade.md) | Padrões de feedback, loading assíncrono e nível de acessibilidade | Superado por DDR-005 |
| [DDR-004](../docs/decisions/DDR-004-adocao-design-system-nextjs.md) | Adoção do design system da coleção Next.js como fonte de verdade visual | Aceito |
| [DDR-005](../docs/decisions/DDR-005-acessibilidade-wcag-aa-obrigatoria.md) | Acessibilidade WCAG 2.1 AA obrigatória e feedback realinhado ao design system | Aceito |
| [DDR-006](../docs/decisions/DDR-006-espera-de-tomada-primeira-classe.md) | Espera de tomada como estado visível de primeira classe, com fila própria | Aceito |
| [DDR-007](../docs/decisions/DDR-007-impedimento-como-dimensao-visual-ortogonal.md) | Impedimento como dimensão visual ortogonal, que só desabilita o que o contrato recusa | Aceito — emendado em 2026-09-09 (INC-14) |

---

## Princípios Estáveis

_(preencher após /guidelines)_
