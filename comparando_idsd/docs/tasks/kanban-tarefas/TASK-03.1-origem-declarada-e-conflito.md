# TASK-03.1 — Envelope de origem declarada e conflito com o estado atual

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** nenhuma
- **Cenários cobertos:** SCN-005.3, SCN-007.2
- **Origem:** SDR-002, RN-012, RN-013, RN-031

#### Contexto

Toda escrita sobre tarefa declara de que estado ela parte. Esse mecanismo único
resolve concorrência e idempotência de uma vez: se o estado mudou, quem chega
depois é recusado e recebe **o que** aconteceu; se o efeito já está aplicado, a
solicitação é absorvida sem novo evento. Ele é criado aqui e reutilizado por
todas as operações dos épicos seguintes.

#### O que deve ser feito

- [ ] Definir o bloco de origem declarada e exigi-lo em toda escrita sobre
      tarefa, exceto a criação.
- [ ] Reavaliar a origem contra o estado corrente dentro da transação, sob
      bloqueio otimista.
- [ ] Devolver `409` com o bloco de estado atual quando houver divergência.
- [ ] Devolver `200` sem novo evento quando o efeito já estiver aplicado.
- [ ] Não implementar cabeçalho de chave de idempotência.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/OrigemDeclarada.java` | criar | registro de entrada compartilhado |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/VerificadorDeOrigem.java` | criar | reavaliação e decisão entre `409` e `200` |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/EstadoAtualResposta.java` | criar | bloco devolvido no `409` |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/RegistradorDeEvento.java` | alterar | passa a exigir a verificação antes de aplicar |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

Bloco de origem, presente no corpo de toda escrita sobre tarefa:

`"origem": { "etapaId": "<uuid>", "condicao": "<...>", "versao": 0 }`

Corpo do `409`:

`{ "type", "title", "status": 409, "detail", "traceId", "estadoAtual": { "etapaId", "etapaNome", "condicao", "responsavel": { "id", "nome" }, "assumidaEm", "versao" } }`

O bloco de estado atual não é enfeite: os cenários de concorrência exigem que
quem perdeu saiba **o que** aconteceu, não apenas que houve erro. Um `409` sem
esse bloco reprova os cenários.

Idempotência: solicitação cuja origem coincide com o estado corrente **e** cujo
efeito já está aplicado é absorvida sem novo evento e devolve `200` com o estado
atual. Não há cabeçalho de chave de idempotência.

Códigos comuns das rotas de tarefa: `400` corpo malformado; `401` sem token;
`403` sem permissão; `404` tarefa inexistente ou em projeto sem participação;
`422` regra de negócio violada; `409` origem divergente; `429` com `Retry-After`.

#### Guia técnico — pontos de atenção

- **A origem declarada é estritamente mais forte que uma chave de idempotência.**
  Ela também detecta a mudança feita por terceiro, e é por isso que a chave foi
  recusada. Não a acrescente "por compatibilidade".
- **A reavaliação acontece dentro da transação**, depois de carregar a tarefa sob
  bloqueio otimista. Verificar antes de abrir a transação deixa a janela aberta.
- **Divergência e efeito já aplicado são casos distintos** e a ordem da decisão
  importa: primeiro verifique se o efeito pedido já está aplicado; só então
  compare a origem.
- **`versao` vem do cliente e nunca é confiada como autoridade** — ela é insumo
  da comparação, não da autorização.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | Escrita com origem divergente recebe `409` com o estado atual completo | inspeção do corpo, incluindo responsável e versão |
| 2 | Repetir uma operação cujo efeito já está aplicado devolve `200` sem novo evento | contagem de eventos antes e depois |
| 3 | Duas escritas simultâneas sobre a mesma tarefa produzem uma vencedora e um `409` | teste de concorrência |
| 4 | Escrita sem o bloco de origem é recusada com `400` | requisição sem o bloco |
| 5 | Nenhuma rota aceita cabeçalho de chave de idempotência | inspeção do contrato e do código |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
