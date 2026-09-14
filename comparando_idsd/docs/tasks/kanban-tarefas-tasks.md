# Tasks — kanban-tarefas
_Data: 2026-09-09 | Autor: Thiago Goncalves Cavalcante_
_PRD: docs/prd/kanban-tarefas-prd.md (v1.3, congelado)_
_TechSpec: docs/techspec/kanban-tarefas-techspec.md (v1.5)_
_Cenários congelados: docs/prd/kanban-tarefas/*.feature_

> Este documento é o plano de execução. Ele não redefine requisito, não decide
> arquitetura e não escreve teste — só distribui, ordena e instrui.
> A camada de User Story não existe: o cenário Gherkin já é a narrativa e já
> está congelado como contrato.

**Sistema único.** O workspace declara um sistema, `idsd`, e todo caminho de
arquivo neste documento é relativo à raiz dele, na árvore que o quickstart da
TechSpec fixa: `backend/`, `frontend/` e `docker/`. Não há segundo sistema, e
por isso não há tabela de sequenciamento entre sistemas.

**Orçamento do laço agêntico.** O flow `feature.yaml` não declara orçamento
próprio de tentativas. Vale o padrão desta entrega: **3 tentativas** por task de
executor `agente` ou `misto`. Esgotado o orçamento, a task para e o registro vai
para o histórico dela — não se aumenta o orçamento no meio da execução.

---

## Princípios para tasks prontas para IA

Uma task está pronta quando quem a executa **não precisa abrir o PRD nem a
TechSpec para saber o que fazer**. Isso implica cópia integral, não referência:

- **Auto-contida.** Todo dado necessário está no arquivo da task — assinatura de
  método, nome de campo, mensagem de erro literal, modo de arredondamento.
  Remeter a uma seção de outro documento não é instrução, é adiamento.
- **Um resultado verificável.** Se a task tem dois resultados independentes, são
  duas tasks.
- **Sem decisão em aberto.** Task que começa com uma escolha a fazer é uma
  lacuna do `/techspec` que escapou do `/analyze` — devolva em vez de executar.
- **Escopo de arquivo declarado.** O que pode ser criado, o que pode ser
  alterado, e o que é proibido tocar.

---

## Sumário de épicos

| ID | Épico | Sistema | Cenários entregues | Tasks | Executor | Pode iniciar |
| --- | --- | --- | --- | --- | --- | --- |
| EPIC-01 | Acesso, sessão e projetos visíveis | idsd | SCN-001.1, SCN-001.2, SCN-001.3, SCN-002.1, SCN-002.2, SCN-002.3, SCN-021.1, SCN-021.2, SCN-021.3, SCN-022.1, SCN-022.2 | 8 | misto | imediatamente |
| EPIC-02 | Fluxo de etapas, criação de tarefa e board | idsd | SCN-002.4, SCN-017.1, SCN-017.2, SCN-017.3, SCN-004.1, SCN-004.2, SCN-004.3, SCN-022.3, SCN-003.1, SCN-003.2 | 8 | misto | após EPIC-01 |
| EPIC-03 | Handoff: mover, assumir e devolver | idsd | SCN-005.1, SCN-005.2, SCN-005.3, SCN-006.1, SCN-006.2, SCN-007.1, SCN-007.2, SCN-007.3, SCN-008.1, SCN-008.2 | 5 | misto | após EPIC-02 |
| EPIC-04 | Impedimento como terceira dimensão | idsd | SCN-009.1, SCN-009.2, SCN-009.3, SCN-010.1, SCN-010.2, SCN-010.3, SCN-003.3, SCN-006.3, SCN-007.4, SCN-008.3 | 4 | misto | após EPIC-03 |
| EPIC-05 | Desfechos: conclusão, encerramento e reabertura | idsd | SCN-011.1, SCN-011.2, SCN-011.3, SCN-012.1, SCN-012.2, SCN-012.3, SCN-012.4, SCN-013.1, SCN-013.2, SCN-013.3 | 5 | misto | após EPIC-04 |
| EPIC-06 | Raias, participação e papéis | idsd | SCN-018.1, SCN-018.2, SCN-019.1, SCN-019.2, SCN-019.3 | 3 | misto | após EPIC-03 |
| EPIC-07 | Fila pessoal e consultas agregadas | idsd | SCN-014.1, SCN-014.2, SCN-014.3, SCN-015.1, SCN-015.2, SCN-015.3, SCN-016.1, SCN-016.2, SCN-016.3, SCN-018.3 | 6 | misto | após EPIC-05 e EPIC-06 |
| EPIC-08 | Acompanhamento em tempo real | idsd | SCN-020.1, SCN-020.2, SCN-020.3, SCN-019.4 | 4 | misto | após EPIC-07 |

**Invariante de rastreabilidade:** todo cenário do PRD é entregue por
**exatamente um** épico; todo épico entrega **pelo menos um** cenário. Cenário
órfão ou duplicado reprova o GATE-RASTREABILIDADE.

Os quatro cenários cruzados de impedimento — SCN-003.3, SCN-006.3, SCN-007.4 e
SCN-008.3 — pertencem ao EPIC-04 e não aos épicos dos requisitos que os
nomeiam. Cada um deles verifica que a **marca de impedimento sobrevive** a uma
operação de outra dimensão, e nenhum é executável antes de existir impedimento.
Deixá-los no épico de origem exigiria implementar meio impedimento ali.

### Cobertura de cenários

| Cenário | RF | Épico | Tipo de teste |
| --- | --- | --- | --- |
| SCN-001.1 | RF-001 | EPIC-01 | e2e |
| SCN-001.2 | RF-001 | EPIC-01 | e2e |
| SCN-001.3 | RF-001 | EPIC-01 | integração |
| SCN-002.1 | RF-002 | EPIC-01 | integração |
| SCN-002.2 | RF-002 | EPIC-01 | integração |
| SCN-002.3 | RF-002 | EPIC-01 | integração |
| SCN-002.4 | RF-002 | EPIC-02 | integração |
| SCN-003.1 | RF-003 | EPIC-02 | e2e |
| SCN-003.2 | RF-003 | EPIC-02 | integração |
| SCN-003.3 | RF-003 | EPIC-04 | integração |
| SCN-004.1 | RF-004 | EPIC-02 | integração |
| SCN-004.2 | RF-004 | EPIC-02 | unitário |
| SCN-004.3 | RF-004 | EPIC-02 | integração |
| SCN-005.1 | RF-005 | EPIC-03 | integração |
| SCN-005.2 | RF-005 | EPIC-03 | integração |
| SCN-005.3 | RF-005 | EPIC-03 | integração |
| SCN-006.1 | RF-006 | EPIC-03 | integração |
| SCN-006.2 | RF-006 | EPIC-03 | integração |
| SCN-006.3 | RF-006 | EPIC-04 | integração |
| SCN-007.1 | RF-007 | EPIC-03 | integração |
| SCN-007.2 | RF-007 | EPIC-03 | unitário |
| SCN-007.3 | RF-007 | EPIC-03 | integração |
| SCN-007.4 | RF-007 | EPIC-04 | integração |
| SCN-008.1 | RF-008 | EPIC-03 | integração |
| SCN-008.2 | RF-008 | EPIC-03 | integração |
| SCN-008.3 | RF-008 | EPIC-04 | integração |
| SCN-009.1 | RF-009 | EPIC-04 | integração |
| SCN-009.2 | RF-009 | EPIC-04 | unitário |
| SCN-009.3 | RF-009 | EPIC-04 | integração |
| SCN-010.1 | RF-010 | EPIC-04 | integração |
| SCN-010.2 | RF-010 | EPIC-04 | integração |
| SCN-010.3 | RF-010 | EPIC-04 | unitário |
| SCN-011.1 | RF-011 | EPIC-05 | integração |
| SCN-011.2 | RF-011 | EPIC-05 | integração |
| SCN-011.3 | RF-011 | EPIC-05 | integração |
| SCN-012.1 | RF-012 | EPIC-05 | integração |
| SCN-012.2 | RF-012 | EPIC-05 | integração |
| SCN-012.3 | RF-012 | EPIC-05 | integração |
| SCN-012.4 | RF-012 | EPIC-05 | integração |
| SCN-013.1 | RF-013 | EPIC-05 | integração |
| SCN-013.2 | RF-013 | EPIC-05 | integração |
| SCN-013.3 | RF-013 | EPIC-05 | integração |
| SCN-014.1 | RF-014 | EPIC-07 | integração |
| SCN-014.2 | RF-014 | EPIC-07 | e2e |
| SCN-014.3 | RF-014 | EPIC-07 | integração |
| SCN-015.1 | RF-015 | EPIC-07 | integração |
| SCN-015.2 | RF-015 | EPIC-07 | e2e |
| SCN-015.3 | RF-015 | EPIC-07 | integração |
| SCN-016.1 | RF-016 | EPIC-07 | integração |
| SCN-016.2 | RF-016 | EPIC-07 | integração |
| SCN-016.3 | RF-016 | EPIC-07 | integração |
| SCN-017.1 | RF-017 | EPIC-02 | integração |
| SCN-017.2 | RF-017 | EPIC-02 | unitário |
| SCN-017.3 | RF-017 | EPIC-02 | integração |
| SCN-018.1 | RF-018 | EPIC-06 | integração |
| SCN-018.2 | RF-018 | EPIC-06 | integração |
| SCN-018.3 | RF-018 | EPIC-07 | integração |
| SCN-019.1 | RF-019 | EPIC-06 | integração |
| SCN-019.2 | RF-019 | EPIC-06 | integração |
| SCN-019.3 | RF-019 | EPIC-06 | integração |
| SCN-019.4 | RF-019 | EPIC-08 | e2e |
| SCN-020.1 | RF-020 | EPIC-08 | e2e |
| SCN-020.2 | RF-020 | EPIC-08 | e2e |
| SCN-020.3 | RF-020 | EPIC-08 | e2e |
| SCN-021.1 | RF-021 | EPIC-01 | integração |
| SCN-021.2 | RF-021 | EPIC-01 | integração |
| SCN-021.3 | RF-021 | EPIC-01 | integração |
| SCN-022.1 | RF-022 | EPIC-01 | integração |
| SCN-022.2 | RF-022 | EPIC-01 | integração |
| SCN-022.3 | RF-022 | EPIC-02 | integração |

70 cenários, 9 `e2e`, 56 `integração`, 5 `unitário` — a mesma contagem do PRD
v1.5 e da TechSpec v1.7. SCN-002.4 veio da emenda de 2026-09-10 que fechou
INC-22 e fica no EPIC-01, na mesma task da listagem: a marca de fluxo não
configurado é campo da resposta de `GET /v1/projetos`, e não depende de etapa,
tarefa nem board para ser verificada. Os três últimos vieram da emenda de 2026-09-10, que
criou RF-022: SCN-022.1 e SCN-022.2 ficam no EPIC-01, onde já mora a
administração global; SCN-022.3 fica no EPIC-02, porque verifica a **recusa da
criação de tarefa** em projeto sem fluxo, e nada disso é executável antes de o
fluxo e a tarefa existirem.

### Dimensionamento

| Épico | Tasks | Situação |
| --- | --- | --- |
| EPIC-01 | 8 | ok (≤ 8) |
| EPIC-02 | 8 | ok (≤ 8) |
| EPIC-03 | 5 | ok (≤ 8) |
| EPIC-04 | 4 | ok (≤ 8) |
| EPIC-05 | 5 | ok (≤ 8) |
| EPIC-06 | 3 | ok (≤ 8) |
| EPIC-07 | 6 | ok (≤ 8) |
| EPIC-08 | 4 | ok (≤ 8) |

---

## Grafo de dependências

```
EPIC-01
  ├── TASK-01.1
  ├── TASK-01.2 (depende de TASK-01.1)
  ├── TASK-01.3 (depende de TASK-01.2)
  ├── TASK-01.4 (depende de TASK-01.3)
  ├── TASK-01.5 (depende de TASK-01.4)
  ├── TASK-01.6 (depende de TASK-01.4)
  ├── TASK-01.8 (depende de TASK-01.5)
  └── TASK-01.7 (depende de TASK-01.5, TASK-01.6, TASK-01.8)

EPIC-02 (depende de EPIC-01)
  ├── TASK-02.1
  ├── TASK-02.2 (depende de TASK-02.1; e de TASK-01.5, por SCN-002.4)
  ├── TASK-02.3 (depende de TASK-02.1)
  ├── TASK-02.4 (depende de TASK-02.3)
  ├── TASK-02.5 (depende de TASK-02.4, TASK-02.2)
  ├── TASK-02.6 (depende de TASK-02.5)
  ├── TASK-02.7 (depende de TASK-02.2)
  └── TASK-02.8 (depende de TASK-02.6, TASK-02.7)

EPIC-03 (depende de EPIC-02)
  ├── TASK-03.1
  ├── TASK-03.2 (depende de TASK-03.1)
  ├── TASK-03.3 (depende de TASK-03.1)
  ├── TASK-03.4 (depende de TASK-03.3)
  └── TASK-03.5 (depende de TASK-03.2, TASK-03.4)

EPIC-04 (depende de EPIC-03)
  ├── TASK-04.1
  ├── TASK-04.2 (depende de TASK-04.1)
  ├── TASK-04.3 (depende de TASK-04.2)
  └── TASK-04.4 (depende de TASK-04.3)

EPIC-05 (depende de EPIC-04)
  ├── TASK-05.1
  ├── TASK-05.2 (depende de TASK-05.1)
  ├── TASK-05.3 (depende de TASK-05.2)
  ├── TASK-05.4 (depende de TASK-05.1)
  └── TASK-05.5 (depende de TASK-05.3, TASK-05.4)

EPIC-06 (depende de EPIC-03)
  ├── TASK-06.1
  ├── TASK-06.2 (depende de TASK-06.1)
  └── TASK-06.3 (depende de TASK-06.2)

EPIC-07 (depende de EPIC-05 e EPIC-06)
  ├── TASK-07.1
  ├── TASK-07.2
  ├── TASK-07.3 (depende de TASK-07.2)
  ├── TASK-07.4 (depende de TASK-07.3)
  ├── TASK-07.5 (depende de TASK-07.1)
  └── TASK-07.6 (depende de TASK-07.3)

EPIC-08 (depende de EPIC-07)
  ├── TASK-08.1
  ├── TASK-08.2 (depende de TASK-08.1)
  ├── TASK-08.3 (depende de TASK-08.1)
  └── TASK-08.4 (depende de TASK-08.2, TASK-08.3)
```

Épicos que podem correr em paralelo: EPIC-04 e EPIC-06, ambos após EPIC-03;
EPIC-05 entra em paralelo com EPIC-06 assim que EPIC-04 fecha. Todo o resto é
serial, porque cada épico consome o esquema e o núcleo de escrita do anterior.
A ordem dentro do épico é serial — as tasks compartilham o mesmo PR.

---

## EPIC-01 — Acesso, sessão e projetos visíveis

- **Sistema:** idsd
- **Entrega:** SCN-001.1, SCN-001.2, SCN-001.3, SCN-002.1, SCN-002.2, SCN-002.3, SCN-021.1, SCN-021.2, SCN-021.3, SCN-022.1, SCN-022.2
- **Depende de:** nenhuma
- **Fatia vertical:** ao final, uma pessoa autentica no provedor de identidade, é
  autoprovisionada, vê a lista dos projetos em que participa com as permissões de
  cada um, e o primeiro administrador global existe por promoção auditada. Sobe
  em contêiner com um comando, com banco migrado por serviço dedicado.
- **Gate do épico:** PR aberto e os onze cenários entregues passando.

### TASK-01.1 — Esqueleto do backend e estrutura por domínio

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** nenhuma
- **Cenários cobertos:** SCN-001.1
- **Origem:** ADR-001, ADR-009, quickstart §1 e §2
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-01.1-esqueleto-backend.md`

### TASK-01.2 — Compose de desenvolvimento, migração e identidade

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-01.1
- **Cenários cobertos:** SCN-001.1
- **Origem:** ADR-008, ADR-011, ADR-012, ADR-013, quickstart §3
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-01.2-compose-e-migracao.md`

### TASK-01.3 — Migration 1 e entidades de usuário, projeto e participação

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-01.2
- **Cenários cobertos:** SCN-002.1
- **Origem:** RF-002, RN-015, BDR-001, modelo de dados §3 e §8
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-01.3-migration-configuracao.md`

### TASK-01.4 — Sessão autenticada, autoprovisionamento e admin global

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-01.3
- **Cenários cobertos:** SCN-001.3, SCN-021.1
- **Origem:** RF-001, RF-021, RN-035, ADR-003, ADR-006, ADR-010
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-01.4-sessao-e-admin-global.md`

### TASK-01.5 — Listagem e detalhe de projeto com alcance global

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-01.4
- **Cenários cobertos:** SCN-002.1, SCN-002.2, SCN-002.3, SCN-021.2, SCN-021.3
- **Origem:** RF-002, RF-021, RN-015, RN-035, BDR-001
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-01.5-projetos-visiveis.md`

### TASK-01.6 — problem+json, negação por padrão e limite de requisições

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-01.4
- **Cenários cobertos:** SCN-002.3
- **Origem:** RNF-004, RNF-010, TechSpec Seção 8
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-01.6-erros-e-limites.md`

### TASK-01.7 — Frontend: entrada autenticada, lista de projetos e novo projeto

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** misto
- **Tentativas:** 3
- **Esforço:** M
- **Depende de:** TASK-01.5, TASK-01.6, TASK-01.8
- **Cenários cobertos:** SCN-001.1, SCN-001.2
- **Origem:** RF-001, RF-002, RF-022, DDR-004, DDR-005
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-01.7-frontend-entrada-e-projetos.md`

### TASK-01.8 — Criação de projeto pelo administrador global

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-01.5
- **Cenários cobertos:** SCN-022.1, SCN-022.2
- **Origem:** RF-022, RN-035, RN-036, RN-037, RN-038, ADR-010, BDR-001
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-01.8-criacao-de-projeto.md`

---

## EPIC-02 — Fluxo de etapas, criação de tarefa e board

- **Sistema:** idsd
- **Entrega:** SCN-002.4, SCN-017.1, SCN-017.2, SCN-017.3, SCN-004.1, SCN-004.2, SCN-004.3, SCN-022.3, SCN-003.1, SCN-003.2
- **Depende de:** EPIC-01
- **Fatia vertical:** ao final, quem configura define o fluxo de etapas do
  projeto, qualquer participante cria tarefa e todos veem o board com as etapas,
  as tarefas e os contadores. É a primeira fatia em que o produto faz o que o
  nome promete.
- **Gate do épico:** PR aberto e os dez cenários entregues passando.

### TASK-02.1 — Migration 2 e entidades de etapa e raia

- **Status:** concluída
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-01.3
- **Cenários cobertos:** SCN-017.1
- **Origem:** RF-017, RF-018, RN-021, RN-023, modelo de dados §3
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-02.1-migration-etapa-e-raia.md`

### TASK-02.2 — Consulta e substituição do fluxo de etapas

- **Status:** concluída
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-02.1, TASK-01.5
- **Cenários cobertos:** SCN-002.4, SCN-017.1, SCN-017.2, SCN-017.3
- **Origem:** RF-002, RF-017, RN-001, RN-020, RN-021, RN-022, RN-038
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-02.2-configuracao-do-fluxo.md`

### TASK-02.3 — Migrations 3 a 6 do anel de verdade e da projeção

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-02.1
- **Cenários cobertos:** SCN-004.1
- **Origem:** SDR-001, SDR-004, RN-008, RN-014, modelo de dados §4 a §6 e §8
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-02.3-migrations-verdade-e-projecao.md`

### TASK-02.4 — Núcleo de escrita: evento, sequência e projeção

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-02.3
- **Cenários cobertos:** SCN-004.1
- **Origem:** SDR-001, SDR-004, RNF-008, TechSpec Seção 5
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-02.4-nucleo-de-escrita.md`

### TASK-02.5 — Criação de tarefa

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-02.4, TASK-02.2
- **Cenários cobertos:** SCN-004.1, SCN-004.2, SCN-004.3, SCN-022.3
- **Origem:** RF-004, RF-022, RN-004, RN-006, RN-038
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-02.5-criacao-de-tarefa.md`

### TASK-02.6 — Leitura do board

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-02.5
- **Cenários cobertos:** SCN-003.1, SCN-003.2
- **Origem:** RF-003, RN-002, RN-008, RN-015, RNF-009
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-02.6-leitura-do-board.md`

### TASK-02.7 — Frontend: configuração do fluxo de etapas

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** misto
- **Tentativas:** 3
- **Esforço:** M
- **Depende de:** TASK-01.7, TASK-02.2
- **Cenários cobertos:** SCN-017.1, SCN-017.2, SCN-017.3
- **Origem:** RF-017, tela TL-08, DDR-004, DDR-005
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-02.7-frontend-configuracao-do-fluxo.md`

### TASK-02.8 — Frontend: board e criação de tarefa

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** misto
- **Tentativas:** 3
- **Esforço:** G
- **Depende de:** TASK-02.6, TASK-02.7
- **Cenários cobertos:** SCN-003.1, SCN-003.2, SCN-004.1, SCN-004.2, SCN-004.3
- **Origem:** RF-003, RF-004, telas TL-03 e TL-05, DDR-002, DDR-005, DDR-006
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-02.8-frontend-board-e-nova-tarefa.md`

---

## EPIC-03 — Handoff: mover, assumir e devolver

- **Sistema:** idsd
- **Entrega:** SCN-005.1, SCN-005.2, SCN-005.3, SCN-006.1, SCN-006.2, SCN-007.1, SCN-007.2, SCN-007.3, SCN-008.1, SCN-008.2
- **Depende de:** EPIC-02
- **Fatia vertical:** ao final, a tarefa circula. Alguém move de etapa, o próximo
  assume e quem não vai tocar devolve ao pool — que é o handoff apontado como o
  efeito mais caro do estado atual, e o motivo de existir da direção aprovada.
- **Gate do épico:** PR aberto e os dez cenários entregues passando.

### TASK-03.1 — Envelope de origem declarada e conflito com o estado atual

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** nenhuma
- **Cenários cobertos:** SCN-005.3, SCN-007.2
- **Origem:** SDR-002, RN-012, RN-013, RN-031
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-03.1-origem-declarada-e-conflito.md`

### TASK-03.2 — Movimentação entre etapas

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-03.1
- **Cenários cobertos:** SCN-005.1, SCN-005.2, SCN-005.3, SCN-006.1, SCN-006.2
- **Origem:** RF-005, RF-006, RN-005, RN-009, RN-018, RN-030
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-03.2-movimentacao-entre-etapas.md`

### TASK-03.3 — Tomada de tarefa

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-03.1
- **Cenários cobertos:** SCN-007.1, SCN-007.2, SCN-007.3
- **Origem:** RF-007, RN-007, RN-030, RN-033
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-03.3-tomada-de-tarefa.md`

### TASK-03.4 — Devolução ao pool

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-03.3
- **Cenários cobertos:** SCN-008.1, SCN-008.2
- **Origem:** RF-008, RN-030
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-03.4-devolucao-ao-pool.md`

### TASK-03.5 — Frontend: ficha da tarefa com mover, assumir e devolver

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** misto
- **Tentativas:** 3
- **Esforço:** G
- **Depende de:** TASK-02.8, TASK-03.2, TASK-03.4
- **Cenários cobertos:** SCN-005.1, SCN-005.3, SCN-007.1, SCN-007.3, SCN-008.1
- **Origem:** RF-005, RF-006, RF-007, RF-008, telas TL-03 e TL-04, DDR-002, DDR-005, DDR-006
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-03.5-frontend-ficha-e-handoff.md`

---

## EPIC-04 — Impedimento como terceira dimensão

- **Sistema:** idsd
- **Entrega:** SCN-009.1, SCN-009.2, SCN-009.3, SCN-010.1, SCN-010.2, SCN-010.3, SCN-003.3, SCN-006.3, SCN-007.4, SCN-008.3
- **Depende de:** EPIC-03
- **Fatia vertical:** ao final, o time sinaliza o que travou, quem pode
  desbloquear vê o destaque, e o desfecho é registrado por quem resolveu. O tempo
  de impedimento passa a ser série própria, medida em paralelo às outras duas.
- **Gate do épico:** PR aberto e os dez cenários entregues passando.

Os quatro cenários transversais — o cartão com duas contagens, o movimento que
preserva a marca, a tomada de tarefa impedida e a devolução com impedimento
aberto — entregam neste épico e não naqueles que criaram as operações
correspondentes. Cada um verifica que a marca sobrevive a uma operação de outra
dimensão, e nenhum é executável antes de o impedimento existir.

### TASK-04.1 — Abertura e anotação de impedimento

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** nenhuma
- **Cenários cobertos:** SCN-009.1, SCN-009.2, SCN-009.3
- **Origem:** RF-009, RN-010, RN-024, BDR-002
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-04.1-abertura-de-impedimento.md`

### TASK-04.2 — Registro do desfecho do impedimento

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-04.1
- **Cenários cobertos:** SCN-010.1, SCN-010.2, SCN-010.3
- **Origem:** RF-010, RN-032, BDR-002
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-04.2-desfecho-do-impedimento.md`

### TASK-04.3 — Ortogonalidade da marca nas operações de etapa e de tomada

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-04.2
- **Cenários cobertos:** SCN-006.3, SCN-007.4, SCN-008.3
- **Origem:** RN-002, RN-009, RN-032, RN-033
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-04.3-ortogonalidade-da-marca.md`

### TASK-04.4 — Frontend: marca no cartão e bloco de impedimento na ficha

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** misto
- **Tentativas:** 3
- **Esforço:** M
- **Depende de:** TASK-03.5, TASK-04.3
- **Cenários cobertos:** SCN-003.3, SCN-009.1, SCN-010.1
- **Origem:** RF-009, RF-010, telas TL-03 e TL-04, DDR-005, DDR-007
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-04.4-frontend-impedimento.md`

---

## EPIC-05 — Desfechos: concluir, encerrar e reabrir

- **Sistema:** idsd
- **Entrega:** SCN-011.1, SCN-011.2, SCN-011.3, SCN-012.1, SCN-012.2, SCN-012.3, SCN-012.4, SCN-013.1, SCN-013.2, SCN-013.3
- **Depende de:** EPIC-04
- **Fatia vertical:** ao final, a tarefa sai do fluxo por um dos dois desfechos e
  o Product Owner pode reabrir a que foi concluída, começando episódio novo. É a
  fatia que fecha o ciclo de vida e torna o tempo por etapa comparável.
- **Gate do épico:** PR aberto e os dez cenários entregues passando.

### TASK-05.1 — Conclusão ao alcançar etapa terminal

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** nenhuma
- **Cenários cobertos:** SCN-011.1
- **Origem:** RF-011, RN-011, RN-018
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-05.1-conclusao-em-etapa-terminal.md`

### TASK-05.2 — Superfície de conclusão e recusa compartilhada da marca

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-05.1
- **Cenários cobertos:** SCN-011.2, SCN-011.3
- **Origem:** RF-011, RN-011, RN-031
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-05.2-superficie-de-conclusao.md`

### TASK-05.3 — Encerramento sem conclusão

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-05.2
- **Cenários cobertos:** SCN-012.1, SCN-012.2, SCN-012.3, SCN-012.4
- **Origem:** RF-012, RN-011, RN-016, RN-018, RN-032
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-05.3-encerramento-sem-conclusao.md`

### TASK-05.4 — Reabertura pelo Product Owner com novo episódio

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-05.3
- **Cenários cobertos:** SCN-013.1, SCN-013.2, SCN-013.3
- **Origem:** RF-013, RN-017, RN-019, RN-034
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-05.4-reabertura-pelo-po.md`

### TASK-05.5 — Frontend: desfechos na ficha e diálogo de reabertura

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** misto
- **Tentativas:** 3
- **Esforço:** M
- **Depende de:** TASK-04.4, TASK-05.4
- **Cenários cobertos:** SCN-011.1, SCN-011.3, SCN-012.2, SCN-013.1
- **Origem:** RF-011, RF-012, RF-013, telas TL-04 e TL-10, DDR-005, DDR-007
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-05.5-frontend-desfechos-e-reabertura.md`

---

## EPIC-06 — Raias, participação e papéis

- **Sistema:** idsd
- **Entrega:** SCN-018.1, SCN-018.2, SCN-019.1, SCN-019.2, SCN-019.3
- **Depende de:** EPIC-05
- **Fatia vertical:** ao final, quem configura organiza o board em raias e
  administra quem participa do projeto e com quais papéis, com efeito imediato
  sobre as permissões e sobre as tarefas assumidas por quem sai.
- **Gate do épico:** PR aberto e os cinco cenários entregues passando.

### TASK-06.1 — Consulta e substituição das raias

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** nenhuma
- **Cenários cobertos:** SCN-018.1, SCN-018.2
- **Origem:** RF-018, RN-023
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-06.1-raias.md`

### TASK-06.2 — Participação e papéis, com devolução ao pool na remoção

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** nenhuma
- **Cenários cobertos:** SCN-019.1, SCN-019.2, SCN-019.3
- **Origem:** RF-019, RN-024, RN-027, BDR-001, BDR-002
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-06.2-participacao-e-papeis.md`

### TASK-06.3 — Frontend: raias na configuração e tela de participação

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** misto
- **Tentativas:** 3
- **Esforço:** M
- **Depende de:** TASK-05.5, TASK-06.1, TASK-06.2
- **Cenários cobertos:** SCN-018.1, SCN-019.1
- **Origem:** RF-018, RF-019, telas TL-08 e TL-09, DDR-005
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-06.3-frontend-raias-e-participacao.md`

---

## EPIC-07 — Fila pessoal e consultas agregadas

- **Sistema:** idsd
- **Entrega:** SCN-014.1, SCN-014.2, SCN-014.3, SCN-015.1, SCN-015.2, SCN-015.3, SCN-016.1, SCN-016.2, SCN-016.3, SCN-018.3
- **Depende de:** EPIC-06
- **Fatia vertical:** ao final, cada pessoa vê em um lugar só o que espera por
  ela em todos os projetos, o gestor acompanha sem interferir, e o tempo por
  etapa produz a linha de base que hoje não existe.
- **Gate do épico:** PR aberto e os dez cenários entregues passando.

### TASK-07.1 — Fila pessoal entre projetos

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** nenhuma
- **Cenários cobertos:** SCN-014.1, SCN-014.2, SCN-014.3
- **Origem:** RF-014, RN-024, RN-025, BDR-002
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-07.1-fila-pessoal.md`

### TASK-07.2 — Andamento do projeto em modo somente-leitura

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** nenhuma
- **Cenários cobertos:** SCN-015.1, SCN-015.2, SCN-015.3
- **Origem:** RF-015, RN-014, RN-015, RN-026
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-07.2-andamento-do-projeto.md`

### TASK-07.3 — Tempo por etapa com as três séries

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** nenhuma
- **Cenários cobertos:** SCN-016.1, SCN-016.2, SCN-016.3, SCN-018.3
- **Origem:** RF-016, RN-008, RN-014, RN-019, RN-023, RN-025, RN-029
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-07.3-tempo-por-etapa.md`

### TASK-07.4 — Exportação do tempo por etapa em CSV

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-07.3
- **Cenários cobertos:** SCN-016.1
- **Origem:** RNF-007, RN-014
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-07.4-exportacao-csv.md`

### TASK-07.5 — Frontend: minha fila

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** misto
- **Tentativas:** 3
- **Esforço:** M
- **Depende de:** TASK-06.3, TASK-07.1
- **Cenários cobertos:** SCN-014.1, SCN-014.3
- **Origem:** RF-014, tela TL-06, DDR-005, DDR-006
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-07.5-frontend-minha-fila.md`

### TASK-07.6 — Frontend: andamento e tempo por etapa

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** misto
- **Tentativas:** 3
- **Esforço:** M
- **Depende de:** TASK-07.2, TASK-07.4, TASK-07.5
- **Cenários cobertos:** SCN-015.1, SCN-016.1, SCN-016.3
- **Origem:** RF-015, RF-016, tela TL-07, DDR-005
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-07.6-frontend-andamento-e-tempo.md`

---

## EPIC-08 — Acompanhamento em tempo real

- **Sistema:** idsd
- **Entrega:** SCN-020.1, SCN-020.2, SCN-020.3, SCN-019.4
- **Depende de:** EPIC-07
- **Fatia vertical:** ao final, o board se atualiza sozinho em todas as sessões
  abertas e a fila avisa quem passou a ser o próximo responsável. É o que fecha a
  segunda metade da direção aprovada — registrar o estado e avisar o próximo são
  a mesma ação.
- **Gate do épico:** PR aberto e os quatro cenários entregues passando.

### TASK-08.1 — Canal STOMP com autenticação e autorização de inscrição

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** nenhuma
- **Cenários cobertos:** SCN-020.1
- **Origem:** RF-020, RNF-004, ADR-003, ADR-006
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-08.1-canal-stomp.md`

### TASK-08.2 — Publicação após o commit, escuta e varredura de retomada

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-08.1
- **Cenários cobertos:** SCN-020.1, SCN-020.2
- **Origem:** RF-020, RNF-001, RNF-002, ADR-004, SDR-004
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-08.2-publicacao-e-escuta.md`

### TASK-08.3 — Revogação de inscrição na mudança e na remoção de participação

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-08.2
- **Cenários cobertos:** SCN-019.4
- **Origem:** RF-019, RN-015, RNF-004
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-08.3-revogacao-de-inscricao.md`

### TASK-08.4 — Frontend: cliente do canal, tolerância a lacuna e resincronização

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** misto
- **Tentativas:** 3
- **Esforço:** G
- **Depende de:** TASK-07.6, TASK-08.3
- **Cenários cobertos:** SCN-020.1, SCN-020.2, SCN-020.3
- **Origem:** RF-020, RNF-001, telas TL-03 e TL-06, DDR-003, DDR-005
- **Detalhe:** `docs/tasks/kanban-tarefas/TASK-08.4-frontend-tempo-real.md`

---

## Fora do escopo desta entrega

| Item | Por quê | Dono |
| --- | --- | --- |
| Promoção do papel semântico de impedimento para a coleção compartilhada `frontend/nextjs` | O papel foi definido localmente na extensão de tema deste sistema porque a coleção não o prevê; generalizá-lo é decisão de biblioteca e não de feature | `/guidelines` |
| Escolha da plataforma de integração contínua | Dívida nomeada na coleção `infra/docker`: os pré-requisitos estão escritos, a plataforma não foi escolhida, e nenhum dos 19 critérios depende de pipeline | `/guidelines` |
| Registry de imagens e política de tag de release | Dívida nomeada com gatilho de reabertura declarado: não há publicação nem ambiente compartilhado, e normatizar agora seria regra que ninguém consegue conferir | `/guidelines` |
| Escrita dos testes e execução das medições de carga | Os envelopes de desempenho estão nos critérios de aceite; quem os transforma em suíte congelada é a etapa de verificação | `/tests` |
| Instrumentação histórica anterior ao primeiro período de uso | Não existe linha de base, por decisão registrada na direção; o primeiro período de uso a produz | fora do produto |

## Fora deste artefato — regras negativas

- Este plano **não decide comportamento**. Toda regra citada aqui já está
  congelada no PRD; divergência entre uma task e um cenário é defeito da task.
- Este plano **não decide arquitetura**. Rotas, contratos, colunas e mecanismos
  vêm das decisões já aceitas; escolha nova descoberta durante a execução volta
  para quem a decide, e não é resolvida dentro da task.
- Este plano **não escreve verificação**. Os critérios de aceite dizem o que
  precisa ser verdadeiro e como se confere; os cenários congelados e os arquivos
  de verificação são produzidos na etapa seguinte.
- Este plano **não reordena camadas em épicos**. Nenhum épico é "o banco" ou "a
  API": cada um entrega comportamento observável de ponta a ponta.
- Este plano **não reabre escopo**. Item ausente aqui e presente no PRD é
  lacuna de cobertura, verificável pela tabela; item presente aqui e ausente no
  PRD é escopo órfão, e nenhum existe.

