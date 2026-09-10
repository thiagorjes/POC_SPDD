# TASK-05.2 — Superfície de conclusão e recusa compartilhada da marca

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-05.1
- **Cenários cobertos:** SCN-011.2, SCN-011.3
- **Origem:** RF-011, RN-011, RN-031

#### Contexto

Dois cenários congelados exigem que o produto **recuse** marcar como concluída:
fora de etapa terminal e com impedimento aberto. Recusa sem superfície não tem o
que exercitar, e é só para isso que esta rota existe. Ela nunca transiciona a
condição.

#### O que deve ser feito

- [ ] Implementar `POST /v1/tarefas/{tarefaId}/conclusao`.
- [ ] Devolver `422` fora de etapa terminal, explicando que a conclusão se dá ao
      alcançar uma etapa terminal.
- [ ] Devolver `422` com impedimento aberto, exigindo o desfecho registrado
      antes.
- [ ] Devolver `200` idempotente quando a tarefa já está concluída, sem novo
      evento.
- [ ] Não transicionar a condição por nenhum caminho desta rota.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/java/<pkg>/internal/tarefa/ConclusaoController.java` | criar | rota de conclusão |
| `backend/src/main/java/<pkg>/internal/tarefa/ConclusaoService.java` | criar | apenas recusa e resposta idempotente |
| `backend/src/main/java/<pkg>/internal/tarefa/VerificadorDeMarca.java` | alterar | segunda chamadora da verificação compartilhada |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

`POST /v1/tarefas/{tarefaId}/conclusao`

- **Entrada:** `{ origem }`.
- **Saída `200`:** o cartão. Só ocorre quando a tarefa **já está** `CONCLUIDA` —
  é a resposta idempotente, sem novo evento.
- **`422`** tarefa fora de etapa terminal, com `detail` explicando que a conclusão
  se dá ao alcançar uma etapa terminal. Nada é alterado.
- **`422`** impedimento aberto, com `detail` exigindo o desfecho registrado antes;
  o impedimento permanece aberto.
- **`409`** origem divergente.

Nenhum caminho desta rota transiciona a condição: ou a tarefa já chegou à etapa
terminal pela movimentação, ou a solicitação é recusada.

#### Guia técnico — pontos de atenção

- **Esta rota não conclui nada.** Fazê-la transicionar cria o segundo caminho que
  a decisão de arquitetura recusou.
- **A verificação da marca é a mesma da movimentação e do encerramento** — uma
  implementação, três chamadoras, `detail` diferindo só na operação nomeada.
- **A ordem das recusas importa para a mensagem**: fora de etapa terminal e com
  impedimento aberto são causas distintas e a pessoa precisa saber qual é.
- **`200` aqui é resposta a estado já alcançado**, não confirmação de operação
  executada.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | Marcar como concluída fora de etapa terminal é recusado com `422` explicativo | inspeção do corpo do erro |
| 2 | A recusa não altera etapa nem condição | comparação do estado antes e depois |
| 3 | Marcar como concluída com impedimento aberto é recusado com `422` exigindo o desfecho | inspeção do corpo do erro |
| 4 | O impedimento permanece aberto após a recusa | leitura do intervalo |
| 5 | Tarefa já concluída devolve `200` sem novo evento | contagem de eventos antes e depois |
| 6 | Nenhum caminho desta rota grava evento de conclusão | inspeção do log após cada caso |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
