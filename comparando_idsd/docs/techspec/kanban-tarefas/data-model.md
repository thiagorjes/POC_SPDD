# Modelo de dados — kanban-tarefas

_Data: 2026-09-09 | Fonte: `../kanban-tarefas-techspec.md` Seção 3_
_Decisões que o governam: SDR-001, SDR-002, BDR-002, ADR-005, ADR-009_

> Este é o documento completo. A TechSpec resume e referencia; nada aqui é
> repetido lá.

---

## 1. Os três anéis

O modelo tem três anéis com regras de escrita diferentes, e confundi-los é o erro
que RNF-008 existe para impedir.

| Anel | Tabelas | Regra de escrita |
| --- | --- | --- |
| **Verdade** | `evento_tarefa` | Somente `INSERT`. Nenhum caminho do produto emite `UPDATE` ou `DELETE` |
| **Projeção** | `tarefa`, `intervalo_tarefa`, `impedimento` | Derivadas do anel de verdade e reconstruíveis a partir dele. Escritas na mesma transação do evento |
| **Configuração** | `projeto`, `etapa`, `raia`, `participacao`, `participacao_papel`, `usuario` | Mutáveis por natureza. RN-022: alteração vale dali em diante e nunca reescreve histórico |

A projeção é descartável por construção. Defeito de cálculo em série de tempo se
corrige reconstruindo a projeção, sem perda de histórico e sem migração de dado.

---

## 2. Diagrama de entidades

```
usuario ──< participacao >── projeto ──< etapa
   │            │                │        │
   │            └──< participacao_papel    └──< raia
   │                                  │
   │                                  └──< tarefa >── etapa (atual)
   │                                          │  └─── raia (opcional)
   └──────── ator ────────< evento_tarefa >───┤
                                              ├──< intervalo_tarefa
                                              └──< impedimento
```

`intervalo_tarefa` **não referencia `usuario`**. É a materialização de RN-014:
a proibição de agregar tempo por pessoa deixa de depender de disciplina de quem
escreve a consulta e passa a ser propriedade do esquema.

---

## 3. Anel de configuração

### `usuario`

| Campo | Tipo | Notas |
| --- | --- | --- |
| `id` | `uuid` PK | |
| `subject_id` | `text` UNIQUE NOT NULL | `sub` do token do provedor de identidade. Chave de vínculo, nunca o e-mail |
| `nome` | `text` NOT NULL | Espelhado do token a cada entrada, com recuo declarado abaixo |
| `email` | `text` NULL | Idem. **Anulável desde 2026-09-10** — ver abaixo |
| `admin_global` | `boolean` NOT NULL DEFAULT false | ADR-007 e BDR-001. Fora do vínculo por projeto |
| `criado_em` | `timestamptz` NOT NULL | Autoprovisionamento na primeira entrada (RF-001) |

Não há senha, não há cadastro local — ADR-006.

**Token válido sem `name` ou sem `email` (decisão de 2026-09-10).** É estado
alcançável, e não hipótese: em realm federado o provedor de origem decide quais
claims propaga, e nada obriga as duas. Com as colunas `NOT NULL`, a primeira
entrada dessa pessoa violava restrição e o autoprovisionamento de RF-001
respondia `500` — falha de instalação disfarçada de defeito da aplicação, na
única rota que a pessoa consegue alcançar.

- `email` passa a ser **anulável**. Nada no sistema depende dele: ADR-010 fixou o
  `sub` como chave e proíbe expressamente que autorização o consulte, de modo que
  o e-mail é campo de exibição. Exigi-lo seria transformar um dado decorativo em
  pré-condição de entrada.
- `nome` continua `NOT NULL` e ganha recuo no serviço, nesta ordem: `name` →
  `preferred_username` → `subject_id`. Ele **é** exibido — no cartão, na fila e na
  relação de participações —, e nulo ali produziria interface sem quem, que é pior
  que um identificador feio. O recuo é do serviço e não `DEFAULT` da coluna:
  `DEFAULT` só age na inserção e deixaria a atualização a cada entrada gravar
  nulo.
- **Recusar a entrada foi considerado e descartado.** O token é legítimo e a
  autenticação funcionou; o que falta é configuração do realm. Recusar deixaria a
  pessoa sem caminho algum, porque ADR-006 não prevê autenticação alternativa, e
  o erro apareceria para quem não tem como corrigi-lo.
- O espelhamento a cada entrada não apaga o que já existe: claim ausente
  **preserva** o valor gravado. Sem isso, uma mudança de configuração do provedor
  esvaziaria em massa o e-mail de quem já havia entrado.

### `projeto`

| Campo | Tipo | Notas |
| --- | --- | --- |
| `id` | `uuid` PK | |
| `nome` | `text` NOT NULL | |
| `descricao` | `text` | |
| `criado_em` | `timestamptz` NOT NULL | |
| `seq_atual` | `bigint` NOT NULL DEFAULT 0 | Contador de sequência de eventos do projeto. Ver §4 |

O contador vive aqui, e não em `SEQUENCE` do PostgreSQL, por um motivo que o
comitê de análise expôs: `SEQUENCE` não é transacional, então transação revertida
deixaria buraco permanente e todo cliente resincronizaria para sempre.

### `etapa`

| Campo | Tipo | Notas |
| --- | --- | --- |
| `id` | `uuid` PK | **Identidade estável.** RN-021: renomear não afeta o histórico porque a série segue o `id`, não o nome |
| `projeto_id` | `uuid` FK NOT NULL | |
| `nome` | `text` NOT NULL | Mutável |
| `ordem` | `integer` NOT NULL | Define adjacência de RN-005. Única por projeto entre etapas não arquivadas |
| `terminal` | `boolean` NOT NULL DEFAULT false | RN-001: ao menos uma por projeto |
| `arquivada_em` | `timestamptz` NULL | Remoção é lógica. Histórico e intervalos referenciam a etapa para sempre; apagá-la fisicamente destruiria a série que RNF-008 protege |

RN-020 — remoção recusada com tarefas na etapa — é regra de serviço, verificada
antes de gravar `arquivada_em`. O banco não a expressa porque a condição envolve
contagem de tarefas ativas, não integridade referencial.

### `raia`

| Campo | Tipo | Notas |
| --- | --- | --- |
| `id` | `uuid` PK | |
| `projeto_id` | `uuid` FK NOT NULL | |
| `nome` | `text` NOT NULL | |
| `ordem` | `integer` NOT NULL | |
| `arquivada_em` | `timestamptz` NULL | |

RN-023: raia não restringe transição e não entra em agregação. Consequência no
esquema: **nenhuma tabela do anel de projeção referencia `raia`**. A ausência é a
regra.

### `participacao` e `participacao_papel`

| `participacao` | Tipo | Notas |
| --- | --- | --- |
| `id` | `uuid` PK | |
| `usuario_id` | `uuid` FK NOT NULL | |
| `projeto_id` | `uuid` FK NOT NULL | |
| `criada_em` | `timestamptz` NOT NULL | |
| — | UNIQUE `(usuario_id, projeto_id)` | Um vínculo por par |

| `participacao_papel` | Tipo | Notas |
| --- | --- | --- |
| `participacao_id` | `uuid` FK NOT NULL | |
| `papel` | `text` NOT NULL | Catálogo fechado do BDR-001 |
| — | PK `(participacao_id, papel)` | Papéis acumuláveis no mesmo projeto |

Remover a participação de quem tem tarefa assumida devolve a tarefa ao pool
(RN-027, SCN-019.2) e preserva no log quem havia assumido. A `participacao` é
apagada; o histórico não.

### Catálogo de papéis e permissões

Conjunto fechado, definido em código como enumeração — não é tabela, e é isso que
impede que alguém componha permissão nova em runtime, conforme BDR-001.

| Papel | Ler board | Escrever tarefa | Desbloquear | Encerrar sem conclusão | Reabrir | Configurar projeto |
| --- | --- | --- | --- | --- | --- | --- |
| `project_admin` | sim | sim | **sim** | sim | não | sim |
| `product_owner` | sim | sim | **sim** | sim | **sim** | não |
| `dev` | sim | sim | não | não | não | não |
| `gestor` | sim | **não** | não | não | não | não |
| `user` (legado) | não | não | não | não | não | não |

- **Desbloquear** vem de BDR-002. Quem sinalizou o impedimento também resolve,
  sem papel (SCN-010.2) — é regra de serviço, não linha desta tabela.
- **Reabrir** é privativo de `product_owner` (RN-017, SCN-013.2).
- **Encerrar sem conclusão** exige permissão de configuração (RN-016). É a única
  linha em que `product_owner` a tem sem administrar o projeto — decorre de RN-016
  falar em "permissão de configuração" e de o PO ser quem decide que a tarefa não
  será feita.
- `gestor` é somente-leitura (RN-015): não recebe ação de escrita na interface e é
  recusado se solicitar por outro caminho.
- `admin_global` atravessa todos os projetos e não aparece aqui porque não é
  vínculo — BDR-001.

---

## 4. Anel de verdade: `evento_tarefa`

Log append-only. Fonte de tudo o que a projeção afirma.

| Campo | Tipo | Notas |
| --- | --- | --- |
| `id` | `bigserial` PK | Ordem total de gravação |
| `tarefa_id` | `uuid` NOT NULL | |
| `projeto_id` | `uuid` NOT NULL | Desnormalizado: todo filtro de consulta parte do projeto |
| `tipo` | `text` NOT NULL | Ver catálogo abaixo |
| `ocorrido_em` | `timestamptz` NOT NULL | |
| `ator_id` | `uuid` NULL | Quem agiu. `NULL` em evento decorrente de configuração (ex.: devolução ao pool por remoção de participação) |
| `episodio` | `integer` NOT NULL | 1 na criação; incrementa a cada reabertura (RN-019) |
| `etapa_origem_id` | `uuid` NULL | |
| `etapa_destino_id` | `uuid` NULL | |
| `condicao_origem` | `text` NULL | |
| `condicao_destino` | `text` NULL | |
| `seq` | `bigint` NOT NULL | Sequência por projeto, para o payload do `NOTIFY` e a resincronização client-side do ADR-004. **Único** por `(projeto_id, seq)`. Geração abaixo |
| `dados` | `jsonb` NULL | Motivo do impedimento, desfecho, título na criação. Nunca dado de cliente (IDSD 4.10.1) |

### Geração do `seq` (SDR-004)

O número é atribuído **no banco, dentro da transação de escrita**, por
`UPDATE projeto SET seq_atual = seq_atual + 1 WHERE id = ? RETURNING seq_atual`.
Três consequências que precisam estar explícitas, porque nenhuma é gratuita:

- **Serializa as escritas do mesmo projeto**, e só delas. Duas transições
  concorrentes em tarefas diferentes do mesmo projeto passam pelo `@Version` de
  SDR-002 sem conflito — é justamente o caso em que `MAX(seq)+1` produziria o
  mesmo número. A contenção é o preço da contiguidade, e é aceita: o board de um
  projeto não é caminho de escrita de alta concorrência.
- **Rollback não deixa buraco**, porque o incremento reverte com a transação.
- O contador **não** é por pod. ADR-004 dizia isso e está superado por SDR-004
  neste ponto: com duas instâncias escrevendo no mesmo projeto, contador por pod
  produz `seq` duplicado, e duplicata torna a lacuna indetectável — quebra
  exatamente a rede de segurança que ADR-004 nomeia para RNF-002.

**Sem `UPDATE`, sem `DELETE`.** A garantia é dupla: nenhum método de repositório
os expõe, e a role de aplicação no PostgreSQL recebe apenas `SELECT, INSERT` nesta
tabela. RNF-008 é verificado tentando alterar por cada caminho exposto.

### Catálogo de tipos de evento

| Tipo | Origina | Fecha intervalo | Abre intervalo |
| --- | --- | --- | --- |
| `TAREFA_CRIADA` | RF-004 | — | `PERMANENCIA`, `ESPERA_TOMADA` |
| `TAREFA_ASSUMIDA` | RF-007 | `ESPERA_TOMADA` | — |
| `TAREFA_DEVOLVIDA` | RF-008, RN-027 | — | `ESPERA_TOMADA` |
| `TAREFA_MOVIDA` | RF-005, RF-006 | `PERMANENCIA`, `ESPERA_TOMADA` | `PERMANENCIA`, `ESPERA_TOMADA` |
| `IMPEDIMENTO_ABERTO` | RF-009 | — | `IMPEDIMENTO` |
| `IMPEDIMENTO_ANOTADO` | RF-009, RN-010 | — | — |
| `IMPEDIMENTO_RESOLVIDO` | RF-010 | `IMPEDIMENTO` | — |
| `TAREFA_CONCLUIDA` | RF-011 | todos abertos | — |
| `TAREFA_ENCERRADA_SEM_CONCLUSAO` | RF-012 | `PERMANENCIA`, `ESPERA_TOMADA` | — |
| `TAREFA_REABERTA` | RF-013 | — | `PERMANENCIA`, `ESPERA_TOMADA` (episódio+1) |

`TAREFA_MOVIDA` **não** fecha `IMPEDIMENTO`: RN-009 manda o impedimento acompanhar
a tarefa sem que a transição encerre nem reinicie sua contagem. É o que SCN-006.3
verifica, e a linha mais fácil de errar deste catálogo.

---

## 5. Anel de projeção

### `tarefa` — estado corrente

| Campo | Tipo | Notas |
| --- | --- | --- |
| `id` | `uuid` PK | |
| `projeto_id` | `uuid` FK NOT NULL | |
| `titulo` | `text` NOT NULL | RF-004: obrigatório (SCN-004.2) |
| `descricao` | `text` | |
| `etapa_id` | `uuid` FK NOT NULL | Dimensão 1 de RN-002 |
| `raia_id` | `uuid` FK NULL | |
| `condicao` | `text` NOT NULL | Dimensão 2 de RN-002. Domínio de RN-003 |
| _(sem coluna)_ | — | Dimensão 3 de RN-002 — a marca de impedimento é derivada da existência de linha em `impedimento` com `desfecho IS NULL`, nunca replicada aqui |
| `responsavel_id` | `uuid` FK NULL | `NULL` quando aguardando tomada (RN-006) |
| `assumida_em` | `timestamptz` NULL | |
| `episodio_atual` | `integer` NOT NULL DEFAULT 1 | |
| `versao` | `bigint` NOT NULL | Bloqueio otimista (`@Version`). SDR-002 |
| `criada_em` | `timestamptz` NOT NULL | |

Domínio de `condicao`: `AGUARDANDO_TOMADA`, `EM_CURSO`, `CONCLUIDA`,
`ENCERRADA_SEM_CONCLUSAO`. Restrição de verificação no banco, além da enumeração
em código.

**`IMPEDIDA` saiu do domínio da condição — emenda de RN-002/RN-003 (PRD v1.1).**
O impedimento é a terceira dimensão, ortogonal às outras duas: coexiste com
qualquer condição não terminal e com qualquer etapa. Modelá-lo como valor de
`condicao` obrigava a movimentação a sobrescrevê-lo, que é exatamente o defeito
que INC-01 localizou. Como dimensão derivada, nenhuma escrita sobre `tarefa`
consegue apagá-lo: só o registro do desfecho em `impedimento` o apaga (RN-032), e
isso é propriedade do esquema, não disciplina de quem escreve o serviço.

Consequências diretas:

- A tomada é aceita com impedimento aberto (RN-033) — não há pré-condição a
  verificar em `tarefa`, porque a marca não vive lá.
- A resolução do impedimento não restaura condição nenhuma: a condição nunca foi
  alterada pela abertura.
- A tarefa impedida mantém `etapa_id` e pode mudá-la (RN-009), e a movimentação
  não toca em `impedimento`.
- A projeção de leitura do board expõe a marca por `EXISTS` sobre `impedimento`
  aberto, no mesmo join da consulta única de §6 — não há segunda ida ao banco.

### `intervalo_tarefa` — as três séries

| Campo | Tipo | Notas |
| --- | --- | --- |
| `id` | `bigserial` PK | |
| `tarefa_id` | `uuid` NOT NULL | |
| `projeto_id` | `uuid` NOT NULL | |
| `etapa_id` | `uuid` NOT NULL | Etapa vigente na **abertura** do intervalo. Em `IMPEDIMENTO`, é instantâneo, não vínculo — ver abaixo |
| `tipo` | `text` NOT NULL | `PERMANENCIA`, `ESPERA_TOMADA`, `IMPEDIMENTO` |
| `episodio` | `integer` NOT NULL | RN-019 |
| `inicio` | `timestamptz` NOT NULL | |
| `fim` | `timestamptz` NULL | `NULL` = em curso |
| `duracao` | `interval` GENERATED ALWAYS AS (`fim - inicio`) STORED | `NULL` enquanto em curso. Indexável — ver §6 |

**`etapa_id` em intervalo de impedimento é instantâneo da abertura, e isso não
viola RN-009.** A regra proíbe que a movimentação encerre ou reinicie a contagem
do impedimento; ela não exige que o impedimento seja anônimo quanto à etapa. A
versão anterior deste modelo deixava a coluna nula para impedimento, e o comitê
mostrou que a consequência era pior que o suposto rigor: o bloco `impedimento`
que RF-016 devolve **dentro de cada etapa** não seria computável, porque todo
impedimento cairia num único grupo nulo. Com o instantâneo, a série existe e o
intervalo continua atravessando a movimentação sem ser tocado.
**Não há coluna de pessoa, e não haverá.** RN-014.
**Não há coluna de total.** RN-008 proíbe somar as três séries entre si; uma
coluna de soma seria o convite a fazê-lo.

Um mesmo instante pode estar coberto por até três intervalos abertos da mesma
tarefa — permanência, espera e impedimento —, e é isso que SCN-003.3 e SCN-016.1
verificam. Não existe restrição de não sobreposição **entre tipos diferentes**.
Existe, sim, no máximo um intervalo aberto **por tipo** por tarefa: índice único
parcial sobre `(tarefa_id, tipo)` com `fim IS NULL`. É o que impede o impedimento
duplicado de RN-010 no nível do banco, e não só na regra de serviço.

### `impedimento`

| Campo | Tipo | Notas |
| --- | --- | --- |
| `id` | `uuid` PK | |
| `tarefa_id` | `uuid` NOT NULL | |
| `projeto_id` | `uuid` NOT NULL | Desnormalizado, como em `evento_tarefa` e `intervalo_tarefa`: RF-014 filtra pelo conjunto de projetos da pessoa e RF-015 por um projeto |
| `intervalo_id` | `bigint` FK NOT NULL | Aponta para o `intervalo_tarefa` de tipo `IMPEDIMENTO` |
| `motivo` | `text` NOT NULL | RF-009: obrigatório (SCN-009.2) |
| `anotacoes` | `jsonb` NOT NULL DEFAULT `[]` | RN-010: segunda sinalização anexa aqui, sem criar impedimento novo nem reiniciar contagem (SCN-009.3) |
| `aberto_por` | `uuid` FK NOT NULL | Habilita SCN-010.2 — quem sinalizou pode resolver |
| `desfecho` | `text` NULL | Preenchido na resolução |
| `resolvido_por` | `uuid` FK NULL | |

Índice único parcial sobre `tarefa_id` com `desfecho IS NULL`: no máximo um
impedimento aberto por tarefa (RN-010).

---

## 6. Índices

Conforme `backend/java/database.md` §4 — toda chave estrangeira indexada, mais os
compostos que as consultas do PRD exigem.

| Índice | Tabela | Serve |
| --- | --- | --- |
| `(projeto_id, etapa_id, condicao)` | `tarefa` | Board de RF-003; contagem por etapa de RF-015 |
| `(condicao, projeto_id)` parcial em `AGUARDANDO_TOMADA` | `tarefa` | Fila de RF-014, que atravessa projetos |
| `(responsavel_id)` | `tarefa` | Devolução ao pool na remoção de participação (RN-027) |
| `(projeto_id, etapa_id, tipo, inicio)` | `intervalo_tarefa` | Recorte da janela de RF-016 |
| `(projeto_id, etapa_id, tipo, duracao)` parcial em `fim IS NOT NULL` | `intervalo_tarefa` | Percentis de RF-016. Sem ele, o plano é varredura do recorte mais ordenação por duração calculada |
| `(tarefa_id, tipo)` parcial em `fim IS NULL` | `intervalo_tarefa` | Intervalos abertos; único, ver §5 |
| `(tarefa_id, episodio)` | `intervalo_tarefa` | Soma por episódio de RN-019 (SCN-013.3) |
| `(tarefa_id)` parcial em `desfecho IS NULL` | `impedimento` | Impedimento aberto; único, ver §5 |
| `(projeto_id)` parcial em `desfecho IS NULL` | `impedimento` | Listas de impedimento aberto de RF-014 e RF-015, e a marca de impedimento do board de RF-003 — dimensão 3 é derivada, então toda leitura que a exibe passa por aqui. **Não** é único |
| `(projeto_id, seq)` único | `evento_tarefa` | Resincronização por `seq` do ADR-004. Único porque duplicata torna a lacuna indetectável — SDR-004 |
| `(tarefa_id, id)` | `evento_tarefa` | Histórico da tarefa e reconstrução da projeção |
| `(usuario_id, projeto_id)` único | `participacao` | Checagem de autorização por projeto (BDR-001) **e percurso da relação de RF-002**. Nota de 2026-09-11 (ACH-15): a relação parte de `usuario_id` e o prefixo deste índice a serve. Isso passa a ser **decisão declarada** e não coincidência — nenhum índice adicional em `(usuario_id)` é criado, porque seria duplicata do prefixo e custo de escrita sem ganho de leitura. A consequência é que **encolher esta restrição para `(projeto_id, usuario_id)` degrada a rota mais quente da navegação**, e quem o fizer precisa criar o índice de `usuario_id` no mesmo passo. Os papéis são alcançados pelo prefixo da chave primária de `participacao_papel` |
| `(projeto_id, ordem)` único parcial em `arquivada_em IS NULL` | `etapa` | Adjacência de RN-005 sem ambiguidade de ordem |

**Leituras que precisam ser consulta única, não navegação de entidade.** As duas
listas de impedimento aberto (RF-014 e RF-015) e o board de RF-003 montam cada
item com campos de três tabelas. Carregá-los navegando associação JPA é o N+1 que
`backend/java/database.md` §2 e §3 mandam evitar, e o volume relevante aqui não é
o de 5.000 tarefas: é o de impedimentos abertos, que cresce sem teto porque
RN-026 garante que nada expira sozinho. As três leituras se declaram como
projeção para `record` de consulta, com join explícito.

---

## 7. Ciclo de vida da tarefa

Três dimensões simultâneas, conforme RN-002 (PRD v1.1). A condição não é etapa, a
etapa não é condição, e a marca de impedimento não é nenhuma das duas.

**Dimensão 2 — condição:**

```
                    criar (RF-004)
                          │
                          ▼
                 AGUARDANDO_TOMADA ◄──── devolver (RF-008)
                    │        ▲           remover participação (RN-027)
         assumir    │        │           mover (RF-005, RF-006)
         (RF-007)   ▼        │           reabrir (RF-013)
                 EM_CURSO ───┘

  EM_CURSO ── chegar a etapa terminal (RF-011) ──► CONCLUIDA ── reabrir (RF-013) ─┐
                                                                                  │
  qualquer condição ativa ── encerrar (RF-012) ──► ENCERRADA_SEM_CONCLUSAO  (fim)  │
                                                                                  │
                                        primeira etapa, AGUARDANDO_TOMADA ◄───────┘
                                        episódio + 1 (RN-034)
```

**Dimensão 3 — marca de impedimento**, independente da anterior:

```
        sem impedimento aberto ── impedir (RF-009) ──► com impedimento aberto
                    ▲                                          │
                    └──── registrar desfecho (RF-010) ─────────┘
```

- Nenhuma aresta da dimensão 2 altera a dimensão 3, e vice-versa: mover, devolver,
  assumir, renomear e remover participação preservam a marca (RN-032), e abrir ou
  resolver impedimento preserva a condição.
- `ENCERRADA_SEM_CONCLUSAO` é terminal absoluto (RN-018, SCN-012.3): não há aresta
  de saída, e a tentativa de mover é recusada.
- `CONCLUIDA` só sai por reabertura do `product_owner` (RN-017); a saída
  incrementa o episódio e devolve a tarefa à **primeira etapa** do fluxo (RN-034).
- Conclusão **e** encerramento sem conclusão com impedimento aberto são
  recusados (RN-011, SCN-011.3, SCN-012.4) — são as duas únicas leituras cruzadas
  entre as dimensões, e valem nos três caminhos descritos em
  `contracts/board-e-tarefas.md`. Por isso `TAREFA_ENCERRADA_SEM_CONCLUSAO` não
  precisa fechar `IMPEDIMENTO`: o evento nunca é gravado com a marca acesa.
- A tarefa com impedimento aberto preserva a etapa e admite transição de etapa
  (RN-009), e pode ser assumida (RN-033, SCN-007.4).
- Nenhuma aresta é disparada por decurso de prazo (RN-026). Não existe agendador
  neste sistema, e essa ausência é decisão, não omissão.

---

## 8. Migrations

Flyway, dialeto PostgreSQL (ADR-005, ADR-009). Nomenclatura de
`backend/java/database.md` §1: `V<ANO><MES><DIA><HORA>__descricao_clara.sql`.
Migration aplicada nunca é alterada — corrige-se com uma nova.

**Quem executa não é a aplicação.** Por ADR-011, o Flyway roda em serviço
dedicado do `compose`, que migra até o fim e sai; as instâncias sobem com
`ddl-auto=validate` e sem permissão de migrar. O teste de RNF-002 sobe três
instâncias simultâneas, e migrar no boot as poria em disputa pelo lock. A
consequência aceita é que aplicação contra banco desatualizado **falha** em vez
de se autocorrigir — que é o comportamento correto.

| Ordem | Conteúdo |
| --- | --- |
| 1 | `usuario`, `projeto` — **inclusive `seq_atual`** —, `participacao`, `participacao_papel` e seus índices |
| 2 | `etapa`, `raia`, restrição de ordem única por projeto |
| 3 | `tarefa`, restrição de verificação sobre `condicao`, índices do board e da fila |
| 4 | `evento_tarefa` e a concessão restrita de `SELECT, INSERT` à role de aplicação |
| 5 | `intervalo_tarefa`, `impedimento` e os índices únicos parciais |
| 6 | unicidade de `(projeto_id, seq)` e a coluna gerada `duracao` com seu índice |
| 7 | `ALTER TABLE usuario ALTER COLUMN email DROP NOT NULL` |

A ordem 7 existe porque a 1 já foi aplicada pela TASK-01.3, e migration aplicada
não se altera — corrige-se com uma nova. Ela é barata (`DROP NOT NULL` não
reescreve a tabela) e pode ser executada a qualquer momento antes de a rota de
sessão entrar em uso com realm federado.

**Correção de 2026-09-10 (ACH-02 da revisão de TASK-01.3).** `projeto.seq_atual`
estava listado na ordem 6 e nasce na 1, que é onde a TASK-01.3 corretamente o
criou — a coluna é declarada em §3 como parte de `projeto`, e `POST /v1/projetos`
insere o projeto já com `seq_atual = 0`. Quem implementasse a migration 6 seguindo
a tabela antiga escreveria `ADD COLUMN` sobre coluna existente, e a migração
falharia contra qualquer banco já migrado — no serviço dedicado, com o backend
parado esperando por `service_completed_successfully` (ADR-011). A decisão estava
certa desde a task; faltava propagá-la para cá.

**Não há gatilho de `NOTIFY` no banco.** A versão anterior desta tabela previa um,
e ele concorria com a publicação por `EventoBoardPublisher` em `afterCommit` que a
TechSpec Seção 5 e o ADR-004 prescrevem — dois publicadores para o mesmo evento
fariam cada mudança gerar dois broadcasts, e o cliente contabilizaria `seq`
repetido como estado inconsistente. SDR-004 escolhe a porta de aplicação e trata
a janela de perda que ela abre.

`spring.jpa.hibernate.ddl-auto=validate` em todo perfil, inclusive teste.
Testcontainers roda as mesmas migrations, na mesma imagem do `compose` (SDR-003):
o esquema sob teste é o esquema de produção, não uma aproximação.

---

## 9. Reconstrução da projeção

A alegação de que a projeção é descartável só vale se houver como refazê-la.

A rotina lê `evento_tarefa` em ordem de `id`, por projeto, e reescreve `tarefa`,
`intervalo_tarefa` e `impedimento` do zero. É idempotente e não toca no anel de
verdade.

**Exige janela sem escrita no projeto.** Reconstruir concorrentemente com escritas
novas produz projeção divergente do log — o defeito exato que a rotina existe para
curar. A execução toma bloqueio consultivo por projeto (`pg_advisory_xact_lock`
sobre o `id`), e escrita que chegue durante a janela recebe `409`. É operação
administrativa e rara; tratá-la como online custaria mais do que a raridade
justifica.

O teste que a sustenta: executar um percurso completo de tarefa, capturar a
projeção, reconstruir, comparar. Divergência reprova. Sem esse teste, "a projeção
é reconstruível" é afirmação sem lastro — e a divergência silenciosa entre log e
projeção é o risco que SDR-001 assume ao escolher este desenho.
