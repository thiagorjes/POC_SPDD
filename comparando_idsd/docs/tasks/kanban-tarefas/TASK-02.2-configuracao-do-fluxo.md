# TASK-02.2 — Consulta e substituição do fluxo de etapas

- **Status:** concluída
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

- [x] Implementar `GET /v1/projetos/{projetoId}/etapas`.
- [x] Implementar `PUT /v1/projetos/{projetoId}/etapas` substituindo o fluxo
      inteiro.
- [x] Exigir permissão de configuração nas duas rotas, com `403` para
      participante sem ela.
- [x] Recusar com `422` a configuração sem nenhuma etapa terminal, **sem
      alterar** o fluxo vigente.
- [x] Recusar com `422` o arquivamento de etapa que contém tarefas ativas,
      identificando qual.
- [x] Preservar o identificador ao renomear.
- [x] Acrescentar `fluxoConfigurado` a cada item de `GET /v1/projetos`,
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
| 2026-09-14 | tentativa 1 — Red confirmado | A suíte não compilava, e `EtapaServiceTest` falhava exatamente nos três símbolos que esta task cria: `RegraDeNegocioViolada`, `EtapaService` e `FluxoRequisicao`. Os outros quatro arquivos de teste que não compilam nomeiam classes de TASK-02.4/02.5/03, e são a linha de base |
| 2026-09-14 | tentativa 1 — desvio de escopo declarado | A tabela de arquivos da task declara 4 arquivos e não bastou. Fora dela, e cada um com a razão: **`shared/RegraDeNegocioViolada.java`** — a suíte congelada a importa desse pacote em três testes, e está fora do alcance de quem implementa; **`SubstituicaoDeFluxo.java` + `EtapaRepositorioImpl.java` + `EtapaRepositorio.java`** — a escrita de etapa não existia, e `EtapaRepositorio` é `Repository` marcadora por ACH-01 da revisão de 02.1, de modo que nada nela escreve; **`TarefasAtivasPorEtapa.java`** — a costura que o ponto de atenção da própria task pede; **`ProjetoConsulta`/`ProjetoRepository`/`ProjetoResumo`/`ProjetoController`** — critérios 7 e 8; **`CriacaoDeProjeto`** — critério 9; **`frontend/.../formulario-de-novo-projeto.tsx` e `frontend/src/lib/api/projetos.ts`** — critério 10 |
| 2026-09-14 | tentativa 1 — desenho forçado pelo banco | A substituição do fluxo é feita em **dois passos** e não em um. `etapa_projeto_ordem_unico` é único parcial e não é adiável — `DEFERRABLE` só existe em `UNIQUE CONSTRAINT`, e restrição parcial não é constraint —, de modo que trocar a ordem de duas etapas ativas viola o índice no meio da transação mesmo com estado final válido. O passo intermediário desloca as ordens vigentes para uma faixa de trabalho e descarrega, antes de reatribuir. É a consequência que ACH-07 da revisão de TASK-02.1 escreveu na migration endereçada a esta task. O deslocamento é feito pela entidade e não por `update` em massa: em massa, a etapa cuja ordem final coincide com a original pareceria inalterada, nenhum `UPDATE` seria emitido e a linha ficaria em disco com a ordem deslocada. A ordem original é restituída antes de arquivar, para não gravar na série de tempo um número que nunca foi posição |
| 2026-09-14 | tentativa 1 — construtor de um argumento | `EtapaServiceTest` fixa `new EtapaService(repositorio)`, o que impede que a contagem de tarefas ativas seja colaborador de construtor. O construtor de um argumento ficou e **declara** que a regra de etapa terminal é aritmética sobre a requisição: se um dia ela passar a precisar de banco ou da porta de contagem, o teste congelado para de compilar, que é o aviso desejado |
| 2026-09-14 | tentativa 1 — Verde parcial, por dependência e não por defeito | `EtapaServiceTest` 5/5 e `semPermissaoDeConfigurarE403` verdes. Os outros 4 testes de `ConfiguracaoDoFluxoIT` seguem vermelhos porque exercitam `GET /v1/projetos/{id}/board` e `POST /v1/projetos/{id}/tarefas`, que nascem em TASK-02.6 e TASK-02.5 — falham com `404` de rota inexistente, e nenhum deles falha por caminho desta task. **SCN-002.4 continua sem teste:** o campo `fluxoConfigurado` passou a ser emitido, mas escrever o teste é do `/tests`, não de quem implementa. Ver a pendência 14 |
| 2026-09-14 | tentativa 1 — medição | 168 testes, **70 verdes / 98 vermelhos**, contra a base de 163 (64/99). Delta: os 5 testes novos de `EtapaServiceTest`, que passaram a compilar, mais 1 vermelho que virou verde. **Zero regressões** — a única falha suspeita, `CriacaoDeProjetoIT.criaEnomeiaAPrimeiraAdministradora`, é `404` em `GET /participacoes`, rota de task posterior; as asserções de criação, inclusive `etapas: []` do critério 9, passaram. `AusenciaDeNMaisUmIT` 2/2, o que sustenta o critério 8. Medição em cópia descartável sob `backend/target/`, porque 4 arquivos de teste de tasks posteriores impedem a suíte de compilar |
| 2026-09-14 | revisão técnica — reprovada | `/code-review TASK-02.2` devolveu 17 achados, 6 bloqueantes, em `docs/review/kanban-tarefas/TASK-02.2-review.md`. Quatro bloqueantes são a mesma omissão: a validação de borda cobre a forma de cada campo e não a do conjunto, de modo que `id` repetido, `ordem` duplicada, elemento nulo na lista e `ordem` alcançando a faixa de trabalho saem como `500` onde o contrato pede `422`. ACH-05 volta ao `/techspec` (concorrência, SDR-002), ACH-10 ao `/tasks` (a obrigação de TASK-02.5 publicar `TarefasAtivasPorEtapa` não está escrita em lugar nenhum além deste arquivo) e ACH-11 ao `/tests` (o plano de verificação atribui SCN-017.x ao EPIC-07 e o plano de tasks ao EPIC-02) |
| 2026-09-14 | tentativa 2 — achados de código fechados | 14 dos 17 achados corrigidos: os 5 bloqueantes de código (ACH-01 a ACH-04 e ACH-09) e mais ACH-06, 07, 08, 12, 13, 14, 15, 16, 17. A forma do **conjunto** passou a ser recusada antes de qualquer escrita — `id` repetido, `ordem` repetida e item nulo saem em `422` com `errors` nomeando o que colidiu —, e os tetos que faltavam viraram validação: `Etapa.ORDEM_MAXIMA` torna a faixa de trabalho inalcançável por requisição em vez de suposta inalcançável, e `MAXIMO_DE_ETAPAS` e `TAMANHO_MAXIMO_DO_NOME` põem limite no que uma escrita autenticada pode gravar contra a coluna `text` sem teto. ACH-05 (concorrência), ACH-10 (`/tasks`) e ACH-11 (`/tests`) não são desta mão |
| 2026-09-14 | tentativa 2 — o tradutor de restrição nasceu estreito | ACH-02 pedia `@ExceptionHandler` para `DataIntegrityViolationException`. O global, que traduz **toda** violação em `409`, reprovou `TransacaoUnicaDeCriacaoIT` — verificação além dos cenários de TASK-01.8, que injeta uma violação arbitrária e exige `5xx`, e que está congelada. A exigência é legítima e não é conflito: "o pedido conflita com o estado" e "o banco recusou algo que ninguém previu" são casos diferentes, e o segundo é defeito. O tradutor ficou **nominal** — casa pelo nome da restrição, hoje só `etapa_projeto_ordem_unico`, e o resto segue para o `500` de sempre. Restrição nova entra na lista junto da rota que pode violá-la |
| 2026-09-14 | tentativa 2 — ACH-17 por ordem e não por acidente | A ordem original da etapa arquivada passou a ser restituída **depois** de a linha estar arquivada em disco. Antes, a restituição corria antes do flush e só funcionava porque o Hibernate emite `INSERT` antes de `UPDATE` e porque a linha saía do índice parcial no mesmo statement — um flush intermediário futuro quebraria o caminho com o `500` de ACH-02 |
| 2026-09-14 | tentativa 2 — medição | 168 testes, **70 verdes / 98 vermelhos**, exatamente a linha de base de antes destas correções — lista de falhas comparada item a item, **zero regressão**. Os 4 arquivos de teste de tasks posteriores continuam impedindo a compilação da suíte, e a medição de novo saiu de cópia descartável em `/tmp` dentro do contêiner |
| 2026-09-14 | tentativa 1 — validador inutilizável | `check_escopo.py` acusou 18 arquivos, inclusive os que esta task não tocou, e ainda assim saiu em **exit 0**. É a pendência 12 (raiz git é `SPDD_puro`, todo caminho volta prefixado por `comparando_idsd/`) somada a um segundo defeito não registrado: o script reporta erro e não reprova. Escopo conferido à mão, e declarado na linha de desvio acima |
| 2026-09-14 | achados devolvidos aos donos | Os 3 achados fora da mão do `/implement` foram fechados por quem os possui. ACH-05 virou **SDR-005** e a TechSpec v1.12: a substituição do fluxo serializa por bloqueio pessimista da linha de `projeto`, e não por estado de origem declarado — SDR-002 não alcança escrita de conjunto, porque o dano concorrente é a etapa alheia **ausente** do corpo, sobre a qual não há versão a conferir. ACH-10 virou item de execução em TASK-02.5, que é onde a porta `TarefasAtivasPorEtapa` ganha implementação. ACH-11 realinhou 19 linhas do plano de verificação — a divergência de épico era bem maior que os 3 cenários relatados |
| 2026-09-14 | tentativa 3 — SDR-005 implementada | O bloqueio pessimista da linha de `projeto` entrou como **primeira operação** de `EtapaService.substituirFluxo`, antes de ler o fluxo vigente: tudo o que vem depois, inclusive as recusas, decide sobre um conjunto que mais ninguém pode estar substituindo. A porta é `SubstituicaoDeFluxo.bloquearProjeto`, com `em.find(Projeto.class, id, PESSIMISTIC_WRITE)` em `EtapaRepositorioImpl` — `find` com lock explícito e não consulta derivada, porque o `SELECT ... FOR UPDATE` precisa ir ao banco ainda que a entidade já esteja gerenciada pela resolução de permissão. Projeto ausente é defeito de estado (`IllegalStateException`), não entrada de usuário: a rota resolveu o `404` antes |
| 2026-09-14 | tentativa 3 — desvio de `ProjetoRepository` declarado | SDR-005 antecipava o `@Lock` em `ProjetoRepository`, e o downstream não é cumprível assim: injetá-lo exigiria colaborador novo em `EtapaService`, e `EtapaServiceTest` fixa `new EtapaService(EtapaRepositorio)` — suíte congelada, fora do alcance de quem implementa. O bloqueio foi para o fragmento de escrita de etapa, que `EtapaService` já possui. A **decisão** de SDR-005 é cumprida inteira — qual linha trava, quando e por quanto tempo —; muda só a porta. Como o mock de `EtapaRepositorio` absorve a chamada, os 5 testes unitários seguem válidos sem alteração |
| 2026-09-14 | tentativa 3 — sem medição | **Nada foi compilado nem executado.** Não há `mvn`, `mvnw` nem Docker nesta máquina — `docker` não está no `PATH` e de `C:\Program Files\Docker` só restam os `cli-plugins`. As tentativas anteriores mediram dentro do contêiner, caminho hoje indisponível. A verificação de que a suíte continua em 168/70/98 fica pendente e é pré-condição da reexecução do `/code-review` |
| 2026-09-14 | revisão técnica (reexecução) — reprovada | 23 achados, 4 bloqueantes, em `docs/review/kanban-tarefas/TASK-02.2-review-r2.md`. **15 são do `/tests` e não desta mão**, e dizem a mesma coisa por ângulos diferentes: o conserto dos bloqueantes da primeira revisão entrou sem verificação — os tetos de `Etapa`, as três recusas de conjunto e o tradutor `409` não aparecem em teste algum —, e a verificação de concorrência escrita para SDR-005 fica **verde com o bloqueio removido**. O bloqueante de produção é ACH-01: `PESSIMISTIC_WRITE` sem `lock timeout`, sem `statement_timeout` e sem timeout de transação, com Hikari no padrão e `db` na probe de readiness. É contenção convertida em indisponibilidade por um sujeito autenticado com `CONFIGURAR`, e a frase de SDR-005 sobre a espera ser limitada pelo timeout vigente descreve um timeout que não existe — metade do achado volta ao `/techspec` |
| 2026-09-14 | tentativa 4 — ACH-01 fechado, nas duas mãos | **`/implement`:** o teto de espera entrou em três camadas, e nenhuma delas é o hint `jakarta.persistence.lock.timeout` — o dialeto PostgreSQL do Hibernate só traduz espera **zero**, e hint positivo seria mais um teto que o código afirma ter e o banco não aplica, que é a forma de defeito que esta revisão inventariou. O teto fica em `lock_timeout` de 5 s na sessão (quanto se espera para entrar), `@Transactional(timeout = 10)` na rota (quanto se pode segurar depois de entrar) e `connection-timeout` de 5 s no pool, com `maximum-pool-size` explícito. Espera esgotada ganhou código próprio: `503` com `Retry-After`, porque com o teto a requisição deixa de pendurar e passa a **terminar**, e o que ela termina não pode aparecer como `500` — trocar indisponibilidade por defeito aparente não é conserto. Continua `5xx`, o que preserva a exigência de `TransacaoUnicaDeCriacaoIT`. `statement_timeout` ficou **fora, declaradamente**: alcança toda consulta do sistema e não há medição para escolher o número. **`/techspec`:** SDR-005 emendado e TechSpec v1.13 — a redação original atribuía o limite a um `timeout` de transação vigente que não existia em lugar nenhum da configuração, e era essa garantia inexistente que tornava aceitável o trade-off. O contrato de `sessao-e-projetos` ganhou o `503` |
| 2026-09-14 | tentativa 4 — **medida**, e a medição confirma os bloqueantes | A toolchain voltou no mesmo dia: Maven 3.9.14 e Docker 29.7.2. O JDK 25 continua ausente do host e não faz falta — `docker/compose.test.yaml` roda a suíte em `maven:3.9-eclipse-temurin-25` com o socket do daemon montado (ADR-012), que é como as tentativas anteriores mediram. `compile` em **BUILD SUCCESS**, 36 fontes, `release 25`: o `503`, os três imports e o `@Transactional(timeout)` são válidos. `verify` na árvore íntegra para no `testCompile`, nos **exatos 4** arquivos registrados — nenhum novo. Em cópia sem esses 4: **171 testes, 73 verdes / 98 vermelhos** contra a base de 168/70/98, lista de vermelhos idêntica, **zero regressão** — inclusive `TransacaoUnicaDeCriacaoIT`, que era o risco real de colidir com o tratador novo. Os 3 testes novos verdes, `SessaoEProjetosIT` 8/8. **E o experimento decisivo:** com `repositorio.bloquearProjeto(projetoId)` removido, `SubstituicaoDeFluxoConcorrenteIT` fica **2/2 verde**, e o log traz 8 violações de `etapa_projeto_ordem_unico` traduzidas para `409` e absorvidas pelo `isLessThan(500)` — a cadeia que ACH-03 descrevera por leitura, agora observada. ACH-02, ACH-03 e ACH-04 confirmados. A classe roda em 0,845 s com o bloqueio e 16,22 s sem ele: na presença dele, contenção nenhuma é exercitada (ACH-09) |
| 2026-09-14 | tentativa 6 — os 19 achados não bloqueantes fechados, nas três mãos | **`/implement` (ACH-05, 06, 07, 16, 17, 18):** teto de corpo em filtro próprio, `LimiteDeCorpo`, com duas guardas porque `Content-Length` é opcional, e `413 corpo-grande-demais`; `TarefasAtivasPorEtapa.contarEm` passa a receber a coleção inteira, e é a assinatura, não a boa intenção, que torna a consulta única possível dentro da seção crítica; javadoc de `bloquearProjeto` sem a afirmação sobre código que ainda não existe; log do caminho inválido rebaixado; `projetoId` fora da mensagem de exceção; acentuação uniformizada em todo o `TratadorDeErro`, e não apenas nas duas mensagens que ACH-18 nomeava. **`/techspec` (ACH-08, ACH-11):** duas emendas a SDR-005 — a janela TOCTOU da permissão fica decidida e mantida, com as quatro condições que a tornam aceitável **aqui** nomeadas para que a próxima decisão não as redescubra, e o `422 etapa-fora-do-fluxo` fica decidido como desfecho pretendido e explicitamente não `409`. **`/tests` (os 11 restantes):** três classes novas e 18 testes ao todo — `LimitesDaConfiguracaoDoFluxoIT`, `TradutorDeIntegridadeTest`, `TetoDeCorpoIT`, mais dois testes e quatro correções em `SubstituicaoDeFluxoConcorrenteIT` e a semeadura de `Cenario` falhando rápido em não-2xx. A premissa de ACH-11 estava errada e a correção está no arquivo: dois corpos com os mesmos `id` são **ambos válidos**, e o `422` exige que a primeira reduza o fluxo. **Assimetria medida em 4 mutações, todas vermelhas:** sem `liberarOrdens`, sem `and e.arquivadaEm is null`, com o tradutor `409` tornado global, com o teto de corpo elevado. Plano de verificação **v1.8**. Suíte em **189 testes, 91 / 98**, lista de vermelhos idêntica à base: zero regressão. **Nenhum achado em aberto nesta task** |
| 2026-09-14 | tentativa 5 — ACH-02, ACH-03 e ACH-04 fechados pelo `/tests` | Plano de verificação **v1.7**. A correção é de uma palavra e desfaz três achados: as asserções de status de `SubstituicaoDeFluxoConcorrenteIT` passaram de `isLessThan(500)` a **`200` exato**. Sob serialização toda requisição das duas verificações é válida e nenhuma pode ser recusada; sem serialização a perdedora sai `409`, e `409` passa por baixo de qualquer faixa. Asserir propriedade do desfecho estava certo; asserir **faixa** de status não era assertiva nenhuma. ACH-02 foi fechado por **declaração** e não por conserto: a mistura dos dois corpos é inalcançável, porque as faixas de ordem se sobrepõem e toda intercalação colide antes no índice único parcial, e faixas disjuntas não são alternativa — o contrato exige ordens contíguas a partir de 1. A asserção permanece, nomeada no arquivo como rede e não como prova, o que é mais honesto que removê-la. **Assimetria medida, três execuções:** como está, 2/2 verde; sem `bloquearProjeto`, 2/2 vermelho; com `bloquearProjeto` depois da leitura do fluxo vigente, 2/2 vermelho. A primeira mutação de ACH-04 errou o alvo — movi o bloqueio para antes da leitura, não depois — e ficou verde por motivo legítimo: o DR exige que ele preceda a **leitura**, não que seja a primeira instrução do método; a nuance está registrada no javadoc e no relatório. Suíte em **171 testes, 73 / 98**, zero regressão. **Nenhum bloqueante em aberto nesta task** |
| 2026-09-14 | tentativa 4 — sem medição, de novo (superada pela linha acima) | **Nada compilado, nada executado.** `mvn`, `mvnw` e Docker seguem ausentes; `java` apareceu na máquina mas é **21**, e o projeto é 25; não há repositório local `.m2`; o `python` do `PATH` é o atalho da Microsoft Store, que não executa. Os quatro tetos e o `503` são conserto **por leitura**. A pendência de execução cresceu: além dos 3 testes novos nunca executados, há agora um caminho de resposta novo sem verificação — o próprio ACH-01 criou o item que ACH-12 já apontava para o `409` |