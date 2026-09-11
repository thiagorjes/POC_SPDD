# TASK-03.4 — Devolução ao pool

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-03.3
- **Cenários cobertos:** SCN-008.1, SCN-008.2
- **Origem:** RF-008, RN-030

#### Contexto

Devolver é o inverso de assumir e o caminho honesto para quem percebe que não vai
tocar a tarefa. A devolução mantém a tarefa na etapa e abre uma **nova** espera
de tomada: o tempo que a tarefa espera pela segunda vez é episódio próprio, não
continuação do primeiro.

#### O que deve ser feito

- [ ] Implementar `DELETE /v1/tarefas/{tarefaId}/tomada`, com a origem no corpo.
- [ ] Voltar a condição a `AGUARDANDO_TOMADA` na mesma etapa, limpando o
      responsável.
- [ ] Abrir um novo intervalo de `ESPERA_TOMADA`.
- [ ] Recusar com `403` quem não é o responsável registrado.
- [ ] Preservar o impedimento aberto e sua contagem.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/TomadaController.java` | alterar | acrescenta a rota de devolução |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/TomadaService.java` | alterar | aplicação da devolução |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

`DELETE /v1/tarefas/{tarefaId}/tomada`

- **Entrada:** `{ origem }` no corpo.
- **Saída `200`:** volta a `AGUARDANDO_TOMADA` na mesma etapa e abre **nova**
  `ESPERA_TOMADA`.
- **`403`** quando quem pede não é o responsável registrado.
- Impedimento aberto permanece aberto com a contagem em curso.

O evento gravado é `TAREFA_DEVOLVIDA`.

#### Guia técnico — pontos de atenção

- **A nova espera é um intervalo novo**, com início no instante da devolução.
  Reabrir o intervalo anterior apagaria o tempo já medido e falsearia a série.
- **A etapa não muda.** Devolver não é retroceder.
- **Recusa é `403` e não `409`**: o pedido é compreendido e o estado não está
  divergente — quem pede é que não tem legitimidade.
- **O impedimento segue intocado**, como em toda operação que não seja o registro
  do desfecho.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | Devolver deixa a tarefa aguardando tomada na mesma etapa, sem responsável | leitura do cartão |
| 2 | A devolução abre um novo intervalo de espera de tomada | dois intervalos de espera no episódio, o segundo aberto |
| 3 | A permanência na etapa não é interrompida pela devolução | intervalo de permanência mantém o início |
| 4 | Quem não é o responsável recebe `403` | requisição por outro participante |
| 5 | Impedimento aberto permanece aberto após a devolução | leitura do cartão e do intervalo |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
