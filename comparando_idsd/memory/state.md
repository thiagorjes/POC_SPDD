# Estado Operacional — IDSD
_Atualizado em: 2026-09-14_

> Estado atual do sistema e das demandas em andamento.
> Para princípios estáveis e Decision Records, veja [constitution.md](constitution.md).
> Para o registro integral de gates e o histórico narrativo, veja
> [historico.md](historico.md) — arquivo de consulta, não carregado por padrão.

---

## Sistema

| Campo | Valor |
| --- | --- |
| Nome | IDSD |
| Cenário | Novo (greenfield) |
| Guidelines | vinculadas — `guidelines.yaml` → `requirements/guidelines` (`_shared`, `backend/java`, `frontend/nextjs`, `infra/docker`) |
| Inicializado em | 2026-09-03 |

Os flows disponíveis não são listados aqui: eles vivem em `governance/flows/`,
e essa é a única fonte. Copiar a cadeia de etapas para cá cria uma segunda
versão que envelhece em silêncio — foi o que aconteceu na stack anterior.

Regenerar os derivados: `python .agents/scripts/init.py`.
Verificar sincronia: `python .agents/scripts/check_drift.py`.

---

## Demandas ativas

| Demanda | Flow | Classe | Etapa atual | Estado |
| --- | --- | --- | --- | --- |
| kanban-tarefas | `feature` | feature / risco alto / impacto sistema | TASK-02.3 (implementada, sem medição) | Cadeia liberada: PRD v1.5, TechSpec v1.14, plano de verificação v1.8, 8 épicos / 43 tasks, 70/70 cenários congelados cobertos. Os 17 achados da **primeira** revisão de TASK-02.2 estão fechados e a serialização de SDR-005 entrou. A **reexecução** abriu **23 achados novos, 4 bloqueantes** — 15 do `/tests`, 6 do `/implement`, 2 do `/techspec`. **Os 4 bloqueantes estão fechados** — ACH-01 pelo teto de espera de SDR-005 (TechSpec v1.13) e ACH-02/03/04 pelo `/tests` (plano v1.7), os três verificados pela assimetria. Os **19 restantes também estão fechados** — `/implement` 6, `/techspec` 2, `/tests` 11 —, e o fechamento acrescentou 18 testes, quatro deles com poder de falha provado por mutação. **0 achados em aberto.** A tese da revisão: o conserto dos bloqueantes anteriores entrou sem verificação, e a única verificação escrita nesta mão, `SubstituicaoDeFluxoConcorrenteIT`, **não ficava vermelha se o bloqueio de SDR-005 fosse removido** — agora fica. Ver `docs/review/kanban-tarefas/TASK-02.2-review-r2.md`. |

**Escopo congelado.** 70 cenários — 9 `e2e`, 56 `integração`, 5 `unitário` —,
mais 14 verificações além dos cenários e 4 desvios declarados.

**Progresso.** EPIC-01 fechado (TASK-01.1 a 01.8; a ordem executada não foi a
numérica, porque 01.7 depende de 01.8). EPIC-02 aberto com TASK-02.1 concluída
e revisada, e TASK-02.2 concluída e revisada **duas vezes** — a segunda abriu
4 bloqueantes, todos fechados e verificados no mesmo dia.

**Suíte.** **189 testes, 91 verdes / 98 vermelhos** (2026-09-14), contra a base
de 168 / 70 / 98: os 21 testes novos de TASK-02.2 entraram todos verdes e a
lista de vermelhos é idêntica — zero regressão. Os vermelhos são a linha de
base — cenários de tasks ainda não implementadas —, e a suíte **não compila** na
árvore de trabalho, porque os cenários unitários nomeiam classes de tasks
posteriores; a medição exige excluí-los em cópia descartável. São 4 os arquivos
que restam nessa condição, um a menos desde que TASK-02.2 criou `EtapaService`,
`FluxoRequisicao` e `RegraDeNegocioViolada`.

**4 dos 5 testes de `ConfiguracaoDoFluxoIT` seguem vermelhos por dependência e
não por defeito:** eles exercitam o board e a criação de tarefa, que nascem em
TASK-02.6 e TASK-02.5. O fluxo de etapas só fica verificável de ponta a ponta
quando o épico fechar — é a mesma razão que mantém GATE-NFR reprovado.

**Pendência de spec viva:** Q-012 — decisão sobre claim ausente exige migration
nova e um recuo no autoprovisionamento fora do plano vigente. Dono `/tasks`,
não bloqueante.

---

## Gates atravessados

> Uma linha por gate, com o veredito final. O detalhe de cada achado está em
> [historico.md](historico.md); os relatórios de revisão, em `docs/review/`.

| Demanda | Gate | Data | Veredito |
| --- | --- | --- | --- |
| kanban-tarefas | GATE-INTENT | 2026-09-04 | aprovado — Thiago Goncalves Cavalcante (demandante) |
| kanban-tarefas | GATE-CLASSIFY | 2026-09-04 | aprovado — agente, sem override |
| kanban-tarefas | GATE-CONTEXT | 2026-09-04 | aprovado — 0 lacunas bloqueantes |
| kanban-tarefas | GATE-DIRECAO | 2026-09-04 | aprovado — Thiago Cavalcante, sem ressalvas |
| kanban-tarefas | GATE-SPEC | 2026-09-09 | aprovado — 4 reconfirmações de emenda até a v1.5 (2026-09-10); H-03 aberta por decisão da etapa |
| kanban-tarefas | GATE-PROVENIENCIA | 2026-09-09 | aprovado — 1 inferência (RNF-009) confirmada pelo demandante |
| kanban-tarefas | GATE-CONSISTENCIA | 2026-09-10 | **aprovado** na 9ª execução (`--pre-implement`) — 0 achados, 0 waiver. As 8 anteriores reprovaram; os 22 achados estão fechados |
| kanban-tarefas | GATE-RASTREABILIDADE | 2026-09-09 | aprovado — invariante cenário↔épico fechada nas duas direções |
| kanban-tarefas | GATE-VERIFICACAO-INDEPENDENTE | 2026-09-10 | aprovado — 0 arquivos de produção lidos ou escritos. **Reavaliar:** o plano v1.6 acrescentou `SubstituicaoDeFluxoConcorrenteIT` em modo `audit`, com 3 arquivos de produção lidos. A exceção está declarada no artefato; o gate precisa ser julgado sabendo dela |
| kanban-tarefas | GATE-GHERKIN-CONGELADO | 2026-09-10 | aprovado — 70/70 cobertos, 0 cenários alterados desde o gate de spec |
| kanban-tarefas | GATE-REVISAO-TECNICA | parcial | **reprovado por não haver épico fechado**, e não mais por achado bloqueante. 10 revisões parciais (TASK-01.1 a 01.8, 02.1, 02.2 duas vezes), 134 achados, 31 bloqueantes; **134 fechados e 0 abertos**. Os 23 achados da reexecução de 2026-09-14 foram fechados no mesmo dia, cada bloqueante com a verificação que o torna vermelho de novo |
| kanban-tarefas | GATE-NFR | parcial | **reprovado** — os envelopes só são mensuráveis no fechamento de épico. RNF-004, RNF-008 e RNF-010 medidos dentro. A reexecução de TASK-02.2 acrescentou uma segunda razão: SDR-005 introduziu espera por lock numa rota autenticada sem envelope de espera. **O envelope passou a existir em 2026-09-14** — 5 s de espera pelo bloqueio, 10 s de transação, 5 s por conexão —, mas está **declarado e não medido**, como os outros sete |

---

## Devoluções

> Contadores do anti-ping-pong. Ver o orçamento vigente na policy.

| Demanda | Por par | Por diamante | Total |
| --- | --- | --- | --- |

---

## Pendências conhecidas

| # | Pendência | Efeito |
| --- | --- | --- |
| 01 | Ambiente integrado efêmero para o gate E2E de épico multi-sistema. | Enquanto não existir, esse gate é aprovação manual. |
| 03 | `frontend/react` é stub na biblioteca; não governa este sistema. | Nenhum, enquanto nenhum sistema declarar a coleção. |
| 04 | Não existe mapa de domínios do sistema, embora o template de classificação o pressuponha. | O eixo Domínio de `kanban-tarefas` é provisório; reconciliar quando o mapa existir. |
| 05 | `.agents/skills/design/SKILL.md` (Fase 3) prescreve seções diferentes das que `validate-rules.json` exige. | Todo Design Brief escrito conforme a skill reprova no validador. Corrigir a fonte — provavelmente a lista da SKILL.md. |
| 06 | `check_proveniencia.py` reprova dúvida cujo Status não **comece** literalmente com `resolv`/`fechad`; negrito quebra. | Marcação em negrito bloqueia o GATE-SPEC sem que a causa apareça na mensagem. |
| 11 | `check_task_files` exige ao menos um item `- [ ]`, de modo que **task concluída sempre reprova**. | O validador do plano de tasks nunca mais sai em exit 0, e o sinal deixa de distinguir defeito de progresso. |
| 20 | `check_veredicto.py` lê a tabela de fechamento do relatório de revisão como se fosse a tabela de Achados: cada linha de fechamento vira quatro erros (severidade, local, descrição, destino). Descoberto em 2026-09-14, ao medir a pendência 18. Dono: ferramenta. | O validador do `/code-review` sai com dezenas de erros que não correspondem a defeito, e o sinal real fica submerso. |
| 21 | Não existe role de aplicação distinta do dono do schema: `BANCO_USUARIO` é o `POSTGRES_USER` da imagem, superusuário e dono. Toda concessão restrita do banco — a de `evento_tarefa` e a de `etapa`/`raia` — é inerte. Aberta em 2026-09-14 por TASK-02.3. Dono: `/tasks` (infra). | RNF-008 fica garantido de um lado só, o da aplicação, e os critérios 4 e 10 de TASK-02.3 não podem ser satisfeitos. |
| 22 | A suíte congelada se contradiz: `ImutabilidadeDoLogIT` exige que `TRUNCATE evento_tarefa` falhe para a credencial da aplicação, e `TesteDeIntegracao.esvaziarBanco` trunca todas as tabelas com a mesma credencial antes de cada teste. Aberta em 2026-09-14 por TASK-02.3. Dono: `/tests`, junto da 21. | Fechar a 21 sem decidir esta deixa **toda** a suíte de integração vermelha no `@BeforeEach`. |
| 16 | Em `docker/compose.yaml`, `PORTA_IDENTIDADE` e `IDENTIDADE_HOSTNAME` são independentes: porta deslocada sem hostname deslocado manda o navegador para outra stack da máquina. Dono `/tasks`/infra. | O sintoma aparece a um serviço de distância da causa. Custou uma sessão de diagnóstico em 2026-09-11. |
| 17 | O validador da TechSpec lê a menção a `TASK-01.6` em `kanban-tarefas-techspec.md:389` como planejamento de task. É rastreabilidade, não plano. Dono: ferramenta. | Toda revisão da TechSpec sai com um erro que não corresponde a defeito. |

**Resolvidas** (registro em [historico.md](historico.md)): 02 coleção
`infra/docker` elaborada · 07 `check_gherkin` em bloco cercado · 08 propagação
na TechSpec · 09 rota de criação de projeto · 10 `{{FEATURE}}` com sufixo
`-verificacao` · 13 `---` lido como patch no `/code-review` · 14 SCN-002.4 sem
teste, fechada em 2026-09-14 pelo `/tests` (plano v1.6, cobertura 70/70) · 15
RNF-005 sem dono · 12 prefixo de caminho em `check_escopo.py`, resolvida em
2026-09-14 por `git rev-parse --show-prefix` · 18 erro rebaixado a aviso em
`run_custom_steps`, resolvida em 2026-09-14 — a acusação original (exit 0 com
`ERRO:`) não se reproduziu nos scripts, mas o caminho que a produz existia no
`validate.py` · 19 drift de `.github/instructions/`, resolvida em 2026-09-14:
era fim de linha e não edição à mão, e `generate_platform.py` passou a gravar
em LF.

---

## Histórico

> Registro integral, com as razões de cada decisão, em
> [historico.md](historico.md). Abaixo, só a linha do tempo.

| Data | Mudança |
| --- | --- |
| 2026-09-14 | `/implement TASK-02.3` — o anel de verdade e o de projeção entram no esquema: quatro migrations (`tarefa` com a restrição de domínio de `condicao`, `evento_tarefa` com a concessão restrita, `intervalo_tarefa` e `impedimento` com os únicos parciais, a unicidade de `(projeto_id, seq)` e a `duracao` gerada), quatro entidades e quatro repositórios, nenhum deles publicando remoção física. **Três achados abertos na execução.** O primeiro e o segundo são o mesmo nó: os critérios 4 e 10 — a role de aplicação com apenas `SELECT, INSERT` no log e sem `DELETE` em `etapa`/`raia` — **não são satisfeitos**, porque a role de aplicação distinta continua não existindo; a aplicação conecta como o `POSTGRES_USER` da imagem, que é superusuário e dono, e contra ele todo `REVOKE` é inerte. É o que ACH-09 de TASK-02.1 previu ao recusar fazê-lo lá, supondo que aqui a role existiria. Pior: a suíte congelada **contradiz a si mesma** nesse ponto — `ImutabilidadeDoLogIT` exige que o `TRUNCATE` do log falhe para a credencial da aplicação, e `TesteDeIntegracao.esvaziarBanco` trunca todas as tabelas com **essa mesma credencial** antes de cada teste. Nenhuma configuração de privilégio satisfaz as duas. O terceiro é menor: `impedimento.resolvido_em` entrou no esquema **sem constar** da task nem de `data-model.md` §5, porque `ImpedimentoServiceTest` a exige. **Nada foi executado** — `mvn`, `docker` e JDK sumiram da máquina de novo, e o git recusa o repositório por *dubious ownership*, o que também impede `check_escopo.py` e o commit |
| 2026-09-14 | **Pendências 12, 18 e 19 fechadas, as três medidas no contêiner.** 12 era conversão de caminho: `git diff --name-only` fala da raiz do repositório e a tabela da task fala da raiz do sistema; o prefixo agora vem de `git rev-parse --show-prefix` e o que está fora do sistema sai da lista. 18 **não se reproduziu como descrita** — os scripts retornam 1 e o `validate.py` reprova —, mas o caminho que produz o sintoma existia: `run_custom_steps` classificava pela saída do step, e `ERRO:` com exit 0 virava `AVISO`; linha que se declara erro passa a ser erro, com `on_failure: warn` preservado. 19 **não era edição à mão**: o conteúdo é idêntico após normalizar o fim de linha, e o gerador produzia CRLF no Windows contra LF no Linux — `generate_platform.py` grava em `newline="\n"`, os derivados foram regenerados e `check_drift.py` ganhou `NAO_GERADOS` para o `settings.local.json` da IDE. `check_drift.py` sai em exit 0. Pendência **20** aberta no caminho: `check_veredicto.py` lê a tabela de fechamento como se fosse a de Achados |
| 2026-09-14 | `/tests --audit SDR-005` — `SubstituicaoDeFluxoConcorrenteIT` em `alem/`, a verificação além dos cenários que o DR declara como downstream: dois `PUT` simultâneos deixam o fluxo de **um** deles inteiro e nunca a mistura dos dois, e oito substituições que disputam as mesmas posições não produzem `5xx` nem ordem duplicada. A barreira de largada é o que torna a disputa real — sem ela o custo de subir cada thread serializa as requisições por acidente e o teste passaria verde sobre a implementação que existe para reprovar. **Modo `audit`, e a perda de independência está declarada no plano**: a classe nasceu depois do código e de sua leitura, o que é inevitável quando a decisão vem de achado de revisão. A contramedida foi asserir propriedade de desfecho e nunca mecanismo — trocar o bloqueio por outra coisa que cumpra a promessa mantém os dois verdes. Plano de verificação v1.6; 12 → 13 classes em "além dos cenários". Na mesma versão, **SCN-002.4 ganhou verificador e a cobertura fecha em 70/70** — a pendência 14, aberta desde a v1.1 porque o cenário exigia a tabela `etapa`, que nasceu em TASK-02.1, e o campo `fluxoConfigurado`, emitido em TASK-02.2. Os dois projetos são afirmados na mesma resposta, porque campo constante passaria em qualquer verificação que olhasse um de cada vez. **Sem execução**, pela mesma ausência de toolchain |
| 2026-09-14 | `/implement TASK-02.2 — SDR-005` — o bloqueio pessimista da linha de `projeto` entrou como primeira operação de `substituirFluxo`, antes da leitura do fluxo vigente. A porta ficou em `SubstituicaoDeFluxo.bloquearProjeto` e não em `ProjetoRepository` como o downstream do DR antecipava: `EtapaServiceTest` fixa `new EtapaService(EtapaRepositorio)` e é suíte congelada, de modo que colaborador novo no serviço não cabe. A decisão de SDR-005 é cumprida inteira; muda só a porta. **Sem medição** — não há `mvn`, `mvnw` nem Docker nesta máquina, e as tentativas anteriores mediam dentro do contêiner. Confirmar 168/70/98 é pré-condição da reexecução do `/code-review` |
| 2026-09-14 | **Os 19 achados não bloqueantes fechados — TASK-02.2 sai com 0 em aberto.** Três mãos. `/implement`: teto de corpo em filtro próprio (`LimiteDeCorpo`, `413`), com duas guardas porque `Content-Length` é opcional — `@Size` na lista só é avaliado depois que Jackson materializou o array, de modo que o teto de cem etapas protegia o banco e não a heap; `TarefasAtivasPorEtapa.contarEm` passa a receber a coleção inteira, e a mudança que importa é a **assinatura**, que torna a consulta única possível onde a anterior a tornava impossível, dentro de uma seção crítica com o projeto travado; mais três achados menores de log, mensagem e acentuação. `/techspec`: duas emendas a SDR-005 — a janela TOCTOU da permissão fica **decidida e mantida**, com as quatro condições que a tornam aceitável nesta rota nomeadas para que a próxima decisão não precise redescobri-las, e o `422 etapa-fora-do-fluxo` fica decidido como pretendido e explicitamente não `409`, porque `409` convidaria a retentar o corpo que falhará de novo. `/tests`: **18 testes novos**, doze deles sobre mecanismos que existiam desde o fechamento da primeira revisão sem nenhuma verificação — apagar qualquer um deixava a suíte verde. **A premissa de ACH-11 estava errada, e ficou registrada:** dois corpos com os mesmos `id` são ambos válidos, porque nada foi arquivado entre um e outro; o `422` exige que a primeira requisição **reduza** o fluxo. **Assimetria medida em quatro mutações, todas vermelhas:** sem `liberarOrdens`, sem `and e.arquivadaEm is null`, com o tradutor `409` tornado global, com o teto de corpo elevado acima do corpo de teste. Plano de verificação **v1.8** (13 → 16 classes além dos cenários). Suíte em **189 testes, 91 / 98**, zero regressão |
| 2026-09-14 | **ACH-02, ACH-03 e ACH-04 fechados pelo `/tests` — plano de verificação v1.7.** A correção é de uma palavra e desfaz três achados: as asserções de status de `SubstituicaoDeFluxoConcorrenteIT` passaram de `isLessThan(500)` a **`200` exato**. Serializadas, todas as requisições das duas verificações são válidas e nenhuma pode ser recusada; sem serialização a perdedora sai `409`, pelo tradutor nominal criado na mesma task, e `409` passa por baixo de qualquer faixa. Asserir propriedade do desfecho estava certo — asserir **faixa** de status não. ACH-02 foi fechado por declaração e não por conserto: a mistura dos dois corpos é **inalcançável**, porque as faixas de ordem se sobrepõem e toda intercalação colide antes no índice único parcial, e faixas disjuntas não são alternativa porque o contrato exige ordens contíguas a partir de 1; a asserção fica no arquivo nomeada como rede, não como prova. **Assimetria medida em três execuções:** como está, 2/2 verde; sem `bloquearProjeto`, 2/2 vermelho; com `bloquearProjeto` depois da leitura do fluxo vigente, 2/2 vermelho. A primeira tentativa de mutação de ACH-04 errou o alvo — movi o bloqueio para antes da leitura, não depois — e passou verde por motivo legítimo: o DR exige que ele preceda a **leitura**, não que seja a primeira instrução. Suíte em **171 testes, 73 / 98**, zero regressão. Com isso o épico não tem mais bloqueante aberto |
| 2026-09-14 | **O ambiente deixou de ser bloqueio.** Verificado que o contêiner resolve toda a toolchain ausente do host: JDK 25 e Maven (ADR-012), Testcontainers pelo socket montado, os validadores Python em `python:3.12-slim`, a suíte E2E do Playwright e o perfil `broadcast` de RNF-002 — para os dois últimos falta só copiar `docker/.env.example` para `.env`, já que o segredo `banco-senha.dev` existe. No Git Bash é preciso `MSYS_NO_PATHCONV=1`, senão o `-w` é manglado. **O que o contêiner não resolve:** as pendências 12 e 18, que são defeitos dentro de `check_escopo.py` e `validate.py` e sobrevivem a qualquer ambiente. E `check_drift.py`, rodado pela primeira vez em muito tempo, acusa **9 divergências** em `.github/instructions/` — derivado editado à mão ou fonte alterada sem regenerar. Dívida nova, não ambiente |
| 2026-09-14 | **A toolchain voltou, e a medição confirmou os bloqueantes.** Maven 3.9.14 e Docker 29.7.2 entraram no ambiente; o JDK 25 continua ausente do host e **não faz falta** — a suíte roda em `maven:3.9-eclipse-temurin-25` com o socket do daemon montado, que é o que ADR-012 decidiu. Quatro execuções: `compile` em BUILD SUCCESS com `release 25`; `verify` na árvore íntegra parando no `testCompile` nos **exatos 4** arquivos já registrados, nenhum novo; `verify` em cópia sem esses 4 dando **171 testes, 73/98**, zero regressão, com os 3 testes novos verdes; e o experimento decisivo — `SubstituicaoDeFluxoConcorrenteIT` **com `bloquearProjeto` removido fica 2/2 verde**. O log entrega o mecanismo que ACH-03 previra por leitura: 8 violações de `etapa_projeto_ordem_unico` traduzidas para `409` e absorvidas pelo `isLessThan(500)`. ACH-02, ACH-03 e ACH-04 passam de plausíveis a **confirmados**. Dado não previsto: a classe roda em 0,845 s com o bloqueio e 16,22 s sem ele — vinte vezes, o que mostra que na presença do bloqueio ela não chega a exercitar contenção (ACH-09). `check_escopo.py` segue sem rodar: o `python` do `PATH` é o atalho da Store |
| 2026-09-14 | **ACH-01 fechado**, nas duas mãos que o possuíam. `/implement`: o teto de espera de SDR-005 entrou em três camadas — `lock_timeout` de 5 s na sessão, `@Transactional(timeout = 10)` na rota, `connection-timeout` de 5 s no pool —, e não no hint `jakarta.persistence.lock.timeout`, cujo dialeto PostgreSQL só traduz espera zero: hint positivo seria mais um teto afirmado e não aplicado. Espera esgotada ganhou `503` com `Retry-After` e `type` próprio, porque com o teto a requisição deixa de pendurar e passa a terminar, e o que ela termina não pode sair como `500`; continua `5xx`, o que preserva a exigência da suíte congelada `TransacaoUnicaDeCriacaoIT`. `statement_timeout` ficou fora **declaradamente** — alcança toda consulta e não há medição para escolher o número. `/techspec`: SDR-005 emendado e **TechSpec v1.13**, porque a redação original atribuía o limite a um `timeout` de transação que não existia, e era essa garantia inexistente que sustentava o trade-off. Contrato de `sessao-e-projetos` com o `503`. **Sem execução**: `java` apareceu na máquina, mas é 21 contra o 25 do projeto, e seguem ausentes `mvn`, `mvnw`, Docker, `.m2` local e `python` de verdade |
| 2026-09-14 | `/code-review TASK-02.2` (reexecução) — **23 achados, 4 bloqueantes**, e a maioria sobre verificação e não sobre produção. O conserto dos 14 bloqueantes e relevantes de código da primeira revisão entrou **sem teste algum**: os tetos de `Etapa`, as três recusas de conjunto e o tradutor `409` não aparecem em lugar nenhum da suíte. E a única verificação escrita nesta mão fica **verde com o bloqueio de SDR-005 removido**, por três razões independentes — os dois corpos disputam as mesmas ordens, de modo que a perdedora colide no índice e reverte inteira, e a mistura que o teste afirma detectar é inalcançável; a asserção é `< 500`, que absorve o `409` que o tradutor da mesma task acabou de criar; e a ordem de aquisição, que é a cláusula central do DR, não é verificada. O quarto bloqueante é de produção: `PESSIMISTIC_WRITE` sem `lock timeout`, sem `statement_timeout` e sem timeout de transação, com Hikari no padrão e `db` na probe de readiness — a frase do DR sobre a espera ser limitada pelo timeout vigente descreve algo que não existe. Nada foi executado: não há toolchain na máquina, e a pré-condição "suíte verde" segue inverificável. Ver `docs/review/kanban-tarefas/TASK-02.2-review-r2.md` |
| 2026-09-14 | `/implement TASK-02.2 — achados` — os 14 achados de código fechados, entre eles os 5 bloqueantes desta mão. A forma do **conjunto** passou a ser recusada na borda, antes de qualquer escrita, e os tetos que faltavam viraram validação em vez de suposição: `Etapa.ORDEM_MAXIMA` torna a faixa de trabalho do passo intermediário inalcançável por requisição, `MAXIMO_DE_ETAPAS` e `TAMANHO_MAXIMO_DO_NOME` limitam o que uma escrita autenticada grava. O tradutor de restrição de banco que ACH-02 pedia nasceu **nominal** e não global: o global reprovou `TransacaoUnicaDeCriacaoIT`, que exige `5xx` para violação imprevista — e a exigência está certa, porque violação sem tradutor é defeito e não conflito. 168 testes, 70/98, lista de falhas idêntica à base: zero regressão |
| 2026-09-14 | Os 3 achados de TASK-02.2 sem dono no `/implement` resolvidos pelos donos. **ACH-05 → `/techspec`:** SDR-005 serializa a substituição de fluxo por bloqueio pessimista da linha de `projeto`. SDR-002 não alcançava o caso — o mecanismo de origem declarada pressupõe uma linha existente e versionada pelo cliente, e aqui a unidade é o conjunto: o dano aparece como **ausência**, a etapa que o outro criou e que não está no corpo de quem perdeu, sobre a qual `@Version` não tem o que conflitar. A Alternativa 2 daquele DR, recusada lá por transação longa em board colaborativo, é aceita aqui pelo perfil oposto — operação rara, administrativa, um projeto, teto de 100 etapas —, e trava a mesma linha que SDR-004 já incrementa, sem ponto de contenção novo. Fica **decidido** que a última configuração vence inteira e sem aviso: avisar exige campo de origem no corpo, que quebraria a suíte congelada e é escopo do `/prd`. TechSpec v1.12. **ACH-10 → `/tasks`:** a obrigação de publicar `TarefasAtivasPorEtapa` só existia no histórico da task que criou a porta, e histórico alheio não é instrução — vira item de execução, arquivo declarado e dois critérios de aceite em TASK-02.5, um deles o caso negativo das terminais. **ACH-11 → `/tests`:** a divergência de épico eram **19 cenários e não 3** — EPIC-06 e EPIC-07 estavam trocados em bloco, mais SCN-002.4 e SCN-003.3. A coluna foi alinhada ao plano de tasks, que é a fonte; plano de verificação v1.5 |
| 2026-09-14 | `/code-review TASK-02.2` — 17 achados, 6 bloqueantes. Quatro deles têm a mesma forma: entrada que o contrato manda recusar com `422` na borda sai como `500` depois de tentar escrever — `id` repetido, `ordem` duplicada, elemento nulo na lista e `ordem` que alcança a faixa de trabalho do passo intermediário. O quinto é ausência de decisão sobre concorrência (SDR-002 não aplicada a esta rota: sem `@Version` e sem lock, dois `PUT` simultâneos perdem escrita em silêncio) e o sexto é falta de teto para o que uma escrita autenticada pode gravar. A task cumpriu a própria regra — recusa antes de qualquer escrita — para as duas recusas que conhece, e deixou de fora quatro que o corpo pode produzir |
| 2026-09-14 | `/implement TASK-02.2` — consulta e substituição do fluxo de etapas. A substituição é atômica e roda em **dois passos**, porque `etapa_projeto_ordem_unico` é único parcial e não é adiável: o passo intermediário desloca as ordens vigentes antes de reatribuí-las, exatamente como ACH-07 da revisão de 02.1 previu na migration. `fluxoConfigurado` passa a ser emitido em `GET /v1/projetos`, derivado por `EXISTS` na mesma consulta, e `AusenciaDeNMaisUmIT` segue verde. Critério 10 devolve à tela de criação a frase que ACH-07 de 01.7 retirou, e `fluxoConfigurado` deixa de ser opcional no tipo do cliente. 168 testes, 70/98 — nenhuma regressão. Descoberta a pendência 18 |
| 2026-09-14 | Os 14 achados da revisão de TASK-02.1 fechados. Destaques: TechSpec v1.11 decide a garantia **estreita** da raia (`tarefa.raia_id` existe; o que nenhuma tabela carrega é raia na série de tempo); repositórios trocam `JpaRepository` por `Repository`, tornando a ausência de remoção física propriedade da interface; migration nova em vez de editar a aplicada, para não quebrar o checksum do Flyway; `EsquemaDoFluxoIT` dá ao esquema a cobertura que `ddl-auto=validate` não alcança, com poder de falha provado. 163 testes, 64/99 — nenhuma regressão |
| 2026-09-14 | `/code-review TASK-02.1` — 14 achados, 4 bloqueantes. Todos na distância entre o que os arquivos afirmam sobre si e o que garantem |
| 2026-09-11 | `/implement TASK-02.1` — EPIC-02 aberto: esquema de `etapa` e `raia`, entidades e repositórios |
| 2026-09-11 | EPIC-01 fechado. Revisões de TASK-01.5 a 01.8 conduzidas e todos os achados corrigidos, inclusive os 6 bloqueantes de TASK-01.7 (redirecionador aberto, CVEs de produção, cabeçalhos de segurança) e os 2 de TASK-01.8 (escrita antes do `403`, transação única sem poder de falha) |
| 2026-09-10 | `/implement` de TASK-01.1 a 01.6 e as respectivas revisões; ambiente Docker, segurança, sessão autenticada, RBAC e tratamento de erro em pé |
| 2026-09-10 | `/tests` — suíte congelada v1.0; `/analyze --pre-implement` aprova o GATE-CONSISTENCIA na 9ª execução |
| 2026-09-09 | `/prd` → `/techspec` → `/analyze` → `/tasks`: PRD congelado no GATE-SPEC, TechSpec com 4 SDRs, 8 épicos e 43 tasks. Coleção `infra/docker` elaborada (ADR-011/012/013) |
| 2026-09-04 | `/intent` → `/classify` → `/context` → `/discovery` → `/shape` → `/solution` → `/design`: demanda `kanban-tarefas` percorre a camada de Controle e o primeiro diamante |
| 2026-09-03 | Sistema inicializado via `init.py`; skills e flows migrados para o contrato novo |
