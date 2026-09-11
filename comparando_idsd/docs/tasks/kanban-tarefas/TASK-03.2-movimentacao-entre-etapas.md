# TASK-03.2 — Movimentação entre etapas

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-03.1
- **Cenários cobertos:** SCN-005.1, SCN-005.2, SCN-005.3, SCN-006.1, SCN-006.2
- **Origem:** RF-005, RF-006, RN-005, RN-009, RN-018, RN-030

#### Contexto

Avanço, retrocesso e retorno ao início são a mesma operação sob a mesma regra de
alcançabilidade, e por isso têm um endpoint só: separá-las duplicaria a
validação. É também a operação em que a ortogonalidade das três dimensões é mais
fácil de quebrar — mover mexe na etapa e na condição, e não pode encostar no
impedimento.

#### O que deve ser feito

- [ ] Implementar `POST /v1/tarefas/{tarefaId}/movimentos`.
- [ ] Aceitar como destino apenas a etapa seguinte, a anterior ou a primeira do
      fluxo.
- [ ] Fechar `PERMANENCIA` e `ESPERA_TOMADA` na origem e abrir as duas no
      destino.
- [ ] Zerar o responsável e voltar a condição a `AGUARDANDO_TOMADA`.
- [ ] Não tocar no impedimento aberto — nem na marca, nem no intervalo.
- [ ] Recusar com `422` destino não alcançável e tarefa em condição terminal.
- [ ] Concluir a tarefa quando o destino for etapa terminal, pelo mesmo caminho.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/MovimentoController.java` | criar | rota única de movimentação |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/MovimentoService.java` | criar | alcançabilidade e aplicação |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/MovimentoRequisicao.java` | criar | origem e etapa de destino |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

`POST /v1/tarefas/{tarefaId}/movimentos`

- **Entrada:** `{ origem, etapaDestinoId }`.
- **Saída `200`:** o cartão resultante.
- **Efeito:** fecha `PERMANENCIA` e `ESPERA_TOMADA` na origem, abre as duas no
  destino, zera o responsável e volta a condição a `AGUARDANDO_TOMADA`. O
  impedimento aberto — marca e intervalo — **não é tocado**, e o cartão devolvido
  continua com o bloco de impedimento preenchido.
- **`422`** destino não alcançável: não é a etapa seguinte, nem a anterior, nem a
  primeira do fluxo.
- **`422`** tarefa em `CONCLUIDA` ou `ENCERRADA_SEM_CONCLUSAO`.
- **`409`** origem divergente.
- **Etapa terminal:** mover para etapa `terminal` **conclui** a tarefa. É o
  caminho da conclusão, não uma operação à parte; o desfecho em si é implementado
  no épico seguinte, e esta task apenas garante que o caminho passe por aqui.

O evento gravado é `TAREFA_MOVIDA`, com etapa de origem e de destino e as
condições de origem e destino preenchidas.

#### Guia técnico — pontos de atenção

- **Mover não fecha o intervalo de impedimento e não apaga a marca.** É o erro
  mais provável desta task, e a razão de o impedimento não ser coluna de tarefa:
  a única operação que apaga a marca é o registro do desfecho.
- **Retorno à primeira etapa é exceção nomeada da adjacência**, permitido a
  partir de qualquer etapa. Não o trate como retrocesso comum.
- **A alcançabilidade é calculada sobre as etapas não arquivadas**, pela ordem.
- **Mover reabre a espera de tomada**, sempre um intervalo novo — nunca a
  continuação do anterior.
- **Condição terminal encerra o trânsito.** Tarefa concluída ou encerrada não se
  move; a saída dela é a reabertura, em outro épico.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | Mover para a etapa seguinte muda a etapa e reinicia a contagem de permanência | leitura do cartão e dos intervalos |
| 2 | Mover para etapa não alcançável é recusado com `422` e nada muda | comparação do estado antes e depois |
| 3 | Mover com origem divergente devolve `409` com o estado atual | inspeção do corpo |
| 4 | Retroceder uma etapa é aceito e reinicia a contagem de permanência | percurso de ida e volta |
| 5 | Retornar à primeira etapa a partir de qualquer etapa é aceito | movimento de etapa distante para a primeira |
| 6 | Mover tarefa com impedimento aberto preserva a marca e a contagem de impedimento | inspeção do cartão e do intervalo após o movimento |
| 7 | Mover zera o responsável e devolve a tarefa ao pool | leitura do cartão |
| 8 | Tarefa em condição terminal não se move | tentativa recusada com `422` |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
