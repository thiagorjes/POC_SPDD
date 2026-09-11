# TASK-02.2 — Consulta e substituição do fluxo de etapas

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-02.1, TASK-01.5
- **Cenários cobertos:** SCN-002.4, SCN-017.1, SCN-017.2, SCN-017.3
- **Origem:** RF-002, RF-017, RN-001, RN-020, RN-021, RN-022, RN-038

#### Contexto

O fluxo é substituído inteiro numa operação, e não editado etapa a etapa: a
ordem é propriedade do conjunto, e a edição individual permitiria estados
intermediários sem etapa terminal — que é justamente o que a regra proíbe.

#### O que deve ser feito

- [ ] Implementar `GET /v1/projetos/{projetoId}/etapas`.
- [ ] Implementar `PUT /v1/projetos/{projetoId}/etapas` substituindo o fluxo
      inteiro.
- [ ] Exigir permissão de configuração nas duas rotas, com `403` para
      participante sem ela.
- [ ] Recusar com `422` a configuração sem nenhuma etapa terminal, **sem
      alterar** o fluxo vigente.
- [ ] Recusar com `422` o arquivamento de etapa que contém tarefas ativas,
      identificando qual.
- [ ] Preservar o identificador ao renomear.
- [ ] Acrescentar `fluxoConfigurado` a cada item de `GET /v1/projetos`,
      derivado por existência sobre `etapa` **dentro da mesma consulta**.
      Recebido de TASK-01.5 em 2026-09-11 (ACH-03): lá o campo não era
      implementável, porque a tabela `etapa` nasce em TASK-02.1.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/java/br/com/idsd/kanban/internal/projeto/EtapaController.java` | criar | duas rotas |
| `backend/src/main/java/br/com/idsd/kanban/internal/projeto/EtapaService.java` | criar | substituição transacional do fluxo |
| `backend/src/main/java/br/com/idsd/kanban/internal/projeto/FluxoRequisicao.java` | criar | registro de entrada |
| `backend/src/main/java/br/com/idsd/kanban/internal/projeto/EtapaResposta.java` | criar | registro de saída |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

`GET /v1/projetos/{projetoId}/etapas`

- **Saída `200`:** lista de `{ id, nome, ordem, terminal }`, ordenada, **sem as
  arquivadas**.

`PUT /v1/projetos/{projetoId}/etapas`

- **Entrada:** `{ "etapas": [ { "id": "<uuid opcional>", "nome": "", "ordem": 0, "terminal": false } ] }`
  — `id` ausente **cria**; `id` presente no banco e omitido do corpo **arquiva**.
- **Saída `200`:** o fluxo resultante, na mesma forma do `GET`.
- **`422`** quando nenhuma etapa é terminal. O fluxo vigente **não** é alterado.
- **`422`** quando uma etapa a arquivar contém tarefas ativas, com `detail`
  informando que a etapa precisa ser esvaziada antes e `errors` identificando
  qual.
- **`403`** para participante sem permissão de configuração.

Toda a substituição roda em uma transação: ou o fluxo inteiro vale, ou nada
muda.

#### Guia técnico — pontos de atenção

- **A verificação de etapa terminal acontece antes de qualquer escrita.**
  Validar depois de gravar e reverter deixa passar o estado intermediário em
  qualquer caminho que leia dentro da transação.
- **"Tarefa ativa" exclui as terminais.** Tarefa em condição `CONCLUIDA` ou
  `ENCERRADA_SEM_CONCLUSAO` não impede o arquivamento da etapa; enquanto a
  entidade de tarefa ainda não existir no código, a contagem é zero e o caminho
  precisa ficar preparado para a task que a cria.
- **Alteração de configuração vale dali em diante.** Nada de reescrever
  histórico nem de recalcular série de tempo por causa de renomeação.
- **Não implemente edição etapa a etapa "por conveniência".** A rota única é o
  que garante que nunca exista fluxo sem terminal.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | O fluxo configurado passa a valer para as transições seguintes | configurar e ler de volta a ordem e os identificadores |
| 2 | Configuração sem etapa terminal é recusada com `422` e o fluxo vigente permanece | comparação do fluxo antes e depois da tentativa |
| 3 | Arquivar etapa com tarefas ativas é recusado com `422` identificando a etapa | corpo de erro com `errors` apontando a etapa |
| 4 | Renomear preserva o identificador da etapa | comparação do `id` antes e depois |
| 5 | Participante sem permissão de configuração recebe `403` | requisição com papel `dev` |
| 6 | A leitura não devolve etapas arquivadas | arquivar uma etapa e ler |
| 7 | Projeto sem etapa alguma vem com `fluxoConfigurado` falso, e projeto com etapa vem verdadeiro | SCN-002.4 — lista com os dois projetos numa mesma resposta |
| 8 | O campo novo não custa consulta por item | `AusenciaDeNMaisUmIT` continua verde: derive o `EXISTS` na mesma consulta, e nunca navegando a coleção de etapas por projeto |
| 9 | A resposta de `POST /v1/projetos` deixa de devolver `etapas` constante | `CriacaoDeProjeto.Criado.de(...)` fixa `List.of()` porque a tabela `etapa` não existia; com ela existindo, derivar do projeto. Se continuar constante, todo projeto responderá sem fluxo logo depois de o fluxo ser configurado, e nenhum teste de TASK-01.8 falhará por isso |
| 10 | O estado de sucesso de TL-11 volta a nomear a marca do cartão como o lembrete da pendência | `frontend/src/componentes/formulario-de-novo-projeto.tsx` — a frase saiu por ACH-07 da revisão de TASK-01.7 porque prometia uma sinalização que não chegava; com `fluxoConfigurado` emitido, a marca passa a existir e as duas sinalizações de RN-038 voltam a estar completas |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
| 2026-09-11 | recebimento de escopo | Critério 9 recebido de TASK-01.8 por ACH-07 da revisão dela: `Criado.etapas` é constante porque não há tabela de onde derivar, e a constante é verdadeira hoje. O gatilho está escrito no javadoc de `CriacaoDeProjeto.Criado` para que a próxima pessoa o encontre onde vai editar |
| 2026-09-11 | recebimento de escopo | Critério 10 recebido de TASK-01.7 por ACH-07 da revisão dela: o estado de sucesso de TL-11 prometia que a pendência de configuração ficaria marcada no cartão da lista, e a marca não chega porque `fluxoConfigurado` não é emitido. A promessa saiu da tela — quem escolhe adiar não pode confiar num lembrete que não existe — e volta quando o campo existir, junto do critério 7 |
| 2026-09-11 | recebimento de escopo | SCN-002.4 e `fluxoConfigurado` vieram de TASK-01.5 por ACH-03 da revisão dela. O cenário é congelado e é do escopo de RF-002, mas exige `EXISTS` sobre `etapa`, que não existe antes de TASK-02.1 — fechar a task de origem com um cenário do próprio escopo irrealizável é o defeito que este movimento corrige. A dependência de TASK-01.5 é o que garante que a rota já exista para receber o campo |
