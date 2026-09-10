# Contrato — board e tarefas

_Cobre RF-003 a RF-013 | Base: `/v1`_
_Normas: `_shared/api-standards.md`, SDR-002, SDR-001_

---

## O envelope de escrita

**Toda** rota de escrita sobre tarefa carrega o estado de origem declarado, e a
resposta de conflito devolve o estado corrente. É SDR-002 materializado, e vale
sem exceção — não existe endpoint de escrita neste contrato que fuja da forma
abaixo.

**Entrada — bloco comum a toda escrita:**

```
"origem": { "etapaId": "<uuid>", "condicao": "<CONDICAO>", "versao": <bigint> }
```

**Resposta `409 Conflict`:**

```
{
  "type": "https://errors.idsd/estado-divergente",
  "title": "A tarefa mudou desde que você a leu",
  "status": 409,
  "detail": "<o que mudou, em linguagem de negócio>",
  "instance": "/v1/tarefas/<id>",
  "traceId": "<...>",
  "estadoAtual": { "etapaId", "etapaNome", "condicao", "responsavel": { "id", "nome" }, "assumidaEm", "versao" }
}
```

`estadoAtual` não é enfeite: SCN-005.3, SCN-007.3 e SCN-020.3 exigem que quem
perdeu saiba **o que** aconteceu, não que houve um erro. Um `409` sem esse bloco
reprova os três cenários.

**Idempotência (RN-031):** solicitação cuja origem declarada coincide com o
estado corrente **e** cujo efeito já está aplicado é absorvida sem novo evento e
devolve `200` com o estado atual. É o que SCN-007.2 e SCN-010.3 verificam. Não há
header `Idempotency-Key`.

**Códigos comuns:** `400` corpo malformado; `401` sem token; `403` sem permissão
(RN-015); `404` tarefa inexistente ou em projeto sem participação; `422` regra de
negócio violada; `409` estado de origem divergente; `429` com `Retry-After` ao
exceder o limite de requisições (TechSpec Seção 8).

---

## `GET /v1/projetos/{projetoId}/board` — RF-003

- **Saída `200`:** `{ seq, etapas: [ { id, nome, ordem, terminal, raias: [ { id, nome, tarefas: [ <cartão> ] } ] } ] }`.
- **Cartão:** `{ id, titulo, condicao, raiaId, responsavel, versao, esperaTomada: { desde, decorrido } | null, impedimento: { desde, decorrido, motivo } | null, permanencia: { desde, decorrido } }`.
- Etapa sem tarefa vem na lista com `tarefas: []` — SCN-003.2 exige exibir a etapa
  vazia, não omiti-la.
- `esperaTomada` e `impedimento` podem estar **ambos** preenchidos e a soma nunca
  é calculada nem devolvida (RN-008, SCN-003.3).
- **`condicao` nunca vale `IMPEDIDA`.** O impedimento é a terceira dimensão e sai
  no bloco `impedimento` do cartão, que coexiste com qualquer condição não
  terminal (RN-002, RN-032). O cliente lê as três dimensões em campos separados:
  a etapa pela posição na lista, `condicao`, e `impedimento != null`.
- `seq` é o último número de sequência do projeto, e é o que o cliente compara com
  o `seq` recebido por WebSocket para detectar lacuna (ADR-004).

## `POST /v1/projetos/{projetoId}/tarefas` — RF-004

- **Entrada:** `{ titulo, descricao?, raiaId? }`. Sem bloco de origem: não há
  estado anterior.
- **Saída `201`** com `Location`, corpo com o cartão.
- Nasce em `AGUARDANDO_TOMADA`, na etapa de menor `ordem`, sem responsável
  (RN-004, RN-006).
- **Erros:** `422` título ausente ou em branco (SCN-004.2); `422` projeto sem
  fluxo configurado, com `detail` orientando configurar as etapas antes
  (SCN-004.3).

## `POST /v1/tarefas/{tarefaId}/movimentos` — RF-005 e RF-006

Um único endpoint para avanço, retrocesso e retorno ao início: as três são a mesma
operação sob RN-005, e separá-las duplicaria a validação de alcançabilidade.

- **Entrada:** `{ origem, etapaDestinoId }`.
- **Saída `200`:** o cartão resultante.
- **Efeito:** fecha `PERMANENCIA` e `ESPERA_TOMADA` na origem, abre as duas no
  destino, zera `responsavel_id` e volta a condição a `AGUARDANDO_TOMADA`
  (SCN-005.1, SCN-006.1). O impedimento aberto — marca e intervalo — **não é
  tocado**, e o cartão devolvido continua exibindo `impedimento` preenchido
  (RN-009, RN-032, SCN-006.3). Só o registro do desfecho apaga a marca; nenhuma
  outra operação o faz, e o esquema garante isso porque a marca não é coluna de
  `tarefa` (ver `data-model.md` §5).
- **Erros:**
  - `422` destino não alcançável — não é a etapa seguinte, nem a anterior, nem a
    primeira do fluxo (RN-005, SCN-005.2).
  - `422` tarefa em `CONCLUIDA` ou `ENCERRADA_SEM_CONCLUSAO` (RN-018, SCN-012.3).
  - `409` origem divergente (SCN-005.3).
- **Etapa terminal:** mover para etapa `terminal` **conclui** a tarefa — é o
  caminho de RF-011 (SCN-011.1), não uma operação à parte.

## `POST /v1/tarefas/{tarefaId}/tomada` — RF-007

- **Entrada:** `{ origem }`.
- **Saída `200`:** cartão com `responsavel` e `assumidaEm`; fecha `ESPERA_TOMADA`
  (SCN-007.1).
- Repetir por quem já assumiu: `200`, sem novo evento, `assumidaEm` inalterado
  (SCN-007.2).
- `409` quando outra pessoa assumiu antes, com `estadoAtual.responsavel`
  preenchido (SCN-007.3, SCN-020.3).
- **Impedimento aberto não é pré-condição.** A tomada é aceita, a marca
  permanece e quem assume assume também o trabalho de destravar (RN-033,
  SCN-007.4). Não há verificação a fazer: a marca não vive em `tarefa`.

## `DELETE /v1/tarefas/{tarefaId}/tomada` — RF-008

- **Entrada:** `{ origem }` no corpo.
- **Saída `200`:** volta a `AGUARDANDO_TOMADA` na mesma etapa e abre **nova**
  `ESPERA_TOMADA` (RN-030, SCN-008.1).
- `403` quando quem pede não é o responsável registrado (SCN-008.2).
- Impedimento aberto permanece aberto com a contagem em curso (SCN-008.3).

## `POST /v1/tarefas/{tarefaId}/impedimentos` — RF-009

- **Entrada:** `{ origem, motivo }`.
- **Saída `201`** quando abre; **`200`** quando já havia impedimento aberto e a
  informação foi anexada.
- **A condição não muda.** Abrir impedimento acende a dimensão 3 e nada mais: a
  tarefa em curso continua em curso, a que aguardava tomada continua aguardando,
  e as duas contagens seguem correndo em paralelo à de impedimento (RN-002,
  RN-008).
- Já impedida: nenhum segundo impedimento é criado, o motivo entra em `anotacoes`
  e a contagem **não** reinicia (RN-010, SCN-009.3). O índice único parcial do
  `data-model.md` §5 garante isso mesmo sob concorrência.
- `422` motivo ausente ou em branco (SCN-009.2).
- Após aceitar, o impedimento se destaca para quem tem a permissão de desbloqueio
  no projeto; sem ninguém com ela, para todos os participantes (RN-024, BDR-002,
  SCN-019.3).

## `POST /v1/tarefas/{tarefaId}/impedimentos/{impedimentoId}/resolucao` — RF-010

- **Entrada:** `{ origem, desfecho }`.
- **Saída `200`:** fecha o intervalo de `IMPEDIMENTO` e apaga a marca. A
  condição e a etapa ficam como estiverem — não há condição a restaurar, porque
  a abertura do impedimento nunca a alterou (RN-032, SCN-010.1).
- **Autorizado a:** quem tem permissão de desbloqueio **ou** quem abriu o
  impedimento (SCN-010.2). A segunda metade é regra de serviço sobre
  `impedimento.aberto_por`, não permissão de papel.
- Já resolvido: `200`, nada alterado, tempo registrado inalterado (SCN-010.3).

## `POST /v1/tarefas/{tarefaId}/encerramento` — RF-012

- **Entrada:** `{ origem, motivo }`.
- **Saída `200`:** condição `ENCERRADA_SEM_CONCLUSAO`, intervalos de
  `PERMANENCIA` e `ESPERA_TOMADA` fechados, histórico preservado (SCN-012.1).
  Não há intervalo de `IMPEDIMENTO` a fechar aqui, porque o encerramento com
  impedimento aberto nunca é aceito — ver abaixo.
- **Exige permissão de configuração** (RN-016). `403` para participante comum,
  sem alterar a condição (SCN-012.2).
- **`422` com impedimento aberto** (RN-011, SCN-012.4), com `detail` exigindo o
  desfecho registrado antes. A condição não é alterada e o impedimento permanece
  aberto. É a segunda das duas únicas restrições que a marca impõe, e a razão é a
  de B-04: fechar o intervalo em cascata atribuiria ao sistema um desfecho que
  ninguém afirmou, e o agregado passaria a conter esse número.
- Estado terminal absoluto: nenhuma rota o reverte (RN-018).

## `POST /v1/tarefas/{tarefaId}/reabertura` — RF-013

- **Entrada:** `{ origem, motivo }`.
- **Saída `200`:** volta a `AGUARDANDO_TOMADA` na primeira etapa do fluxo, sem
  responsável e **incrementando o episódio** (RN-019, RN-034, SCN-013.1). A etapa
  de retorno deixou de ser decisão deste contrato — INC-04 apontou que é decisão
  de negócio com efeito na medição, e RN-034 a fixa no PRD v1.1.
- **Privativo de `product_owner`** (RN-017). `403` para qualquer outro papel, e a
  tarefa permanece concluída (SCN-013.2).
- `422` se a tarefa não está `CONCLUIDA` — `ENCERRADA_SEM_CONCLUSAO` não reabre.

## `GET /v1/tarefas/{tarefaId}` — detalhe

Cartão completo mais o histórico do log, em ordem cronológica, com o episódio de
cada evento. É a leitura que precede qualquer escrita quando o cliente perdeu o
estado.

## `POST /v1/tarefas/{tarefaId}/conclusao` — RF-011

A transição continua sendo efeito de mover para etapa terminal (SCN-011.1): esta
rota **não** é um segundo caminho para concluir. Ela existe porque SCN-011.2
congela uma recusa — "tento marcá-la como concluída" na etapa Review — e recusa
exige superfície a exercitar. A versão anterior deste contrato decidia que não
havia operação de conclusão, e o cenário ficava sem alvo: era o achado INC-07.

- **Entrada:** `{ origem }`.
- **Saída `200`:** o cartão. Só ocorre quando a tarefa **já está** `CONCLUIDA` —
  é a resposta idempotente de RN-031, sem novo evento.
- **Erros:**
  - `422` tarefa fora de etapa terminal, com `detail` explicando que a conclusão
    se dá ao alcançar uma etapa terminal (SCN-011.2). Nada é alterado.
  - `422` impedimento aberto, com `detail` exigindo desfecho registrado antes; o
    impedimento permanece aberto (RN-011, SCN-011.3).
  - `409` origem divergente (SDR-002).

Nenhum caminho desta rota transiciona a condição. Ou a tarefa já chegou à etapa
terminal e a conclusão já aconteceu, ou a resposta é recusa — e é exatamente essa
assimetria que o `detail` de SCN-011.2 comunica ao usuário.

**A mesma recusa de RN-011 vale na movimentação e no encerramento:** mover para
etapa terminal com impedimento aberto devolve `422` com o mesmo `detail`, e o
impedimento permanece aberto (SCN-011.3); encerrar sem conclusão devolve `422`
pela mesma verificação (SCN-012.4). Os três caminhos a compartilham; o serviço a
implementa uma vez. O `detail` do encerramento difere apenas na operação
nomeada — a condição de recusa é a mesma.
