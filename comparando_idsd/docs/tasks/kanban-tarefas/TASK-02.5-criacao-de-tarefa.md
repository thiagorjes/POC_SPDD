# TASK-02.5 — Criação de tarefa

- **Status:** concluída
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

- [x] Implementar `POST /v1/projetos/{projetoId}/tarefas`.
- [x] Fazer a tarefa nascer na etapa de menor `ordem`, em `AGUARDANDO_TOMADA`,
      sem responsável.
- [x] Abrir na criação os intervalos de `PERMANENCIA` e `ESPERA_TOMADA`, com
      `episodio` 1.
- [x] Recusar com `422` título ausente ou em branco.
- [x] Recusar com `422` projeto sem fluxo configurado, orientando configurar as
      etapas antes.
- [x] Exigir participação no projeto; sem ela, `404`.
- [x] **Publicar a implementação de `TarefasAtivasPorEtapa`**, a porta que
      TASK-02.2 deixou sem implementação. Enquanto o bean não existir, a recusa
      de RF-017 conta zero e nunca dispara.
- [x] **Resolver `atorId` a partir do principal autenticado, nunca do corpo** —
      metade aberta de ACH-12 da revisão de TASK-02.4.
- [x] **Montar `dados` na borda, campo a campo, a partir das chaves que
      `data-model.md` §4 declara para o tipo** — nunca repassar documento vindo
      do cliente. Metade aberta de ACH-15 da mesma revisão.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/TarefaController.java` | criar | rota de criação |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/CriacaoDeTarefaService.java` | criar | usa o registrador de evento |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/NovaTarefaRequisicao.java` | criar | registro de entrada |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/CartaoResposta.java` | criar | forma do cartão, reusada pelo board |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/ContagemDeTarefasAtivas.java` | criar | implementa `internal/projeto/TarefasAtivasPorEtapa`; é o adaptador que liga a recusa de RF-017 |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/RegistradorDeEvento.java` | alterar, se necessário | só para retirar os dois Javadoc que apontam esta task como o lugar das metades abertas de ACH-12 e ACH-15, depois que elas estiverem fechadas aqui |

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
- **A porta que esta task fecha não é opcional.** `EtapaService` recebe
  `Optional<TarefasAtivasPorEtapa>` e, sem bean publicado, **conta zero** — o
  caminho de recusa de RN-020 existe, está escrito e fica inerte por falta de
  massa. Publicar o bean é o que o liga. Assinatura literal a implementar:
  `long contarEm(UUID etapaId)`, em `br.com.idsd.kanban.internal.projeto`. A
  direção da dependência é obrigatória — o domínio de tarefa implementa a porta
  do domínio de projeto, e nunca o contrário: importar repositório de tarefa
  dentro de `internal/projeto` faria configuração depender de operação.
- **"Tarefa ativa" exclui as terminais.** `CONCLUIDA` e
  `ENCERRADA_SEM_CONCLUSAO` não contam. Contá-las tornaria toda etapa terminal
  inarquivável para sempre. Quem implementa a porta responde por essa exclusão —
  a contagem é sobre a projeção `tarefa`, filtrando `etapa_id` e condição não
  terminal.
- **Título em branco inclui apenas espaços.** A validação de formato precisa
  aparar antes de decidir.
- **Esta task é onde caduca o rebaixamento dos cinco achados de segurança de
  TASK-02.4.** Eles foram rebaixados em 2026-09-15 com aprovador humano nomeado
  sob o argumento **"não há chamador externo"**, e o argumento tem prazo escrito:
  caduca quando a borda nascer, que é aqui. O precedente é literal — o
  rebaixamento de ACH-08 em TASK-02.3 dizia "não há escritor" e caducou em
  TASK-02.4, que foi o primeiro escritor. Duas metades ficaram explicitamente
  para esta task, e **o próprio código do núcleo as endereça a ela por nome**, em
  dois Javadoc de `RegistradorDeEvento`:
  - **`atorId` (ACH-12).** Ele é a autoria do único registro de auditoria do
    sistema e hoje é parâmetro livre, nunca confrontado com o principal.
    Conferi-lo *dentro* do núcleo foi recusado com razão, porque quebraria o job
    e o arnês, que legitimamente não têm principal. A borda é o lugar, e o
    fechamento aqui é **estrutural e não uma checagem**: `NovaTarefaRequisicao`
    não tem campo de ator, e o controlador extrai a identidade do contexto
    autenticado. Não há o que conferir quando não há o que divergir.
  - **`dados` (ACH-15).** O núcleo ganhou teto e checagem de JSON, que é validação
    de forma; nenhuma forma distingue dado de serviço de dado colado do corpo da
    requisição. Aqui o fechamento também é estrutural: a borda **constrói** o
    documento a partir das chaves que `data-model.md` §4 declara para o tipo —
    para `TAREFA_CRIADA`, `titulo` e nada mais — em vez de repassar objeto
    recebido. Note que `titulo` **é** dado de cliente e está ali por decisão
    registrada da spec; o que a regra proíbe é a passagem livre, não este campo.
  - Fechadas as duas, **retire os dois parágrafos de Javadoc** de
    `RegistradorDeEvento` que dizem "o lugar é a borda, que nasce em TASK-02.5".
    Comentário que aponta para dívida já paga é pior que comentário nenhum: o
    próximo leitor procura o que já não existe.

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
| 7 | Com uma tarefa ativa na etapa, `PUT /v1/projetos/{id}/etapas` que a omite é recusado com `422` e as tarefas permanecem onde estavam | SCN-017.3, que só passa a ter poder de falha depois desta task |
| 8 | Com apenas tarefas terminais na etapa, o mesmo `PUT` arquiva a etapa normalmente | contagem que exclui `CONCLUIDA` e `ENCERRADA_SEM_CONCLUSAO` |
| 9 | `atorId` do evento gravado é o do principal autenticado, e o corpo não tem como influenciá-lo | leitura do log após criação; ausência de campo de ator em `NovaTarefaRequisicao` |
| 10 | `dados` do evento gravado contém exatamente as chaves que §4 declara para `TAREFA_CRIADA` | leitura do log |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
| 2026-09-15 | emenda — caducidade do rebaixamento de TASK-02.4 | O rebaixamento dos cinco achados de segurança de TASK-02.4 foi aprovado sob o argumento "não há chamador externo", **com prazo declarado: caduca nesta task**, que é o chamador externo. Duas metades pertenciam explicitamente a ela — `atorId` contra o principal (ACH-12) e distinguir dado de cliente de dado de serviço em `dados` (ACH-15) —, e **nenhuma das duas estava escrita aqui**: viviam no relatório de revisão e em dois Javadoc de `RegistradorDeEvento`. É a mesma falha que a emenda de ACH-10 corrigiu nesta mesma task em 2026-09-14, e pela mesma razão: relatório alheio e comentário de outro arquivo não são instrução desta task. Se esta esquecesse, o rebaixamento caducaria sem que nada no artefato que o `/implement` lê mandasse fechá-lo, e **nenhum teste acusaria** — a suíte congelada não conhece essas metades. Passam a ser dois itens de execução, um ponto de atenção com o precedente escrito e dois critérios de aceite. Ficou registrado que o fechamento das duas é **estrutural e não uma checagem**, e que os dois Javadoc que apontam para esta task devem sair quando ela fechar |
| 2026-09-14 | emenda — ACH-10 da revisão de TASK-02.2 | A obrigação de publicar `TarefasAtivasPorEtapa` existia só no histórico da task que criou a porta, e histórico de outra task não é instrução desta. Se esta esquecesse, a recusa de RN-020 contaria zero para sempre e **nenhum teste acusaria** — SCN-017.3 não tem massa de tarefa antes daqui. Passa a ser item de execução, arquivo declarado, ponto de atenção com a assinatura literal e dois critérios de aceite, um deles o caso negativo das terminais. A direção da dependência ficou escrita junto, porque o atalho — importar o repositório de tarefa dentro de `internal/projeto` — é mais curto que o correto |
| 2026-09-15 | implementação | Cinco arquivos criados e um alterado. **As duas metades de segurança fecharam como a emenda previu — estruturalmente.** `NovaTarefaRequisicao` não tem campo de ator, o controlador extrai a identidade do principal autenticado e a passa como argumento ao serviço; e `dados` é construído chave a chave a partir de §4, de modo que nada do corpo alcança o log. Os dois parágrafos de Javadoc de `RegistradorDeEvento` que apontavam para esta task saíram e foram reescritos como a garantia que passou a existir. **Duas decisões que a suíte congelada impôs.** `CriacaoDeTarefaServiceTest` monta o serviço com **dois** colaboradores, de modo que `AplicadorDeIntervalos`, o `EntityManager` e o `ObjectMapper` entram por campo — sem perda de verificação, porque a recusa que o teste exercita acontece antes de os três serem tocados. E a linha de `tarefa` é gravada e descarregada **antes** do evento, porque `RegistradorDeEvento.exigirCoerencia` confere que a tarefa pertence ao projeto declarado e precisa encontrá-la. **Decisão própria, e ela importa para SDR-006:** `TAREFA_CRIADA` grava `etapa_destino_id`. Sem essa coluna a reconstrução reabriria a permanência inicial na etapa **de hoje** — o aplicador cai no `tarefa.etapaId` quando o evento não declara etapa —, e a série de tempo de toda tarefa que se moveu ficaria errada na origem. **Achado: a task descreve a porta com a assinatura errada** — o ponto de atenção manda implementar `long contarEm(UUID etapaId)`, e a interface real é `Map<UUID, Long> contarEm(Collection<UUID>)` desde ACH-07 da reexecução de TASK-02.2, que a mudou justamente para tornar possível a consulta única dentro da seção crítica. Implementada a real; a task é que está desatualizada, destino `/tasks`. **Medição: 198 testes, 110 verdes / 88 vermelhos**, contra 190 / 96 / 94 — os 8 a mais são `CriacaoDeTarefaServiceTest`, que passou a compilar e saiu 8/8 verde, e os outros 6 que acenderam eram vermelhos antes; **zero regressão**, e os arquivos de teste que não compilam caem de 3 para 2. Todo vermelho remanescente morre em `GET /v1/tarefas/{id}` ou `GET /board`, rotas de TASK-02.6. **Critérios 4b, 9 e 10 medidos e satisfeitos**, os dois últimos direto contra o log em cópia descartável, porque nenhum teste congelado os alcança: `ator_id` é exatamente o identificador da autenticada, e `dados` é `{"titulo": ...}` e nada além. Critérios 2 e 5 medidos junto — dois intervalos abertos com episódio 1, um evento com `seq` atribuída. **7 e 8 seguem sem medição**, porque `ConfiguracaoDoFluxoIT` morre na rota de leitura de tarefa |
