# TASK-02.10 — Ficha da tarefa: leitura de detalhe com o log

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-02.6
- **Cenários cobertos:** nenhum próprio — os cenários que atravessam esta rota
  já estão atribuídos aos épicos que os entregam, e duplicá-los aqui quebraria a
  invariante cenário↔épico nos dois sentidos
- **Origem:** `contracts/board-e-tarefas.md` §`GET /v1/tarefas/{tarefaId}`,
  `contracts/fila-e-consultas.md` (episódios), RN-008, RN-015, RN-019, RNF-008

#### Contexto

Esta task existe porque a rota **não tinha dono** — descoberto em 2026-09-15 na
execução de TASK-02.6 (ACH-02). O contrato a declara desde a v1.0 da TechSpec, a
suíte congelada a exercita em **onze classes de teste**, e nenhuma task do plano
a distribuía: `state.md` a tratava como superfície de TASK-02.6, que entrega só o
board.

Ela é a segunda superfície de leitura e, ao contrário do board, é a que o cliente
usa **quando perdeu o estado**: é dela que sai a `origem` que SDR-002 exige de
toda escrita seguinte. O board mostra a grade; a ficha mostra uma tarefa e o que
aconteceu com ela.

A leitura do log é o que a distingue do cartão. `GET /board` devolve estado
corrente; a ficha devolve estado corrente **mais** a série de eventos em ordem
cronológica, com o episódio de cada um — e é por isso que ela é a única leitura
que torna verificável a imutabilidade prometida por RNF-008 (`ParticipacaoIT`
SCN-019.3 confere que remover participação não apaga que a tomada aconteceu).

**Esta task é pré-condição da medição de três tasks já implementadas:** os
critérios 1, 3, 6, 7 e 8 de TASK-02.5 (ACH-04) e 1 e 3 de TASK-02.4 (ACH-05) não
são mensuráveis sem ela, porque as classes que os exercitariam morrem em `404`
nesta rota.

#### O que deve ser feito

- [ ] Implementar `GET /v1/tarefas/{tarefaId}`.
- [ ] Devolver o cartão completo, na mesma forma fixada por TASK-02.5, acrescido
      de `etapaNome` e `episodioAtual`.
- [ ] Devolver `log` — todo evento da tarefa em ordem cronológica crescente, com
      `tipo`, `episodio`, `ator` e `motivo`.
- [ ] Omitir `ator` quando o evento não tem ator (devolução por perda de
      participação).
- [ ] Devolver `404` para tarefa inexistente **e** para tarefa em projeto sem
      participação de quem chama.
- [ ] Montar a resposta com número fixo de consultas, sem consulta por evento.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/TarefaController.java` | alterar | acrescenta a rota de leitura; a de criação já existe |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/FichaQuery.java` | criar | consultas de projeção e do log, no padrão de `BoardQuery` |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/FichaResposta.java` | criar | registro de saída; compõe `CartaoResposta` em vez de duplicá-lo |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/EventoResposta.java` | criar | item do `log` |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/CartaoResposta.java` | alterar, se necessário | só se a composição exigir; **não** mudar a forma que o board e a criação já emitem |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

`GET /v1/tarefas/{tarefaId}`

- **Saída `200`:** o cartão de TASK-02.5 —
  `{ id, etapaId, titulo, condicao, raiaId, responsavel, versao, esperaTomada, impedimento, permanencia }`
  — acrescido de `etapaNome`, `episodioAtual` e `log`.
- **`etapaNome`** é o nome vigente da etapa, **lido no instante da leitura** e não
  o nome que a etapa tinha quando a tarefa entrou nela. Renomear etapa não é
  movimentação (RN-009): `ConfiguracaoDoFluxoIT` confere que depois de renomear a
  ficha traz `etapaNome` novo e `permanencia.decorrido` **não** reiniciado.
- **`episodioAtual`** é o número do episódio em curso — 1 até a primeira
  reabertura, 2 depois dela (RN-019).
- **`log`:** `[ { tipo, episodio, ocorridoEm, ator: { id, nome } | ausente, motivo | ausente } ]`,
  em ordem **cronológica crescente** — `log[0]` é `TAREFA_CRIADA` e `log[-1]` é o
  último evento. A suíte congelada indexa pelas duas pontas.
- **`ator` ausente, não nulo.** `ParticipacaoIT` afirma
  `$.log[-1].ator` com `doesNotExist()`: é omissão de chave, não `null`.
- **`permanencia` pode vir nula** — tarefa em condição terminal não tem intervalo
  de permanência aberto. Idem `esperaTomada` e `impedimento`.
- **`404`** tarefa inexistente ou em projeto sem participação — nunca `403`, pela
  regra única de `sessao-e-projetos.md`. Participante sem papel algum **lê**: a
  leitura não exige papel além da participação (RN-015).

#### Guia técnico — pontos de atenção

- **A forma do cartão é a de TASK-02.5, e ela não se bifurca.** Três superfícies
  emitem o mesmo objeto — criação, board e ficha —, e a razão está registrada em
  ACH-07 da revisão de TASK-02.5: é dele que o cliente monta a `origem` de
  SDR-002. Uma segunda forma reintroduz o acoplamento que `etapaId` no cartão
  existe para eliminar. **Componha `CartaoResposta`; não o copie.**
- **O log vem do anel de verdade, nunca da projeção.** `evento_tarefa` é a fonte;
  `intervalo_tarefa` alimenta os três blocos de tempo e nada mais. Derivar o
  histórico dos intervalos perderia os eventos que não abrem intervalo.
- **Nenhum campo soma as séries.** Vale aqui a mesma regra do board: a ficha
  exibe as três dimensões em campos separados, e RN-008 proíbe o total.
- **`decorrido` é calculado a partir de `desde` no instante da leitura.**
- **Consulta por evento estoura o envelope.** O número de consultas não cresce
  com o tamanho do log.
- **O nome do ator vem por junção**, não por consulta dentro do laço, e sai
  mesmo para quem já não participa do projeto — é o que SCN-019.3 exige, e é
  também a razão de o log não guardar o nome: ele guarda o `ator_id`, e o nome é
  espelho do token resolvido na leitura.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | A ficha devolve o cartão na mesma forma que a criação e o board emitem | comparação campo a campo das três respostas para a mesma tarefa |
| 2 | `log` traz os eventos em ordem cronológica crescente, com `TAREFA_CRIADA` em `log[0]` | leitura após percurso conhecido |
| 3 | Cada item do log traz o episódio a que pertence, e a reabertura incrementa `episodioAtual` | percurso com reabertura |
| 4 | Evento sem ator omite a chave `ator` em vez de devolvê-la nula | devolução por perda de participação |
| 5 | Renomear a etapa muda `etapaNome` e **não** reinicia `permanencia.decorrido` | `PUT /etapas` seguido de leitura |
| 6 | Espera e impedimento simultâneos vêm nos dois blocos, sem soma | leitura de tarefa impedida e aguardando tomada |
| 7 | Tarefa em projeto sem participação devolve `404`, e tarefa inexistente também | duas requisições |
| 8 | O número de consultas ao banco não cresce com o tamanho do log | contagem com 3 e com 30 eventos |
| 9 | O log exibe evento de quem já não participa do projeto, com o nome | remoção de participação seguida de leitura |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-16 | criação | Task nascida de **ACH-02 da revisão de TASK-02.6**: a rota é declarada pelo contrato e exercitada por onze classes da suíte congelada, e não pertencia a task nenhuma. Criada em EPIC-02 e não em EPIC-03 porque é ela que torna mensuráveis os critérios já em aberto de TASK-02.4 (ACH-05) e TASK-02.5 (ACH-04), ambos do mesmo épico — e porque `TASK-03.5`, a ficha do frontend, a pressupõe pronta. **Não declara cenário:** os cenários que a atravessam já estão atribuídos aos épicos que os entregam, e duplicá-los aqui quebraria a invariante cenário↔épico nos dois sentidos |
