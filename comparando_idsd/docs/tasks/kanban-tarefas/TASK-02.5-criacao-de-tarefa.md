# TASK-02.5 — Criação de tarefa

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-02.4, TASK-02.2
- **Cenários cobertos:** SCN-004.1, SCN-004.2, SCN-004.3, SCN-022.3
- **Origem:** RF-004, RF-022, RN-004, RN-006, RN-038

#### Contexto

É a primeira operação de escrita sobre tarefa e a que exercita o núcleo criado na
task anterior de ponta a ponta. A tarefa nasce nas três dimensões ao mesmo tempo:
na etapa de menor ordem, aguardando tomada e sem impedimento — e nasce já
contando dois intervalos.

#### O que deve ser feito

- [ ] Implementar `POST /v1/projetos/{projetoId}/tarefas`.
- [ ] Fazer a tarefa nascer na etapa de menor `ordem`, em `AGUARDANDO_TOMADA`,
      sem responsável.
- [ ] Abrir na criação os intervalos de `PERMANENCIA` e `ESPERA_TOMADA`, com
      `episodio` 1.
- [ ] Recusar com `422` título ausente ou em branco.
- [ ] Recusar com `422` projeto sem fluxo configurado, orientando configurar as
      etapas antes.
- [ ] Exigir participação no projeto; sem ela, `404`.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/TarefaController.java` | criar | rota de criação |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/CriacaoDeTarefaService.java` | criar | usa o registrador de evento |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/NovaTarefaRequisicao.java` | criar | registro de entrada |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/CartaoResposta.java` | criar | forma do cartão, reusada pelo board |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

`POST /v1/projetos/{projetoId}/tarefas`

- **Entrada:** `{ titulo, descricao?, raiaId? }`. **Sem bloco de origem** — não há
  estado anterior a declarar.
- **Saída `201`** com cabeçalho `Location` e corpo com o cartão.
- **`422`** título ausente ou em branco.
- **`422`** projeto sem fluxo configurado, com `detail` orientando configurar as
  etapas antes.
- **`404`** projeto inexistente ou sem participação de quem chama.

Forma do cartão, que esta task fixa e o board reusa:

`{ id, titulo, condicao, raiaId, responsavel, versao, esperaTomada: { desde, decorrido } | null, impedimento: { desde, decorrido, motivo } | null, permanencia: { desde, decorrido } }`

O evento gravado é `TAREFA_CRIADA`, com `episodio` 1, `ator_id` de quem criou e o
título em `dados`.

#### Guia técnico — pontos de atenção

- **A criação não declara origem, mas passa pelo mesmo núcleo.** Ela também
  consome sequência e também publica após o commit.
- **Dois intervalos abrem juntos e não se somam.** A espera de tomada é série
  própria desde o primeiro instante, não um recorte da permanência.
- **`condicao` nunca nasce como `IMPEDIDA`** — esse valor não existe no domínio.
- **Projeto sem fluxo é recusa de negócio, não erro interno.** A mensagem precisa
  dizer o que fazer, porque o caminho de quem cria projeto e cria tarefa em
  seguida passa exatamente por aqui.
- **Título em branco inclui apenas espaços.** A validação de formato precisa
  aparar antes de decidir.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | A tarefa criada aparece na primeira etapa, aguardando tomada e sem responsável | leitura do corpo de `201` |
| 2 | A criação abre os intervalos de permanência e de espera de tomada | consulta aos intervalos abertos da tarefa |
| 3 | Título em branco é recusado com `422` e nada é gravado | contagem de eventos antes e depois |
| 4 | Projeto sem fluxo é recusado com `422` orientando configurar as etapas | corpo de erro com `detail` |
| 4b | Projeto recém-criado por `POST /v1/projetos` aceita a tarefa depois que o fluxo é configurado | SCN-022.3: criar, tentar e receber `422`; configurar as etapas e criar de novo, com aceite |
| 5 | Um evento de criação foi gravado, com sequência atribuída | leitura do log |
| 6 | Quem não participa do projeto recebe `404` | requisição com sujeito sem participação |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
