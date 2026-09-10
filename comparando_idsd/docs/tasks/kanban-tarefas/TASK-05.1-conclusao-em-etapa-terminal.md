# TASK-05.1 — Conclusão ao alcançar etapa terminal

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** nenhuma
- **Cenários cobertos:** SCN-011.1
- **Origem:** RF-011, RN-011, RN-018

#### Contexto

Concluir não é uma operação à parte: é o efeito de mover a tarefa para uma etapa
marcada como terminal. Manter um único caminho evita que o produto tenha duas
formas de chegar ao mesmo estado, com duas validações que podem divergir.

#### O que deve ser feito

- [ ] Fazer a movimentação para etapa terminal transicionar a condição a
      `CONCLUIDA`.
- [ ] Fechar todos os intervalos abertos da tarefa na conclusão.
- [ ] Recusar a conclusão com impedimento aberto, com a mesma verificação
      compartilhada da marca.
- [ ] Tornar a condição terminal: nenhuma movimentação parte dela.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/java/<pkg>/internal/tarefa/MovimentoService.java` | alterar | destino terminal conclui |
| `backend/src/main/java/<pkg>/internal/tarefa/VerificadorDeMarca.java` | criar | recusa compartilhada por conclusão e encerramento |
| `backend/src/main/java/<pkg>/internal/tempo/AplicadorDeIntervalos.java` | alterar | conclusão fecha os intervalos abertos |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

Mover para etapa `terminal` **conclui** a tarefa: o evento gravado é
`TAREFA_CONCLUIDA`, a condição passa a `CONCLUIDA` e todos os intervalos abertos
são fechados.

A recusa por impedimento aberto é a mesma verificação usada pelo encerramento e
pela superfície de conclusão — uma implementação só, com o `detail` diferindo
apenas na operação nomeada, exigindo o desfecho registrado antes.

Condição terminal: nenhuma movimentação parte de `CONCLUIDA` ou de
`ENCERRADA_SEM_CONCLUSAO`; a tentativa recebe `422`.

#### Guia técnico — pontos de atenção

- **Não crie um segundo caminho de conclusão.** A rota dedicada, na task
  seguinte, existe para exercitar uma recusa e nunca transiciona a condição.
- **A verificação da marca é compartilhada desde já.** Duplicá-la é o caminho para
  as duas cópias divergirem quando a regra mudar.
- **Concluir fecha os três tipos de intervalo abertos**, e é o único evento que
  fecha todos de uma vez.
- **A conclusão preserva o histórico inteiro.** Nada é apagado nem recalculado.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | Mover para etapa terminal conclui a tarefa | leitura do cartão e do evento |
| 2 | A conclusão fecha todos os intervalos abertos da tarefa | consulta aos intervalos com fim nulo |
| 3 | Concluir com impedimento aberto é recusado com `422` e nada muda | comparação do estado antes e depois |
| 4 | Tarefa concluída não se move | tentativa recusada com `422` |
| 5 | O histórico da tarefa permanece íntegro após a conclusão | leitura do detalhe |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
