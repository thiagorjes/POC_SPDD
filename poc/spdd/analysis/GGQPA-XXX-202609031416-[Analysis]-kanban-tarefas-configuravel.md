# SPDD Analysis: Kanban de Tarefas Configurável (CRUDAO) — Board, Impedimentos, Lead-time e RBAC por Projeto

_Gerado por `/spdd-analysis` em 2026-09-03 | Fonte: `requirements/` (PRD v1.0, design brief v1.0, screen map, 8 ADRs, 1 BDR, 3 DDRs, 6 guidelines)_

---

## Original Business Requirement

A entrada é a pasta `requirements/` completa. Os documentos normativos primários (PRD e Design Brief) estão reproduzidos **na íntegra** abaixo. Os demais artefatos lidos integralmente durante a análise e tratados como **restrições vinculantes** são:

| Artefato | Caminho | Natureza |
|---|---|---|
| Guidelines — Architecture | `requirements/guidelines/architecture.md` | Restrição arquitetural |
| Guidelines — Stack | `requirements/guidelines/stack.md` | Restrição tecnológica (versões fixadas) |
| Guidelines — Coding Standards | `requirements/guidelines/coding-standards.md` | Norma de código |
| Guidelines — Security | `requirements/guidelines/security.md` | Norma de segurança |
| Guidelines — Testing | `requirements/guidelines/testing.md` | Norma de qualidade (TDD 80% / BDD 100%) |
| Guidelines — Observability | `requirements/guidelines/observability.md` | Norma operacional |
| ADR-001 | `requirements/decisions/ADR-001-stack-backend-java-spring.md` | Java 25 + Spring Boot, JPA, PostgreSQL, STOMP, OIDC |
| ADR-002 | `requirements/decisions/ADR-002-postgresql-sem-cache-tempo-real.md` | PostgreSQL único armazenamento; sem cache/broker |
| ADR-003 | `requirements/decisions/ADR-003-rbac-hibrido-keycloak.md` | RBAC híbrido: Keycloak autentica, app autoriza |
| ADR-004 | `requirements/decisions/ADR-004-broadcast-listen-notify.md` | Broadcast multi-pod via `LISTEN/NOTIFY` + `seq` + resync |
| ADR-005 | `requirements/decisions/ADR-005-flyway-migrations.md` | Flyway; `ddl-auto=validate` |
| ADR-006 | `requirements/decisions/ADR-006-sem-fallback-auth-keycloak.md` | Sem fallback de autenticação local |
| ADR-007 | `requirements/decisions/ADR-007-bootstrap-admin-global.md` | `Usuario.adminGlobal` + `kanban.bootstrap.admin-email` |
| ADR-008 | `requirements/decisions/ADR-008-dockerizacao-backend-frontend.md` | Dockerfiles backend/frontend + compose completo |
| BDR-001 | `requirements/decisions/BDR-001-rbac-por-projeto.md` | Papéis acumuláveis escopados por (usuário, projeto) |
| DDR-001 | `requirements/decisions/ddr-001-design-tokens-base.md` | Tokens base (cores, tipografia, espaçamento) |
| DDR-002 | `requirements/decisions/ddr-002-drag-and-drop-board.md` | Drag-and-drop com destaque + menu alternativo |
| DDR-003 | `requirements/decisions/ddr-003-feedback-async-acessibilidade.md` | Modal/toast/skeleton; WCAG não obrigatório |
| Screen Map | `requirements/design/kanban-tarefas/screen-map.md` | Rotas e componentes por tela (TL-01..TL-10) |
| Design Tokens | `requirements/design/kanban-tarefas/design-tokens.json` | Tokens implementáveis |
| Protótipos | `requirements/design/kanban-tarefas/prototypes/*.html` + `_shared.css` | 11 telas navegáveis com estados |

---

### PRD (verbatim) — `requirements/prd/kanban-tarefas-prd.md`

# PRD — Kanban de Tarefas
_Versão: 1.0 | Status: Draft | Data: 2026-08-24 | Autor: Thiago Goncalves Cavalcante_

---

## 1. Visão Geral

**Problema:** A equipe de desenvolvimento não tem uma forma centralizada de controlar o andamento das atividades. O acompanhamento hoje depende de reports manuais via email e chat, que precisam ser cruzados manualmente para identificar impedimentos e direcioná-los ao responsável — gerando perda de mensagens críticas (há caso registrado de demanda parada por 5 dias por aviso de impedimento não visto a tempo).

**Solução proposta:** Sistema de kanban com workflows e colunas configuráveis por projeto, onde os próprios desenvolvedores atualizam status e sinalizam impedimentos, com notificações internas de impedimento, lead-time visível por etapa no board e agregado em dashboard, controle de acesso por papéis escopados por projeto e autenticação via SSO (Keycloak).

**Público-alvo:** Equipe de desenvolvimento (devs e liderança técnica) e gestores de outros times que precisam de visibilidade sem participar da execução.

---

## 2. Stakeholders

| Papel | Nome | Responsabilidade |
|-------|------|-----------------|
| Product Owner / Aprovador | Thiago Goncalves Cavalcante | Aprovação de requisitos |
| Usuário final — Equipe de Dev | Devs e liderança técnica | Atualização de status/impedimentos, validação de UX |
| Usuário final — Gestores | Gestores de outros times | Consumo de dashboard e visibilidade de lead-time |

---

## 3. Requisitos Funcionais

### RF-001 — Board com colunas configuráveis por projeto

**Como** membro da equipe de desenvolvimento, **quero** visualizar um board com colunas configuráveis por projeto, **para** acompanhar o andamento das tarefas de acordo com o fluxo de trabalho específico daquele projeto.

**Critérios de aceite:**

**Dado que** um projeto possui um workflow com colunas definidas
**Quando** um usuário abre o board do projeto
**Então** o sistema exibe as colunas na ordem configurada, cada uma com as tarefas na etapa correspondente

**Prioridade:** Must Have

---

### RF-002 — Workflows com transições configuráveis entre etapas

**Como** administrador de projeto, **quero** configurar quais transições entre etapas são permitidas, **para** garantir que o fluxo de trabalho do projeto seja respeitado.

**Critérios de aceite:**

**Dado que** um workflow define transições permitidas entre colunas
**Quando** um usuário tenta mover uma tarefa para uma coluna sem transição configurada a partir da coluna atual
**Então** o sistema bloqueia a movimentação e informa que a transição não é permitida

**Prioridade:** Must Have

---

### RF-003 — CRUD de tarefas com trava de edição pós-"iniciada"

**Como** membro da equipe de desenvolvimento, **quero** criar, visualizar, editar e excluir tarefas, **para** manter o board atualizado com o trabalho real, respeitando o congelamento de campos após a tarefa ser iniciada.

**Critérios de aceite:**

**Dado que** uma tarefa já teve sua execução iniciada (saiu da primeira etapa do workflow)
**Quando** um usuário tenta editar campos estruturais da tarefa (ex.: descrição de escopo)
**Então** o sistema bloqueia a edição desses campos, permitindo apenas os campos definidos como editáveis pós-início (ex.: responsável, status, impedimento)

**Prioridade:** Must Have

---

### RF-004 — Sinalização de impedimento

**Como** membro da equipe de desenvolvimento, **quero** marcar e desmarcar uma tarefa como impedida, **para** sinalizar bloqueios sem depender de comunicação dispersa.

**Critérios de aceite:**

**Dado que** uma tarefa está em andamento
**Quando** um usuário autorizado marca a tarefa como impedida
**Então** o sistema registra o início do período de impedimento, exibe o indicador visual no board e inicia a contagem de lead-time de impedimento

**Prioridade:** Must Have

---

### RF-005 — Notificação de transições aos observadores

**Como** membro da equipe de desenvolvimento ou gestor, **quero** ser notificado internamente quando uma tarefa que observo muda de etapa ou é marcada como impedida, **para** agir rapidamente sem depender de cruzamento manual de reports.

**Critérios de aceite:**

**Dado que** um usuário observa uma tarefa (é responsável, criador ou observador explícito)
**Quando** a tarefa muda de etapa ou tem impedimento marcado/desmarcado
**Então** o sistema gera uma notificação interna visível ao usuário observador

**Prioridade:** Must Have

---

### RF-006 — Cálculo de lead-time por etapa

**Como** gestor ou membro da equipe, **quero** visualizar o lead-time de cada tarefa por etapa, **para** identificar gargalos no fluxo de trabalho.

**Critérios de aceite:**

**Dado que** uma tarefa passou por uma ou mais etapas
**Quando** um usuário abre o detalhe da tarefa
**Então** o sistema exibe o tempo decorrido em cada etapa e o tempo total de impedimento acumulado, conforme RN-001 e RN-002

**Prioridade:** Must Have

---

### RF-007 — Dashboard de gestão com lead-time médio

**Como** gestor, **quero** visualizar um dashboard com o lead-time médio por etapa e por projeto, **para** ter visibilidade de andamento sem precisar acompanhar a execução diretamente.

**Critérios de aceite:**

**Dado que** existem tarefas com histórico de movimentação em um projeto
**Quando** um gestor acessa o dashboard do projeto
**Então** o sistema exibe o lead-time médio por etapa, incluindo o tempo médio de impedimento agregado

**Prioridade:** Must Have

---

### RF-008 — CRUD de projetos (incl. finalizar/reabrir)

**Como** administrador, **quero** criar, editar, finalizar e reabrir projetos, **para** gerenciar o ciclo de vida dos projetos no sistema.

**Critérios de aceite:**

**Dado que** um projeto está ativo
**Quando** um administrador finaliza o projeto
**Então** o projeto passa a ser somente leitura para todos os usuários (conforme RN-015), até que seja reaberto

**Prioridade:** Must Have

---

### RF-009 — CRUD de workflows por projeto

**Como** administrador de projeto, **quero** criar, editar e excluir workflows associados a um projeto, **para** adaptar o fluxo de trabalho às necessidades do time.

**Critérios de aceite:**

**Dado que** um workflow não possui tarefas ativas vinculadas
**Quando** um administrador exclui o workflow
**Então** o sistema remove o workflow; caso existam tarefas ativas vinculadas, o sistema bloqueia a exclusão (RN-005)

**Prioridade:** Must Have

---

### RF-010 — CRUD de colunas (etapas) no board

**Como** administrador de projeto, **quero** criar, editar, reordenar e excluir colunas de um workflow, **para** representar as etapas reais do processo.

**Critérios de aceite:**

**Dado que** uma coluna não é a etapa final e não possui tarefas ativas
**Quando** um administrador configura a coluna
**Então** o sistema exige ao menos uma transição de saída configurada (RN-003), exceto para a etapa final

**Prioridade:** Must Have

---

### RF-011 — CRUD de raias (swimlanes) no board

**Como** administrador de projeto, **quero** criar, editar e excluir raias (swimlanes) no board, **para** organizar tarefas por categoria, time ou critério definido pelo projeto.

**Critérios de aceite:**

**Dado que** um projeto possui ao menos uma raia
**Quando** um usuário visualiza o board
**Então** as tarefas são agrupadas visualmente pelas raias configuradas

**Prioridade:** Must Have

---

### RF-012 — Etapa final com opção de reabertura

**Como** usuário autorizado, **quero** mover uma tarefa para a etapa final ou reabri-la ("desfinalizar"), **para** concluir ou retomar o trabalho quando necessário.

**Critérios de aceite:**

**Dado que** uma tarefa está na etapa final
**Quando** um usuário com permissão `tarefa:finalizar` executa a ação de "desfinalizar"
**Então** o sistema retorna a tarefa para a etapa selecionada, conforme RN-004 e RN-011

**Prioridade:** Must Have

---

### RF-013 — Controle de acesso por papéis configuráveis, escopados por projeto

**Como** administrador, **quero** que o acesso às ações do sistema seja controlado por papéis configuráveis por projeto, **para** garantir que cada usuário só execute ações compatíveis com sua responsabilidade.

**Critérios de aceite:**

**Dado que** um usuário possui um papel específico em um projeto
**Quando** ele tenta executar uma ação administrativa ou sensível
**Então** o sistema valida a permissão no backend antes de autorizar a ação, independentemente do estado da UI (RNF-003)

**Prioridade:** Must Have

---

### RF-014 — Login via SSO (Keycloak)

**Como** usuário do sistema, **quero** autenticar via SSO (Keycloak), **para** acessar o sistema sem precisar cadastrar ou lembrar uma senha local.

**Critérios de aceite:**

**Dado que** um usuário possui credenciais válidas no Keycloak
**Quando** ele inicia o login no sistema
**Então** o sistema redireciona para o fluxo de autenticação do Keycloak e, após sucesso, estabelece a sessão do usuário sem exigir senha local

**Prioridade:** Should Have

---

### RF-015 — Associação de usuário a projeto(s)

**Como** administrador, **quero** associar usuários a um ou mais projetos com um papel definido, **para** controlar quem tem acesso a cada projeto e com quais permissões.

**Critérios de aceite:**

**Dado que** um usuário não está associado a um projeto
**Quando** um administrador o associa com um papel
**Então** o usuário passa a visualizar e operar o board do projeto conforme as permissões do papel atribuído

**Prioridade:** Must Have

---

### RF-016 — Configuração de permissões por projeto (toggles)

**Como** administrador de projeto, **quero** habilitar/desabilitar toggles de permissão específicos por projeto (ex.: `devPodeExcluirTarefa`), **para** ajustar o controle de acesso às particularidades de cada time.

**Critérios de aceite:**

**Dado que** um toggle de permissão está desabilitado em um projeto
**Quando** um usuário do papel afetado tenta executar a ação correspondente
**Então** o sistema bloqueia a ação, mesmo que o papel normalmente permitisse

**Prioridade:** Must Have

---

### RF-017 — Histórico de auditoria da tarefa

**Como** gestor ou administrador, **quero** consultar o histórico de alterações relevantes de uma tarefa, **para** ter rastreabilidade de quem fez o quê e quando.

**Critérios de aceite:**

**Dado que** uma tarefa sofre alteração de responsável, título, etapa ou impedimento
**Quando** a alteração é confirmada
**Então** o sistema registra autor, valor anterior, valor novo e data/hora no histórico de auditoria (RN-016)

**Prioridade:** Must Have

---

### RF-018 — Criar card pelo board

**Como** membro da equipe de desenvolvimento com permissão `tarefa:gerenciar`, **quero** criar um card diretamente pelo board, **para** registrar uma nova tarefa sem sair da visão do fluxo de trabalho.

**Critérios de aceite:**

**Dado que** o projeto está ativo e o usuário possui `tarefa:gerenciar`
**Quando** o usuário cria um novo card pelo board sem informar responsável ou raia
**Então** o sistema cria o card sem responsável (RN-004) e o posiciona na etapa de menor ordem, na primeira raia do projeto ou raia default global (RN-005)

**Prioridade:** Must Have

---

### RF-019 — Excluir card pelo board

**Como** membro da equipe de desenvolvimento com permissão `tarefa:gerenciar`, **quero** excluir um card pelo board, **para** remover tarefas criadas indevidamente ou que não são mais válidas.

**Critérios de aceite:**

**Dado que** o usuário possui `tarefa:gerenciar` (e, se for do papel dev, o toggle `devPodeExcluirTarefa` está habilitado)
**Quando** o usuário exclui um card em um projeto que não está finalizado
**Então** o sistema remove o card, emite o evento `TAREFA_EXCLUIDA` para os demais usuários conectados e reflete a remoção nos boards abertos em até 2 segundos (RNF-001)

**Prioridade:** Must Have

---

## 4. Requisitos Não-Funcionais

### RNF-001 — Atualização em tempo real

**Categoria:** Performance

**Métrica:** Latência de propagação de alterações entre usuários conectados

**Critério:** Alterações no board (movimentação, criação, exclusão de card, impedimento) devem ser refletidas para os demais usuários conectados em até 2 segundos, sem necessidade de refresh manual.

---

### RNF-002 — Escalabilidade horizontal sem inconsistência

**Categoria:** Confiabilidade / Escalabilidade

**Métrica:** Divergência de estado entre instâncias sob escala horizontal

**Critério:** O sistema deve operar corretamente com 1 pod e escalar para 2 ou mais pods, suportando de dezenas a centenas de usuários simultâneos, sem divergência de estado (dados ou eventos em tempo real) entre instâncias.

---

### RNF-003 — Controle de acesso por papel revalidado no backend

**Categoria:** Segurança

**Métrica:** Cobertura de revalidação backend para ações administrativas/sensíveis

**Critério:** Toda ação administrativa ou sensível deve respeitar as permissões do(s) papel(is) do usuário no projeto; toda validação exibida na UI (esconder/desabilitar elemento) deve ser revalidada no backend — nenhuma escrita pode depender exclusivamente de validação client-side.

---

### RNF-004 — Empacotamento em containers

**Categoria:** Portabilidade

**Métrica:** Executável via imagem de container orquestrável

**Critério:** O sistema deve ser empacotado e executável em containers (Docker), orquestrável em OpenShift/Kubernetes, sem dependências que impeçam a execução em ambiente containerizado padrão.

---

### RNF-005 — Responsividade desktop

**Categoria:** Usabilidade

**Métrica:** Suporte a diferentes resoluções/navegadores desktop

**Critério:** A interface deve ser responsiva para uso em desktop, funcionando corretamente nos principais navegadores desktop utilizados pela equipe.

---

## 5. Regras de Negócio

| ID | Regra | Origem |
|----|-------|--------|
| RN-001 | Lead-time de uma etapa conta da entrada até a saída da tarefa naquela etapa. | kanban-configuravel |
| RN-002 | Tempo marcado como impedida é registrado separadamente, somado ao lead-time de impedimento da etapa e ao total, exibido no dashboard. | kanban-configuravel |
| RN-003 | Toda etapa deve ter ao menos uma transição de saída configurada, exceto a etapa final. | kanban-configuravel |
| RN-004 | Etapa final não tem transição de saída padrão, mas permite "desfinalizar" (retorna a tarefa a outra etapa). | kanban-configuravel |
| RN-005 | Não é permitido excluir projeto, workflow, coluna ou raia com tarefas ativas vinculadas — exige migração antes. | kanban-configuravel |
| RN-011 | Mover PARA etapa final ou "desfinalizar" exige permissão `tarefa:finalizar` (product_owner/project_admin/admin por padrão; dev não tem por padrão). | kanban-configuravel |
| RN-012 | Qualquer dev pode autoatribuir ("puxar") uma tarefa a qualquer momento, mesmo já atribuída a outro; dev não atribui a terceiros, só a si. product_owner/project_admin/admin atribuem/reatribuem livremente. Toda troca de responsável vai para auditoria (RF-017). | kanban-configuravel |
| RN-013 | Marcar/desmarcar impedimento é permitido por padrão a dev e product_owner (+ project_admin/admin); gestor não marca por padrão. | kanban-configuravel |
| RN-015 | Projeto finalizado fica somente leitura para todos, inclusive admin/project_admin, até reabertura. | kanban-configuravel |
| RN-016 | Toda alteração relevante (responsável, título, etapa, impedimento) é registrada em auditoria com autor, valor anterior/novo e data/hora. | kanban-configuravel |
| RN-CB-001 | Criar/excluir card exigem permissão `tarefa:gerenciar` no projeto. | criacao-card-board |
| RN-CB-002 | Exclusão por usuário do papel dev exige adicionalmente o toggle `devPodeExcluirTarefa` habilitado no projeto. | criacao-card-board |
| RN-CB-003 | Criação e exclusão de card são bloqueadas se o projeto estiver finalizado. | criacao-card-board |
| RN-CB-004 | Card é criado sem responsável quando não informado na criação. | criacao-card-board |
| RN-CB-005 | Card é criado, por padrão, na etapa de menor ordem, na primeira raia do projeto (ou raia default global, se não houver raia específica). | criacao-card-board |

---

## 6. Casos de Uso

### UC-001 — Criar tarefa pelo board e sinalizar impedimento

**Ator:** Membro da equipe de desenvolvimento (papel dev)
**Fluxo principal:**
1. Usuário abre o board do projeto e clica em "Novo card" na etapa de menor ordem
2. Sistema cria o card sem responsável, na primeira raia do projeto (RN-CB-004, RN-CB-005)
3. Usuário se autoatribui à tarefa (RN-012)
4. Usuário move a tarefa entre etapas conforme avança no trabalho
5. Usuário identifica um bloqueio e marca a tarefa como impedida (RF-004)
6. Sistema notifica observadores da tarefa (RF-005) e inicia contagem de lead-time de impedimento (RN-002)

**Fluxo alternativo:** Se o toggle `devPodeExcluirTarefa` estiver desabilitado e o usuário tentar excluir o card, o sistema bloqueia a ação (RN-CB-002).

---

### UC-002 — Gestor acompanha lead-time via dashboard

**Ator:** Gestor de outro time
**Fluxo principal:**
1. Gestor acessa o dashboard do projeto (sem necessidade de papel de execução)
2. Sistema exibe lead-time médio por etapa e tempo médio de impedimento agregado (RF-006, RF-007)
3. Gestor identifica etapa com maior tempo médio e aciona o time responsável fora do sistema

**Fluxo alternativo:** Se o projeto estiver finalizado, o dashboard permanece acessível em modo somente leitura (RN-015).

---

### UC-003 — Administrador finaliza projeto

**Ator:** Administrador de projeto
**Fluxo principal:**
1. Administrador acessa a configuração do projeto e aciona "Finalizar projeto"
2. Sistema valida que a ação é permitida (papel com `tarefa:finalizar` e regras de projeto)
3. Sistema marca o projeto como finalizado e torna-o somente leitura para todos os usuários, inclusive admin/project_admin (RN-015)

**Fluxo alternativo:** Administrador reabre o projeto posteriormente, restaurando a capacidade de edição (RF-008).

---

## 7. Restrições e Premissas

**Restrições:**
- Notificações são internas ao sistema — sem integração com email, Slack ou outros canais externos.
- Sistema não controla horas/timesheet do desenvolvedor.
- Sistema não suporta múltiplas organizações/clientes (single-tenant).
- Sistema não suporta dependência entre projetos.
- Sem importação em massa de cards, templates de card, duplicação de card ou anexos/arquivos nos cards.

**Premissas:**
- Keycloak já está disponível como provedor de identidade para o SSO (RF-014).
- Ambiente de destino suporta execução containerizada em OpenShift/Kubernetes (RNF-004).
- Times de dev e gestores já operam com noção mínima de fluxo kanban (não é necessário treinamento extensivo de conceito).

---

## 8. Dependências

| Dependência | Tipo | Impacto |
|-------------|------|---------|
| Servidor Keycloak configurado e acessível | Técnica | Bloqueia RF-014 (login SSO) se indisponível |
| Infraestrutura de containers (Docker/OpenShift/Kubernetes) disponível | Técnica | Bloqueia RNF-004 e RNF-002 (escalabilidade horizontal) |
| Mecanismo de comunicação em tempo real entre instâncias (para RNF-001/RNF-002 com múltiplos pods) | Técnica | Impacta RF-005, RF-019 e RNF-001 sob escala horizontal — detalhar em TechSpec |

---

## 9. Critérios de Sucesso (KPIs)

| KPI | Meta | Prazo |
|-----|------|-------|
| Redução de tempo de tarefas paradas por impedimento não visto | Qualitativa — sem meta numérica definida (ver discovery) | — |
| Redução de comunicação dispersa sobre status/impedimentos | Qualitativa — sem meta numérica definida (ver discovery) | — |
| Visibilidade de andamento e lead-time para gestores via dashboard | Qualitativa — sem meta numérica definida (ver discovery) | — |

---

## 10. Fora do Escopo

- Integração com sistemas externos de notificação (email, Slack etc.) — notificações são apenas internas.
- Controle de horas/timesheet do desenvolvedor.
- Suporte a múltiplas organizações/clientes (multi-tenant).
- Dependência entre projetos.
- Importação em massa de cards.
- Templates de card.
- Duplicar card.
- Anexos/arquivos em cards.

---

## 11. Histórico de Revisões

| Versão | Data | Autor | Alteração |
|--------|------|-------|-----------|
| 1.0 | 2026-08-24 | Thiago Goncalves Cavalcante | Versão inicial — consolidação de kanban-configuravel e criacao-card-board |

---

### Design Brief (verbatim) — `requirements/design/kanban-tarefas-design-brief.md`

# Design Brief — Kanban de Tarefas
_Versão: 1.0 | Status: Draft | Data: 2026-08-25 | Autor: Thiago Goncalves Cavalcante_

> Referência: [docs/prd/kanban-tarefas-prd.md](../prd/kanban-tarefas-prd.md) v1.0

---

## 1. Contexto e Objetivo

Sistema de kanban configurável para equipes de desenvolvimento e gestores, permitindo acompanhamento de tarefas, sinalização de impedimentos e visibilidade de lead-time sem depender de comunicação dispersa (RF-001 a RF-019). Este brief define a identidade visual e o escopo de telas do protótipo navegável que informará as decisões de arquitetura frontend do `/techspec`.

---

## 2. Identidade Visual

**Tom:** Sério/corporativo — cores sóbrias, tipografia neutra, foco em densidade de informação.

**Tema:** Light only.

**Referência visual:** Trello — cards espaçosos, cores por status/etiqueta, visual leve dentro do tom corporativo.

### Paleta

| Token | Valor | Uso |
|---|---|---|
| primary | `#0d6efd` | ação primária |
| secondary | `#198754` | ação secundária |
| background | `#f8f9fa` | fundo da página |
| surface | `#ffffff` | cards/painéis |
| error | `#dc3545` | erro |
| success | `#198754` | sucesso |
| warning | `#ffc107` | alerta / impedimento |
| textPrimary | `#212529` | texto principal |
| textSecondary | `#6c757d` | texto secundário |
| border | `#dee2e6` | bordas |
| tipoBadgeBackground | `#e7f1ff` | badge de tipo de tarefa |

### Tipografia e grid

- Fonte: **Inter** (heading e body), fallback system-ui.
- Base de espaçamento: **8px** (múltiplos: 4/8/16/24/32/48).
- Radius: 8px (cards/botões), 4px (badges).
- Breakpoints: desktop-first (RNF-005 — sem suporte mobile obrigatório). Prioritário: 1280px+; secundário: 1024px.

---

## 3. Navegação e Layout

- **Padrão:** Sidebar (navegação global: projetos, dashboard, admin) + Topbar (usuário, notificações, logout).
- Sidebar colapsável, com destaque do projeto ativo.
- Topbar com sino de notificações (RF-005) e menu de usuário.

---

## 4. Inventário de Telas

| ID | Nome | RF(s) | Persona | Rota sugerida |
|---|---|---|---|---|
| TL-01 | Login (SSO Keycloak) | RF-014 | Todos | `/login` |
| TL-02 | Lista de Projetos | RF-008 | Todos | `/projetos` |
| TL-03 | Board (colunas + raias) — variação A (cards compactos) | RF-001, RF-002, RF-011 | Dev, Gestor | `/projetos/:id/board` |
| TL-03b | Board — variação B (cards expandidos) | RF-001, RF-002, RF-011 | Dev, Gestor | `/projetos/:id/board` |
| TL-04 | Detalhe da Tarefa (modal/drawer) | RF-003, RF-004, RF-006, RF-017 | Dev, Gestor | modal sobre `/board` |
| TL-05 | Nova Tarefa (modal) | RF-018 | Dev | modal sobre `/board` |
| TL-06 | Confirmação de Exclusão de Card | RF-019 | Dev | modal sobre `/board` |
| TL-07 | Dashboard | RF-006, RF-007 | Gestor, Dev | `/projetos/:id/dashboard` |
| TL-08 | Admin de Projeto (workflow/colunas/transições) | RF-002, RF-009, RF-010 | Admin | `/projetos/:id/admin` |
| TL-09 | Admin de Papéis/Permissões | RF-013, RF-016 | Admin | `/projetos/:id/admin/papeis` |
| TL-10 | Lista de Usuários (dentro de Admin de Projeto) | RF-015 | Admin | `/projetos/:id/admin/usuarios` |

---

## 5. Fluxos de Navegação

**Happy path — UC-001 (criar tarefa e sinalizar impedimento):**
TL-02 → TL-03 → (clica "Novo card") → TL-05 → card criado → TL-03 → (abre card) → TL-04 → marca impedimento → TL-03 (indicador visual atualizado)

**Fluxo de erro crítico — RF-002 (transição bloqueada):**
TL-03 → usuário arrasta card para coluna sem transição configurada → sistema rejeita o drop, exibe toast/inline de erro, card retorna à posição original.

**Nota de interação — permissão de transição é por coluna, não por raia:** raias (swimlanes) são apenas agrupamento visual (RF-011), sem regra de transição associada. Durante o drag, o board destaca (outline verde) apenas as **colunas** com transição configurada a partir da etapa atual do card (RF-002/RN-003) e esmaece as colunas sem transição permitida — a raia de destino não é restringida.

**Fluxo de erro crítico — RF-019 (exclusão sem permissão):**
TL-03 → usuário aciona excluir card → TL-06 → toggle `devPodeExcluirTarefa` desabilitado → ação bloqueada, mensagem de erro exibida no próprio modal.

---

## 6. Estados por Tela

| Tela | idle | loading | preenchido | erro | sucesso | vazio |
|---|---|---|---|---|---|---|
| TL-01 Login | ✅ | ✅ (redirect SSO) | — | ✅ (falha auth) | ✅ (redirect ok) | — |
| TL-02 Lista de Projetos | ✅ | ✅ | ✅ | ✅ | — | ✅ (sem projetos) |
| TL-03/03b Board | ✅ | ✅ | ✅ | ✅ (transição bloqueada) | — | ✅ (coluna/raia sem cards) |
| TL-04 Detalhe da Tarefa | ✅ | ✅ | ✅ | ✅ (edição bloqueada pós-início) | ✅ (salvo) | — |
| TL-05 Nova Tarefa | ✅ | ✅ (submit) | ✅ | ✅ (validação) | ✅ (criado) | — |
| TL-06 Confirmação Exclusão | ✅ | ✅ (submit) | — | ✅ (sem permissão) | ✅ (excluído) | — |
| TL-07 Dashboard | ✅ | ✅ | ✅ | ✅ | — | ✅ (sem histórico) |
| TL-08 Admin de Projeto | ✅ | ✅ | ✅ | ✅ (validação RN-003/RN-005) | ✅ (salvo) | ✅ (sem workflows) |
| TL-09 Admin de Papéis | ✅ | ✅ | ✅ | ✅ | ✅ (salvo) | — |
| TL-10 Lista de Usuários | ✅ | ✅ | ✅ | ✅ | — | ✅ (sem usuários associados) |

---

## 7. Acessibilidade e Internacionalização

- **Nível:** WCAG AA — contraste mínimo AA em todos os tokens de cor, navegação completa por teclado, foco visível em todos os elementos interativos, labels ARIA em modais e formulários.
- **Notificações em tempo real (RF-005):** usar `aria-live="polite"` para não interromper leitura em andamento.
- **i18n:** apenas pt_BR — sem suporte multi-idioma (RNF-005, desktop-only).
- **Plataforma-alvo:** desktop, breakpoint prioritário 1280px+.

---

## 8. Decisões em Aberto

| Questão | Opções | Impacto |
|---|---|---|
| Densidade do card no board | (A) Compacto — mais cards visíveis, menos detalhe inline / (B) Expandido — badges, responsável e indicador de impedimento visíveis sem abrir o card | Afeta legibilidade em boards com muitas tarefas; decisão a ser validada com o time após revisão do protótipo (TL-03 vs TL-03b) |

---

## 9. Escopo do Protótipo

- Telas: TL-01 a TL-10 (11 arquivos HTML, incluindo as 2 variações de board).
- Estados obrigatórios: idle, loading, preenchido, erro, sucesso, vazio — conforme tabela da seção 6.
- Variações: 2 (TL-03 e TL-03b) para decisão de densidade do card.

---

## 10. Decision Records de Design

> Decisões: —

Nenhuma decisão de design system foi considerada suficientemente estrutural para gerar DDR nesta fase — paleta e tipografia foram definidas diretamente pelo usuário sem trade-offs relevantes a registrar. A escolha entre variações de card (seção 8) poderá gerar DDR após validação do protótipo.

---

## Histórico de Revisões

| Versão | Data | Autor | Alteração |
|---|---|---|---|
| 1.0 | 2026-08-25 | Thiago Goncalves Cavalcante | Versão inicial |

---

## Domain Concept Identification

> **Nota de exploração de codebase:** a exploração dirigida por conceitos foi executada sobre o diretório do projeto (`d:\DEV\Projects\SPDD_puro\poc`). O projeto contém **apenas** `requirements/` e `.claude/` — **não existe código-fonte, build file, migration, schema ou configuração de aplicação neste repositório**. Portanto, todo "conceito existente" abaixo é existente **como decisão documentada** (ADR/BDR/PRD), não como artefato de código verificável aqui. Vários ADRs referenciam artefatos de uma implementação anterior (`systems/CRUDAO/`, `backend/src/main/resources/db/migration/V2`, `V9`, `PermissaoGuard`, `UsuarioProvisioningService`, `TASK-01.1`…) que **não estão presentes** — ver *Technical Risks*.

### Existing Concepts (from codebase)

Nenhum conceito existe como código neste repositório. Os conceitos abaixo estão **especificados e decididos** nos artefatos de requisito e devem ser tratados como pré-modelados (contrato a honrar), não como liberdade de design:

- **Usuario**: identidade autenticada via Keycloak (OIDC), provisionada JIT no primeiro login, chaveada por `keycloak_sub` — possui flag `adminGlobal` (ADR-007) que concede bypass universal de RBAC escopado, exceto a trava de projeto finalizado.
- **Papel**: conjunto nomeado de permissões (`admin`, `project_admin`, `product_owner`, `dev`, `gestor`, `user` legado); `admin` é global e protegido contra criação/edição/exclusão (RN-006, citada em `security.md`/BDR-001).
- **Permissão**: capacidade granular verificável no backend (`tarefa:gerenciar`, `tarefa:finalizar`, `projeto:administrar`) — a autorização é sempre revalidada server-side (RNF-003).
- **UsuarioProjetoPapel**: associação acumulável (usuário × projeto × papel) que escopa o RBAC ao projeto (BDR-001, RF-015).
- **Projeto**: unidade de escopo de tudo (workflow, raias, papéis, board, dashboard); tem ciclo de vida ativo ↔ finalizado, onde finalizado ⇒ somente leitura para todos, inclusive admin global (RN-015).
- **Workflow**: fluxo de trabalho pertencente a um projeto, composto por etapas e transições (RF-009).
- **Etapa (coluna)**: estágio ordenado do workflow; exige ao menos uma transição de saída salvo a etapa final (RN-003); a de menor ordem é o ponto de entrada default de novos cards (RN-CB-005).
- **Transição**: par ordenado de etapas que autoriza a movimentação; ausência de transição ⇒ movimento bloqueado (RF-002).
- **Raia (swimlane)**: agrupamento **puramente visual** de tarefas dentro do board, sem semântica de transição (design brief §5).
- **Tarefa (card)**: unidade de trabalho; possui responsável opcional, etapa atual, raia, estado de impedimento e trava de edição pós-início (RF-003).
- **Impedimento**: estado temporal marcável/desmarcável na tarefa que abre e fecha períodos contabilizados separadamente (RF-004, RN-002).
- **Notificação interna**: aviso in-app entregue a observadores de uma tarefa (responsável, criador, observador explícito) em mudança de etapa ou impedimento (RF-005) — sem canais externos.
- **Auditoria de tarefa**: registro imutável de alteração relevante com autor, valor anterior, valor novo e data/hora (RF-017, RN-016).
- **Toggle de permissão por projeto**: conjunto **fechado** de chaves booleanas que modulam o comportamento default de um papel dentro de um projeto (ex.: `devPodeExcluirTarefa`) — RF-016, BDR-001.
- **Evento de board**: mensagem de mudança de estado propagada entre pods e a clientes STOMP, portando `seq` por projeto para detecção de gap (ADR-004).

### New Concepts Required

Conceitos que os requisitos exigem mas que **nenhum artefato ainda define** — precisam ser introduzidos no REASONS Canvas:

- **Observador de tarefa (explícito)**: RF-005 fala em "responsável, criador **ou observador explícito**", mas nenhum RF/RN define como um observador explícito é adicionado, removido ou listado. É um relacionamento novo (usuário × tarefa) sem CRUD especificado.
- **Período de permanência em etapa (histórico de movimentação)**: RN-001/RF-006 exigem tempo por etapa; a auditoria (RN-016) registra eventos, mas o cálculo de lead-time precisa de um conceito de intervalo fechado (entrada/saída por etapa) — decidir se é derivado da auditoria ou materializado como conceito próprio.
- **Período de impedimento**: análogo ao anterior, para RN-002 (tempo impedido por etapa e total). Requer par abertura/fechamento, não apenas um booleano na tarefa.
- **Agregado de lead-time do projeto**: RF-007 exige médias por etapa no dashboard — conceito de leitura/derivado, com política de atualização (on-demand vs. assíncrona) ainda em aberto.
- **Tipo e prioridade de tarefa**: aparecem no design brief/screen map/protótipos (badges de tipo, `tipoBadgeBackground`, badge de prioridade em TL-03b, campo "tipo" em TL-05) mas **não existem em nenhum RF ou RN** do PRD. Novo conceito de origem exclusivamente de design.
- **Sessão/perfil corrente com permissões efetivas por projeto**: BDR-001 registra a necessidade de um endpoint "permissões do usuário atual no projeto" para o frontend renderizar UI condicional — conceito de projeção, não de persistência.
- **Sequência de eventos por projeto (`seq`) e ponto de resincronização**: ADR-004 exige contador por `projetoId` e um endpoint de snapshot de board para resync — conceito operacional novo.

### Key Business Rules

- **Transição válida** (RF-002, RN-003): mover tarefa exige transição configurada da etapa atual para a destino. Governa: Tarefa, Etapa, Transição. Vale igualmente para drag-and-drop e para o menu do card (DDR-002) — validação única no backend.
- **Etapa final e desfinalizar** (RN-004, RN-011, RF-012): etapa final não tem saída padrão; retorno exige permissão `tarefa:finalizar` (também exigida para *entrar* na etapa final). Governa: Tarefa, Etapa, Permissão.
- **Congelamento pós-início** (RF-003): saindo da primeira etapa, campos estruturais tornam-se imutáveis; permanecem editáveis responsável, status e impedimento. Governa: Tarefa.
- **Autoatribuição livre** (RN-012): qualquer dev pode puxar qualquer tarefa, mesmo já atribuída; dev não atribui a terceiros; papéis administrativos atribuem livremente; toda troca vai para auditoria. Governa: Tarefa, Usuario, Papel, Auditoria.
- **Impedimento é ortogonal à movimentação** (RF-004, DDR-002): impedir não bloqueia nem libera transição. Governa: Tarefa, Impedimento, Transição.
- **Contabilização separada de impedimento** (RN-001, RN-002): tempo impedido soma ao lead-time de impedimento da etapa e ao total, exibido no dashboard. Governa: Período de etapa, Período de impedimento, Agregado.
- **Marcação de impedimento por papel** (RN-013): dev e product_owner (+ project_admin/admin) por padrão; gestor não. Governa: Impedimento, Papel.
- **Integridade referencial de configuração** (RN-005): projeto, workflow, coluna ou raia com tarefas ativas vinculadas não podem ser excluídos — exige migração prévia. Governa: Projeto, Workflow, Etapa, Raia, Tarefa.
- **Projeto finalizado é read-only universal** (RN-015): inclusive para admin/project_admin e admin global (ADR-007); dashboard permanece legível (UC-002). Governa: Projeto e toda escrita descendente.
- **Criação/exclusão de card** (RN-CB-001..005): exigem `tarefa:gerenciar`; exclusão por dev exige adicionalmente o toggle `devPodeExcluirTarefa`; ambas bloqueadas em projeto finalizado; card nasce sem responsável, na etapa de menor ordem e na primeira raia (ou raia default global). Governa: Tarefa, Permissão, Toggle, Etapa, Raia.
- **Auditoria obrigatória** (RN-016, RF-017): alteração de responsável, título, etapa ou impedimento gera registro com autor, antes/depois e timestamp. Governa: Tarefa, Auditoria.
- **RBAC escopado e acumulável** (BDR-001, RF-013, RF-015): permissões são a união dos papéis do usuário **naquele** projeto; `admin` global é transversal e protegido (RN-006); `user` é fallback sem permissões (RN-014). Governa: Usuario, Papel, Projeto.
- **Autorização nunca depende do cliente** (RNF-003): toda regra que a UI aplica escondendo/desabilitando elemento é revalidada no backend, e o projeto de escopo é derivado do recurso, não de parâmetro enviado pelo cliente. Governa: toda escrita.

---

## Strategic Approach

### Solution Direction

- **Sistema novo, greenfield neste repositório**, entregue como duas aplicações containerizadas (ADR-008): backend Spring Boot 3.5.16 / Java 25 expondo REST + WebSocket STOMP, e frontend Next.js consumindo ambos; PostgreSQL como fonte única de verdade; Keycloak como IdP externo.
- **Camadas conforme `architecture.md`**: Controller (REST + STOMP) → Service (regras de workflow, lead-time, permissão) → Repository (Spring Data JPA) → entidades, com MapStruct para entidade↔DTO e Bean Validation na borda. Nenhum estado de negócio em memória de pod (RNF-002).
- **Fluxo de escrita canônico**: requisição autenticada (OIDC) → resolução do usuário e do projeto a partir do recurso → guard de permissão escopada + guard de projeto ativo → regra de domínio (transição/trava/toggle) → persistência + registro de auditoria na mesma transação → publicação do evento de board em `afterCommit` via porta `EventoBoardPublisher` → `NOTIFY` → cada pod retransmite ao tópico STOMP do projeto → frontend aplica delta e valida `seq`.
- **Fluxo de leitura do board**: um endpoint de snapshot por projeto (colunas + raias + cards + estado de impedimento) serve tanto a carga inicial quanto a resincronização após gap de `seq` ou reconexão (ADR-004) — o cliente nunca reconstrói estado só a partir de eventos.
- **Lead-time como derivação de um histórico temporal persistido**, não como campo mutável na tarefa: a direção é registrar intervalos de permanência em etapa e intervalos de impedimento, e calcular tanto o detalhe da tarefa (RF-006) quanto as médias do dashboard (RF-007) a partir dessa mesma base — uma fonte, dois consumidores.
- **Autorização centralizada em um guard declarativo** (estilo `@ExigePermissao` citado em BDR-001) que combina três checagens: permissão do papel acumulado no projeto, toggle do projeto quando aplicável, e projeto não finalizado. Bypass de admin global no primeiro eixo, nunca no terceiro (ADR-007).
- **Frontend orientado às 11 telas do screen map**, com estados obrigatórios (idle/loading/preenchido/erro/sucesso/vazio) e UI condicional alimentada por um endpoint de permissões efetivas do usuário no projeto — UI como conveniência, backend como autoridade.

### Key Design Decisions

- **Origem do lead-time: derivar da auditoria vs. tabela dedicada de intervalos** — trade-off: derivar evita duplicação e mantém uma verdade só, mas acopla o cálculo ao formato de um log de auditoria genérico e força varredura/ordenação a cada leitura; tabela dedicada custa uma escrita a mais por movimentação e um invariante de consistência entre as duas trilhas. → **Recomendação: tabela dedicada de intervalos (permanência em etapa e impedimento), com a auditoria permanecendo como log genérico de RF-017.** Justificativa: RN-001/RN-002 exigem agregação por etapa em nível de projeto (RF-007) e a auditoria é um log heterogêneo de eventos, não um modelo temporal — derivar tornaria o dashboard caro e frágil a qualquer mudança no formato do log.
- **Cálculo do dashboard: sob demanda vs. assíncrono/materializado** — trade-off: sob demanda é simples e sempre fresco, mas o custo cresce com o histórico do projeto; assíncrono garante latência estável mas introduz job, estado intermediário e um padrão de loading próprio (DDR-003 já pressupõe "job assíncrono do dashboard"). → **Recomendação: começar sob demanda com filtro de período, e tratar materialização como evolução condicionada a volume medido.** Justificativa: ADR-002 impõe simplicidade deliberada nesta fase e não há meta numérica de performance para o dashboard no PRD (RNF-001 cobre apenas o board); a UI de skeleton prevista em DDR-003 acomoda ambas as implementações sem retrabalho visual. **Conflito a sinalizar:** DDR-003 assume o dashboard assíncrono como fato — precisa ser reconciliado explicitamente.
- **Escopo de projeto na autorização: derivado do recurso vs. informado na requisição** — trade-off: derivar exige uma resolução (carregar tarefa → etapa → workflow → projeto) antes do guard; informar é barato mas é exatamente o vetor que RNF-003 proíbe. → **Recomendação: sempre derivar do recurso.** Justificativa: aceitar `projetoId` do cliente permitiria escalar privilégio de um projeto para outro.
- **Trava de projeto finalizado: guard transversal vs. checagem por caso de uso** — trade-off: transversal é uniforme e não esquece endpoints novos, mas exige classificar leitura vs. escrita corretamente (o dashboard e o board precisam continuar legíveis). → **Recomendação: guard transversal em toda operação de escrita, sem exceção de papel.** Justificativa: RN-015 é explícita em não abrir exceção nem para admin.
- **Modelo de permissão: catálogo fechado de papéis + toggles vs. RBAC totalmente configurável** — já decidido em BDR-001 a favor do catálogo fechado com toggles. → **Recomendação: manter, e tratar RF-013 ("papéis configuráveis") como configuração de associação usuário↔papel↔projeto e dos toggles, não como criação livre de papéis/permissões.** Justificativa: BDR-001 é explícito, mas o texto de RF-013 e o ADR-003 ("admin cria/edita papéis em tempo de operação") sugerem o contrário — ver ambiguidade A-1.
- **Entrega de notificações por usuário em multi-pod** — trade-off: `convertAndSendToUser` isolado por pod não entrega quando o usuário está em outro pod; alternativas são reaproveitar o canal `board_events` para rotear por usuário ou introduzir broker relay (proibido por ADR-002). → **Recomendação: reaproveitar o canal único `LISTEN/NOTIFY`, com o payload carregando os destinatários e cada pod filtrando suas próprias sessões.** Justificativa: única opção compatível com ADR-002/ADR-004; o próprio ADR-004 registra isso como pendência a detalhar.
- **Reordenação de etapas e migração de tarefas** — RF-010 permite reordenar colunas e RN-005 bloqueia exclusão com tarefas ativas, mas nada define o efeito da reordenação sobre transições já configuradas e sobre a "etapa de menor ordem" (default de criação). → **Recomendação: tratar ordem como atributo puramente de apresentação e ordenação de default, mantendo transições como grafo explícito independente da ordem.** Justificativa: evita invalidação silenciosa de workflow ao arrastar uma coluna.

### Alternatives Considered

- **Redis Pub/Sub ou broker dedicado para tempo real**: rejeitado por ADR-002/ADR-004 nesta fase — a decisão é validar o produto antes de adicionar infraestrutura; `LISTEN/NOTIFY` cobre a escala prevista (dezenas a centenas de usuários).
- **Papéis e permissões inteiramente no Keycloak**: rejeitado por ADR-003 — exigiria acesso administrativo ao IdP para cada mudança de permissão.
- **Fallback de autenticação local**: rejeitado por ADR-006, superando o texto ainda desatualizado de `security.md`.
- **`ddl-auto=update` do Hibernate em vez de migrations**: rejeitado por ADR-005 — inaceitável com múltiplas instâncias.
- **Papel único global por usuário**: rejeitado por BDR-001 — não representa atuação multi-projeto.
- **Somente drag-and-drop (sem menu) no board**: rejeitado por DDR-002 — o menu cobre acessibilidade motora, boards densos e a ação de desfinalizar.
- **Polling do board como mecanismo de atualização**: não considerado nos artefatos, e incompatível com RNF-001 (<2s) a custo razoável — o STOMP já é decisão firmada; o polling permanece apenas como rede de segurança de resync.

---

## Risk & Gap Analysis

### Requirement Ambiguities

- **A-1 — "Papéis configuráveis" (RF-013 e ADR-003) vs. catálogo fechado (BDR-001)**: RF-013 e ADR-003 falam em admin criando/editando papéis e permissões em tempo de operação; BDR-001 fixa um conjunto fechado de papéis e resolve variação por toggles. Precisa decidir se o CRUD de papéis/permissões existe.
- **A-2 — RN ausentes no PRD**: `security.md` e BDR-001 citam **RN-006** (papel `admin` protegido) e **RN-014** (papel `user` sem permissões) como regras vigentes, mas a tabela de regras do PRD v1.0 salta de RN-005 para RN-011 — RN-006 a RN-010 e RN-014 não existem no documento. Falta o texto normativo dessas regras.
- **A-3 — "Campos estruturais" na trava pós-início (RF-003)**: o PRD exemplifica ("descrição de escopo") e lista os editáveis ("responsável, status, impedimento") sem fechar a lista. Título é editável pós-início? RN-016 audita alteração de título, o que sugere que sim — conflitando com a leitura de que título é estrutural.
- **A-4 — Definição de "tarefa iniciada"**: RF-003 define como "saiu da primeira etapa". Se a tarefa retornar à primeira etapa por uma transição de volta, ela "desinicia" e destrava a edição? Não especificado.
- **A-5 — Observador explícito (RF-005)**: como se torna observador? Não há RF de "seguir tarefa", nem tela no screen map, nem componente no protótipo.
- **A-6 — Permissão para *entrar* na etapa final**: RN-011 exige `tarefa:finalizar` para mover **para** a etapa final e para desfinalizar; RF-012 só descreve a ação de desfinalizar. O board (RF-002/DDR-002) precisa refletir isso no destaque de colunas válidas durante o drag — não especificado.
- **A-7 — "Raia default global" (RN-CB-005)**: o conceito é citado como fallback quando não há raia específica, mas nada define quem a cria, se é por projeto ou realmente global, nem se um projeto pode existir sem nenhuma raia.
- **A-8 — Tipo e prioridade de tarefa**: presentes no design (TL-03b, TL-05, token `tipoBadgeBackground`) e ausentes de todo RF/RN. São requisito ou licença visual do protótipo?
- **A-9 — Múltiplos workflows por projeto (RF-009, plural) vs. board único por projeto (RF-001, `/projetos/:id/board`)**: se um projeto pode ter vários workflows, qual deles o board renderiza, e a que workflow uma tarefa pertence?
- **A-10 — Acessibilidade contraditória**: DDR-003 declara WCAG não obrigatório nesta fase; o design brief §7 e `design-tokens.json` declaram WCAG AA com contraste mínimo, foco visível e `aria-live`. Duas normas incompatíveis vigentes.
- **A-11 — Tipografia contraditória**: DDR-001 fixa **Roboto** como fonte única; o design brief §2 e os tokens fixam **Inter**.
- **A-12 — Fallback de autenticação**: `security.md` ainda prevê fallback local; ADR-006 o rejeita. O ADR é posterior e deve prevalecer, mas a guideline não foi atualizada.
- **A-13 — Escopo de `gestor`**: UC-002 diz que o gestor acessa o dashboard "sem necessidade de papel de execução", e RN-013 diz que gestor não marca impedimento — mas não há regra definindo se gestor enxerga o board, e RF-016 cita "visibilidade do board para gestor" (via BDR-001) como caso resolvido por toggle, sem que esse toggle esteja nomeado em lugar nenhum.
- **A-14 — Densidade do card (design brief §8)**: decisão aberta entre TL-03 (compacto) e TL-03b (expandido), pendente de validação com o time — bloqueia a especificação final do componente de card.

### Edge Cases

- **Movimentação concorrente do mesmo card**: dois usuários arrastam o mesmo card para colunas diferentes quase simultaneamente — sem controle de concorrência, o segundo write pode validar a transição contra uma etapa já desatualizada e gerar intervalos de lead-time sobrepostos ou invertidos.
- **Autoatribuição sobre tarefa já atribuída** (RN-012): dois devs puxando a mesma tarefa em sequência rápida; o histórico de auditoria precisa refletir a ordem real, e o notificado precisa ser o responsável correto.
- **Impedimento aberto durante movimentação de etapa**: a tarefa é movida enquanto impedida — o período de impedimento deve ser fechado e reaberto na nova etapa, ou continuar como um único intervalo transversal? RN-002 fala em impedimento "da etapa" **e** total, o que exige uma resposta explícita.
- **Impedimento aberto quando a tarefa é finalizada ou excluída**: intervalo sem fechamento contamina qualquer média do dashboard.
- **Projeto finalizado com impedimentos e etapas abertos**: os relógios continuam correndo durante o período de somente-leitura? Afeta diretamente RF-007.
- **Reabertura de projeto** (RF-008): retomar a contagem de lead-time do ponto anterior ou descontar o intervalo de finalização?
- **Desfinalizar para qual etapa** (RN-004/RF-012): "retorna a tarefa para a etapa selecionada" — a seleção é livre entre todas as etapas ou restrita? Se livre, contorna o grafo de transições de RF-002.
- **Exclusão de raia/coluna com tarefas apenas finalizadas**: RN-005 fala em "tarefas ativas" — tarefa na etapa final conta como ativa? Determina se um projeto maduro consegue ou não reconfigurar seu board.
- **Tarefa sem responsável** (RN-CB-004) combinada com RF-005: sem responsável e sem observador explícito, o criador é o único notificado — e se o criador for removido do projeto?
- **Usuário perde o papel enquanto tem o board aberto**: a sessão STOMP já inscrita continua recebendo eventos de um projeto ao qual não tem mais acesso.
- **Gap de `seq` durante reconexão** (ADR-004): coberto pelo resync, mas o comportamento sob perda de `NOTIFY` **sem** gap detectável (evento perdido sendo o último) não está tratado.
- **Payload de 8KB do `NOTIFY`** (ADR-004): eventos em lote (ex.: reordenação de colunas afetando muitos cards) podem estourar o limite.
- **Board vazio / projeto sem workflow ou sem etapas**: o screen map prevê estado vazio, mas RN-CB-005 pressupõe uma "etapa de menor ordem" existente para criar card.
- **Primeiro login e bootstrap** (ADR-007): se `KANBAN_BOOTSTRAP_ADMIN_EMAIL` não estiver setada ou o e-mail nunca logar, o sistema fica sem ninguém capaz de criar o primeiro projeto.
- **Keycloak indisponível** (ADR-006): sem fallback, o sistema inteiro fica inacessível — inclusive para leitura de dashboard por gestores.

### Technical Risks

- **R-1 — Descompasso entre os artefatos e o repositório**: ADRs 004/005/007 e BDR-001 descrevem uma implementação já existente (migrations V2/V9, `PermissaoGuard`, `UsuarioProvisioningService`, `EventoBoardPublisher`, `TASK-01.1`, `systems/CRUDAO/docker-compose.yml`, `keycloak/realm-export.json`) que **não está neste repositório**. Impacto: o REASONS Canvas pode gerar operações assumindo código inexistente, ou duplicar o que existe em outro repositório. Mitigação: confirmar com o usuário se a implementação será do zero aqui ou se há um repositório de código a ser referenciado antes do `/spdd-generate`.
- **R-2 — Concorrência no board**: múltiplos usuários movendo/editando o mesmo card, com escrita + auditoria + intervalo de lead-time + evento em uma mesma transação. Mitigação: versionamento otimista na tarefa e revalidação da etapa de origem dentro da transação, com erro de conflito propagado à UI como retorno do card à posição original (fluxo já previsto no design brief §5).
- **R-3 — Garantia de entrega do `LISTEN/NOTIFY`**: `NOTIFY` não persiste nem faz replay; queda do listener JDBC perde eventos silenciosamente. Mitigação já decidida (ADR-004): `seq` por projeto + resync via snapshot + retry/backoff com readiness refletindo o estado da conexão. Risco residual: perda do último evento sem gap detectável — mitigar com heartbeat ou resync na volta do foco da aba.
- **R-4 — Roteamento de notificação por usuário em multi-pod**: registrado como pendência no próprio ADR-004; sem solução, RF-005 falha silenciosamente com 2+ pods.
- **R-5 — Custo do dashboard sob histórico crescente**: agregação por etapa sobre todos os intervalos do projeto, sem cache (ADR-002) e sem meta de performance definida. Mitigação: filtro de período obrigatório na consulta e índices sobre (projeto, etapa, intervalo).
- **R-6 — Integridade temporal dos intervalos**: todo cálculo de RN-001/RN-002 depende de intervalos sempre fechados na ordem correta; um evento perdido, um rollback parcial ou um `updated_at` gerado no pod (relógios distintos entre pods) corrompem as médias de forma difícil de detectar. Mitigação: carimbar tempo sempre no banco (`now()` transacional), nunca no pod, e tratar intervalo aberto como invariante verificável.
- **R-7 — Superfície de revalidação de RNF-003**: cada endpoint precisa de três guards (permissão escopada, toggle, projeto ativo); o risco é o esquecimento pontual em endpoints novos. Mitigação: guard declarativo aplicado por default com opt-out explícito, e teste de arquitetura que falhe para endpoint de escrita sem anotação.
- **R-8 — Versões fixadas e frágeis da stack** (`stack.md`): Java 25 + Spring Boot 3.5.16 + Lombok 1.18.46 + Spotless 3.10.0 foram fixados por incompatibilidade concreta de bytecode/`javac`. Qualquer upgrade fora desse conjunto quebra o build de forma não óbvia.
- **R-9 — Testes dependentes de Keycloak em pé** (`testing.md`): `@SpringBootTest` resolve o issuer OIDC eagerly, exigindo Keycloak local. Impacta a meta de 80% TDD / 100% BDD e a execução em qualquer ambiente sem Docker. Mitigação: isolar a configuração de segurança em testes de slice e usar issuer stub.
- **R-10 — Armadilha de nomenclatura JavaBeans** (`coding-standards.md`): campos boolean como `eFinal` quebram Jackson/MapStruct silenciosamente. Aplicável diretamente aos conceitos previstos aqui (`etapaFinal`, `adminGlobal`, `devPodeExcluirTarefa`).
- **R-11 — Acoplamento total ao Keycloak** (ADR-006): indisponibilidade do IdP derruba o sistema inteiro; aceito como risco de infraestrutura, mas sem plano de degradação para leitura.
- **R-12 — Divergência entre normas de design vigentes** (A-10/A-11): implementar tokens conforme o brief contradiz DDR-001/DDR-003; implementar conforme os DDRs contradiz os protótipos entregues.

### Acceptance Criteria Coverage

| AC# | Description | Addressable? | Gaps/Notes |
|-----|-------------|--------------|------------|
| RF-001 | Board exibe colunas na ordem configurada com tarefas na etapa correspondente | Yes | Endpoint de snapshot + render por coluna/raia; depende de resolver A-9 (qual workflow o board renderiza) |
| RF-002 | Movimentação sem transição configurada é bloqueada e informada | Yes | Validação única no backend servindo drag e menu (DDR-002); destaque de colunas válidas precisa considerar A-6 |
| RF-003 | Campos estruturais travados pós-início; editáveis permanecem editáveis | Partial | Lista de campos estruturais não é fechada (A-3) e "desiniciar" ao voltar à primeira etapa não é definido (A-4) |
| RF-004 | Marcar impedida registra início do período, exibe indicador e inicia contagem | Yes | Requer o conceito novo de período de impedimento; comportamento na troca de etapa com impedimento aberto em aberto (edge case) |
| RF-005 | Observadores recebem notificação interna em mudança de etapa/impedimento | Partial | "Observador explícito" não tem CRUD nem tela (A-5); entrega multi-pod não resolvida (R-4) |
| RF-006 | Detalhe da tarefa exibe tempo por etapa e total de impedimento | Yes | Depende da decisão por tabela dedicada de intervalos; RN-002 ambígua quanto a impedimento atravessando etapas |
| RF-007 | Dashboard exibe lead-time médio por etapa incluindo impedimento agregado | Partial | Sem definição de janela/período default, de política de atualização (sob demanda vs. assíncrono, conflito com DDR-003) e de tratamento de intervalos abertos |
| RF-008 | Finalizar projeto o torna somente leitura para todos até reabertura | Yes | Guard transversal de escrita; efeito da finalização sobre relógios de lead-time em aberto (edge case) |
| RF-009 | Excluir workflow sem tarefas ativas; bloquear se houver | Partial | "Tarefa ativa" não é definida (tarefa finalizada conta?); multiplicidade de workflows por projeto em aberto (A-9) |
| RF-010 | Coluna exige ao menos uma transição de saída, exceto a final | Yes | Precisa de decisão sobre efeito da reordenação no grafo de transições (ver Key Design Decisions) |
| RF-011 | Tarefas agrupadas visualmente pelas raias configuradas | Yes | Raia é só visual (brief §5); "raia default global" indefinida (A-7) |
| RF-012 | Desfinalizar retorna a tarefa à etapa selecionada com `tarefa:finalizar` | Partial | Conjunto de etapas elegíveis para retorno não definido — pode contornar o grafo de transições (edge case) |
| RF-013 | Permissão validada no backend independentemente do estado da UI | Yes | Cobertura garantida por guard declarativo (R-7); mas o alcance de "papéis configuráveis" está em disputa (A-1) |
| RF-014 | Login redireciona ao Keycloak e estabelece sessão sem senha local | Yes | Should Have; sem fallback (ADR-006); depende de realm provisionado |
| RF-015 | Associar usuário a projeto com papel concede acesso conforme o papel | Yes | Modelo (usuário × projeto × papel) acumulável de BDR-001; TL-10 já mapeada |
| RF-016 | Toggle desabilitado bloqueia a ação mesmo com papel permissivo | Partial | Só `devPodeExcluirTarefa` está nomeado; o conjunto fechado de toggles não está enumerado (relacionado a A-13) |
| RF-017 | Alteração de responsável/título/etapa/impedimento registra autor, antes/depois, timestamp | Yes | Log genérico separado dos intervalos de lead-time; sem requisito de retenção ou de exposição paginada definido |
| RF-018 | Card criado sem responsável na etapa de menor ordem e primeira raia | Partial | Depende de "raia default global" (A-7) e do caso de projeto sem etapas/raias (edge case) |
| RF-019 | Exclusão remove card, emite `TAREFA_EXCLUIDA` e reflete em até 2s | Yes | Nome de evento já fixado pelo PRD; latência coberta por ADR-004, sujeita a R-3 |
| RNF-001 | Propagação de alterações do board em até 2s | Yes | STOMP + `LISTEN/NOTIFY`; sem plano de medição definido para comprovar o limiar |
| RNF-002 | Operar com 1 pod e escalar para 2+ sem divergência | Partial | Board coberto; notificações por usuário em multi-pod não resolvidas (R-4) |
| RNF-003 | Toda ação sensível revalidada no backend | Yes | Guard declarativo + teste de arquitetura (R-7) |
| RNF-004 | Empacotamento em containers orquestráveis | Yes | ADR-008 define Dockerfiles e compose; manifests OpenShift/K8s fora de escopo |
| RNF-005 | Responsividade desktop nos navegadores da equipe | Yes | Desktop-first 1280px/1024px; lista de navegadores-alvo não é explícita |
