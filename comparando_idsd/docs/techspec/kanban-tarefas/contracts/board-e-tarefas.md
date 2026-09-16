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
(RN-015); `404` tarefa inexistente ou em projeto sem participação — a regra única
que separa os dois está em `sessao-e-projetos.md`, e o caso do participante sem
papel algum é `403`; `422` regra de
negócio violada; `409` estado de origem divergente; `429` com `Retry-After` ao
exceder o limite de requisições (TechSpec Seção 8).

---

## `GET /v1/projetos/{projetoId}/board` — RF-003

- **Saída `200`:** `{ seq, acessoPorAdministracaoGlobal, etapas: [ { id, nome, ordem, terminal, raias: [ { id, nome, tarefas: [ <cartão> ] } ] } ] }`.
- **Cartão:** `{ id, etapaId, titulo, condicao, raiaId, responsavel, assumidaEm, versao, esperaTomada: { desde, decorrido } | null, impedimento: { desde, decorrido, motivo } | null, permanencia: { desde, decorrido } | null }`.
- **`acessoPorAdministracaoGlobal` é do board e não só da relação de projetos.**
  A marca já existia em `GET /v1/projetos` e no detalhe (`sessao-e-projetos.md`),
  e faltava aqui — mas a suíte congelada a exige **sobre esta rota**:
  `AdminGlobalIT` afirma a marca e a grade na **mesma** requisição ao board.
  A razão é a mesma que a instituiu lá: quem chega ao board por administração
  global e não por participação está vendo trabalho alheio, e RN-035 manda que
  o alcance seja visível na resposta e não deduzido pelo cliente. Deduzi-lo no
  board seria pior que na relação, porque ali o cliente teria de correlacionar
  duas respostas de rotas diferentes para saber em que condição está lendo.
- **`assumidaEm` é do cartão** e não da saída da tomada. A versão anterior deste
  contrato descrevia o cartão sem o campo e o prometia em dois outros lugares —
  na saída de `POST .../tomada` e em `estadoAtual` do `409` —, o que só é
  coerente se ele pertencer ao cartão, porque nos dois casos o corpo **é** o
  cartão. `TomadaIT` o afirma sobre o cartão devolvido, e `tarefa.assumida_em`
  existe no esquema desde a primeira migration do anel de projeção. A omissão
  era do contrato.
- **`permanencia` é anulável**, como as outras duas séries. A marcação sem
  `| null` era acidente de redação: a permanência é intervalo **aberto**, e
  tarefa em condição terminal não tem nenhum — `ReaberturaIT` lê exatamente esse
  cartão na etapa terminal. Declarar o bloco obrigatório obrigaria o board a
  fabricar uma permanência que não corre, e um `decorrido` que cresce depois da
  conclusão é o tipo de número que ninguém desconfia até somá-lo.
- **`etapaId` é do cartão e não da posição na lista** (ACH-07 da revisão de
  TASK-02.5). No board a etapa é dedutível do aninhamento, mas o cartão é o
  mesmo objeto devolvido por `POST /tarefas`, pelos movimentos e por
  `GET /tarefas/{id}`, onde não há lista alguma — e é dele que o cliente monta
  a `origem` de SDR-002, que exige `etapaId` na escrita seguinte. Sem o campo,
  a única escrita possível depois de criar uma tarefa seria precedida de uma
  releitura do board. A suíte congelada já o exige (`CriacaoDeTarefaIT` afirma
  `$.etapaId`), de modo que a omissão era do contrato e não da implementação.
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

### A grade tem duas faixas sintéticas, e é a mesma regra nos dois eixos

A grade é o produto cartesiano **etapa × raia**, e nenhum dos dois eixos é
total: a raia é opcional por RN-023, e a etapa é arquivável logicamente por
RN-021. Em ambos os casos existe cartão que não cabe em célula alguma, e cartão
que não cabe **some do board sem erro** — a pior forma de perder trabalho em
curso, porque não há sintoma a investigar.

| Eixo | Faixa sintética | Quem cai nela |
| --- | --- | --- |
| Raia | `id: null`, `nome: "Sem raia"`, última da etapa | cartão sem `raiaId`, ou apontando para raia arquivada |
| Etapa | `id: null`, `nome: "Fora do fluxo"`, `ordem` após a última, `terminal: false`, **última da lista** | cartão cuja `etapaId` aponta para etapa arquivada |

**A célula da etapa arquivada é alcançável por operação permitida**, e não por
defeito: `PUT .../etapas` só recusa arquivar etapa com tarefa **ativa**, e a
contagem exclui declaradamente `CONCLUIDA` e `ENCERRADA_SEM_CONCLUSAO`. Arquivar
uma etapa terminal com histórico é, portanto, aceito — e sem a coluna sintética
todo o trabalho que passou por ela desaparece da tela no instante do
arquivamento.

**Regra única das duas faixas: a sintética existe quando, e só quando, há ao
menos um cartão que precise dela.** Faixa sintética vazia seria etapa ou raia
que ninguém configurou sendo apresentada como se existisse, contra SCN-003.2,
que trata a etapa vazia como informação sobre o **fluxo**. A consequência a
declarar é que o comprimento das listas varia com os dados, e cliente e
verificação indexam por `id` e nunca por posição.

A faixa sintética é **de leitura**. Nada nela é destino de escrita: mover um
cartão para fora dela é movimentação comum, porque a `origem` de SDR-002 carrega
a `etapaId` real e arquivada que o cartão sempre teve, e o destino é etapa
vigente alcançável por RN-005.

### O recorte de terminais — RN-039 e RNF-011

O board devolve tarefa **terminal** apenas enquanto o desfecho tiver ocorrido nos
últimos **30 dias**. As mais antigas saem da tela e continuam integralmente
acessíveis por `GET /v1/tarefas/{tarefaId}` e pelas consultas de andamento e de
tempo por etapa — **o recorte é da tela, nunca do registro**: nenhum evento é
apagado e nenhum intervalo é fechado por causa dele (RN-022, RNF-008).

- O corte é aplicado **na consulta de tarefas**, não na montagem em memória.
  Filtrar depois de trazer tudo cumpriria RN-039 e não cumpriria RNF-011, que é
  a razão pela qual a regra existe.
- A janela é contada do instante do desfecho até o instante da leitura, e o
  instante da leitura é o único da transação (`readOnly`), para que dois cartões
  do mesmo board nunca sejam julgados por relógios diferentes.
- Tarefa **não terminal** nunca é recortada, por mais antiga que seja: trabalho
  parado é justamente o que o board precisa mostrar.
- Reabrir uma tarefa (RF-013) a devolve ao board pela porta comum — ela deixa de
  ser terminal e o recorte não a alcança mais.
- **Envelope (RNF-011):** p95 ≤ 2 s com 5.000 tarefas no projeto, com o recorte
  em vigor, medido pelo mesmo arnês de carga sintética de RNF-009.

O instante do desfecho é projetado em `tarefa.tornou_se_terminal_em`
(`data-model.md` §5) e não derivado em tempo de leitura — ver **SDR-007**.

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
