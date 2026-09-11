# TASK-05.4 — Reabertura pelo Product Owner com novo episódio

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-05.3
- **Cenários cobertos:** SCN-013.1, SCN-013.2, SCN-013.3
- **Origem:** RF-013, RN-017, RN-019, RN-034

#### Contexto

A reabertura devolve ao fluxo a tarefa concluída que precisa de mais trabalho, e
é a única ação do produto privativa de uma só pessoa. O tempo do novo percurso é
episódio próprio: somá-lo ao anterior tornaria o tempo por etapa incomparável
entre tarefas reabertas e não reabertas.

#### O que deve ser feito

- [ ] Implementar `POST /v1/tarefas/{tarefaId}/reabertura`.
- [ ] Devolver a tarefa a `AGUARDANDO_TOMADA` na **primeira** etapa do fluxo, sem
      responsável.
- [ ] Incrementar o episódio e abrir os intervalos do novo episódio.
- [ ] Restringir a operação ao papel de Product Owner; `403` para qualquer outro,
      com a tarefa permanecendo concluída.
- [ ] Recusar com `422` a reabertura de tarefa que não está concluída.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/ReaberturaController.java` | criar | rota de reabertura |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/ReaberturaService.java` | criar | novo episódio |
| `backend/src/main/java/br/com/idsd/kanban/internal/tempo/AplicadorDeIntervalos.java` | alterar | evento de reabertura abre intervalos do episódio novo |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

`POST /v1/tarefas/{tarefaId}/reabertura`

- **Entrada:** `{ origem, motivo }`.
- **Saída `200`:** volta a `AGUARDANDO_TOMADA` na primeira etapa do fluxo, sem
  responsável e **incrementando o episódio**.
- **Privativo do Product Owner.** `403` para qualquer outro papel, e a tarefa
  permanece concluída.
- **`422`** se a tarefa não está `CONCLUIDA` — encerrada sem conclusão não reabre.

O evento gravado é `TAREFA_REABERTA`, com o episódio já incrementado, e abre
`PERMANENCIA` e `ESPERA_TOMADA` do novo episódio. Os intervalos do episódio
anterior permanecem fechados e intactos.

#### Guia técnico — pontos de atenção

- **A etapa de retorno é a primeira do fluxo**, e isso é decisão de negócio com
  efeito na medição — devolver à etapa terminal reconcluiria a tarefa no instante
  seguinte.
- **O episódio é o que mantém o agregado comparável.** Sem ele, uma tarefa
  reaberta some duas permanências na mesma etapa e distorce a média.
- **Nada do episódio anterior é reaberto nem recalculado.**
- **Encerrada sem conclusão é terminal absoluto** e não tem caminho de volta.
- **O papel é verificado sobre a participação real no projeto**, nunca sobre
  papel global nem sobre dado vindo do cliente.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | Reabrir devolve a tarefa à primeira etapa, aguardando tomada e sem responsável | leitura do cartão |
| 2 | O episódio é incrementado e os novos intervalos pertencem a ele | inspeção dos intervalos por episódio |
| 3 | Os intervalos do episódio anterior permanecem fechados e inalterados | comparação antes e depois |
| 4 | Papel diferente de Product Owner recebe `403` e a tarefa segue concluída | requisição por cada outro papel |
| 5 | Tarefa encerrada sem conclusão não reabre | tentativa recusada com `422` |
| 6 | O agregado distingue os episódios da mesma tarefa | consulta de tempo por etapa após reabertura |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
