# Revisão técnica — TASK-02.2, reexecução (revisão parcial de task, não fecha o EPIC-02)

_Data: 2026-09-14 | Revisor: agente `/code-review` | Épico: EPIC-02 | PR: n/a (commit direto em `v202609041`)_
_Commits revisados: be09e10..árvore de trabalho (19 arquivos sujos, 3 não rastreados)_

> O revisor não escreve código. Achado é devolvido a quem implementa; correção
> feita pelo revisor faz o revisor revisar a si mesmo.

Esta é a **segunda** revisão de TASK-02.2. A primeira está em
[TASK-02.2-review.md](TASK-02.2-review.md) e permanece como registro: seus 17
achados estão fechados. Esta reexecução revisa o que entrou **para fechá-los** —
os 14 de código, SDR-005 e as duas verificações novas — e não repete o recorte
anterior.

---

## Escopo revisado

| Item | Valor |
| --- | --- |
| Tasks | TASK-02.2 |
| Cenários entregues | SCN-017.2 e SCN-002.4 (verificáveis hoje); SCN-017.1 e SCN-017.3 declarados, não executáveis nesta task |
| Arquivos | 7 de produção alterados, 1 de teste alterado, 1 de teste criado, 1 DR criada, 6 de registro |
| Suíte | **171 testes, 73 verdes / 98 vermelhos** — medido em 2026-09-14, contra a base de 168 / 70 / 98 |

- **Cenários congelados alterados no período:** nenhum
- **Step definitions alterados no período:** nenhum

O único arquivo de teste pré-existente tocado é `SessaoEProjetosIT`, e a mudança
é **acréscimo** de um método, feita pelo `/tests` e registrada como emenda v1.6
do plano de verificação. A Fase 0 passa.

### Medição — suprida em 2026-09-14, depois de escrito o corpo desta revisão

Este relatório foi redigido sem toolchain: nem `mvn`, nem Docker. A toolchain
voltou no mesmo dia, e **tudo abaixo passou a ser medido**. A rota é a do
ADR-012 — a suíte roda em `maven:3.9-eclipse-temurin-25` com o socket do daemon
do host montado —, de modo que o JDK 25 nunca precisou existir na máquina.

| Execução | Resultado |
| --- | --- |
| `mvn compile` | **BUILD SUCCESS**, 36 fontes, `release 25` |
| `mvn verify`, árvore íntegra | para no `testCompile`, nos **exatos 4** arquivos já registrados em `state.md` — nenhum novo |
| `mvn verify`, cópia sem esses 4 | **171 testes, 73 verdes / 98 vermelhos.** Base: 168 / 70 / 98. Lista de vermelhos idêntica: **zero regressão**. Os 3 testes novos verdes |
| `SubstituicaoDeFluxoConcorrenteIT` **com `bloquearProjeto` removido** | **2/2 verde** |

A última linha é o experimento que fecha ACH-02, ACH-03 e ACH-04, e o log ainda
entrega o mecanismo: **8 violações** de `etapa_projeto_ordem_unico` durante a
execução, todas traduzidas para `409` pelo tradutor nominal criado nesta mesma
task, todas abaixo do `isLessThan(500)`. Era exatamente a cadeia descrita por
leitura. Os três achados passam de **plausíveis** a **confirmados**.

Um quinto dado, não previsto: com o bloqueio, a classe roda em **0,845 s**; sem
ele, em **16,22 s**. A diferença de vinte vezes é a disputa que só existe quando
não há serialização — e confirma por outro caminho que, na presença do bloqueio,
os testes não chegam a exercitar contenção alguma (ACH-09).

`check_escopo.py` segue sem rodar: o `python` do `PATH` é o atalho da Microsoft
Store, que não executa (pendências 12 e 18 permanecem).

---

## Conformidade com a especificação

| Cenário | Passa | Implementado conforme a task | Observação |
| --- | --- | --- | --- |
| SCN-002.4 | não medido | sim | Coberto por `SessaoEProjetosIT:140-162`, com os dois projetos na mesma resposta — a forma que recusa campo constante. Lacuna adjacente em ACH-15 |
| SCN-017.2 | não medido | sim | Sem mudança desde a primeira revisão |
| SDR-005 | **não** | parcial | O mecanismo entrou; a garantia **não está verificada** (ACH-02, ACH-03, ACH-04) e uma premissa do DR é falsa no ambiente (ACH-01) |

- **Escopo além do especificado:** nenhum. O desvio de `ProjetoRepository` para
  `SubstituicaoDeFluxo.bloquearProjeto` está declarado no javadoc, no histórico
  da task e em `state.md` — é desvio de meio, não de escopo.

---

## Envelopes de RNF

| RNF | Envelope | Medido | Como | Situação |
| --- | --- | --- | --- | --- |
| RNF-004 | permissões derivadas no servidor | sim | `SessaoEProjetosIT` | dentro |
| RNF-008, RNF-010 | — | sim (1ª revisão) | inalterados | dentro |
| demais 7 | — | — | só mensuráveis no fechamento do épico | não medido |
| **disponibilidade sob escrita concorrente** | inexistente | — | — | **sem envelope** — ver ACH-01 |

SDR-005 introduziu espera por lock numa rota autenticada e **não declarou teto
de espera**. Não há RNF que cubra isso, e a ausência é o achado.

---

## Achados

| ID | Severidade | Tipo | Local | Descrição | Destino |
| --- | --- | --- | --- | --- | --- |
| ACH-01 | bloqueante | segurança | `EtapaRepositorioImpl` (bloqueio) + `application.yml` | `PESSIMISTIC_WRITE` sem `jakarta.persistence.lock.timeout`, sem `@Transactional(timeout)`, e sem `lock_timeout`, `statement_timeout` ou `idle_in_transaction_session_timeout` na configuração. Espera é ilimitada e cada requisição em espera retém conexão do pool (Hikari no padrão, 10). Um sujeito autenticado com `CONFIGURAR` esgota o pool; o grupo de readiness inclui `db`, então a indisponibilidade alcança a probe. SDR-005 afirma que a espera é limitada pelo timeout de transação vigente — **esse timeout não existe** | /implement + /techspec |
| ACH-02 | bloqueante | teste | `SubstituicaoDeFluxoConcorrenteIT:53-70` | A mistura que o teste diz detectar é inalcançável com o corpo que ele envia: `CORPO_A` ocupa 1..3 e `CORPO_B` 1..2, faixas sobrepostas, de modo que toda intercalação perdedora colide antes no índice único parcial e a transação reverte inteira. `isIn(CORPO_A, CORPO_B)` é satisfeito **com e sem** SDR-005 | /tests |
| ACH-03 | bloqueante | teste | `SubstituicaoDeFluxoConcorrenteIT:60-61, 90-91` | As asserções são `isLessThan(500)`, mas o tradutor nominal desta mesma task converte a violação de `etapa_projeto_ordem_unico` em **409** — menor que 500. O sintoma que o javadoc da classe nomeia passa pela asserção. Sob serialização toda requisição concorrente é válida e deve responder `200`; `< 500` é o único predicado que não distingue os dois mundos | /tests |
| ACH-04 | bloqueante | teste | `SubstituicaoDeFluxoConcorrenteIT` (classe) | A cláusula central de SDR-005 — bloqueio como **primeira** operação, antes da leitura do fluxo vigente — não tem verificação. Mover a chamada para depois da leitura, que é o defeito clássico, mantém os dois testes verdes. O que a classe verifica é "não sai 5xx", propriedade que a transação sozinha já entrega | /tests |
| ACH-05 | relevante | segurança | `FluxoRequisicao` / configuração | Corpo de requisição sem teto de tamanho: `@Size` só é avaliado depois que Jackson materializa o array inteiro, logo o teto protege o banco e não a heap. Alcança toda rota JSON, não só esta | /implement |
| ACH-06 | relevante | código | `SubstituicaoDeFluxo` (javadoc de `bloquearProjeto`) | O javadoc afirma compatibilidade de ordem de aquisição com SDR-004 descrevendo código que **ainda não existe**. É a mesma forma de defeito que a primeira revisão nomeou — a distância entre o que o arquivo afirma sobre si e o que garante | /implement |
| ACH-07 | relevante | dados | `EtapaService` (laço de validação do conjunto) | Consulta por etapa dentro da seção crítica: até 100 idas ao banco com a linha de `projeto` travada. O custo do lock é proporcional a um N+1 evitável | /implement |
| ACH-08 | relevante | segurança | `EtapaController` (recusa de permissão) | A resolução de permissão acontece fora da transação que escreve. Entre a decisão e a escrita há janela em que a participação pode mudar (TOCTOU). Decisão de desenho, não conserto local | /techspec |
| ACH-09 | relevante | teste | `SubstituicaoDeFluxoConcorrenteIT` (largada) | A barreira alinha a largada da **requisição**, não a seção crítica: filtro de segurança, resolução de usuário, de permissão e existência do projeto ficam entre a barreira e a transação, e a variância acumulada aí é ordens de grandeza maior que a janela disputada. Sem repetição e sem pressão sustentada. O precedente da casa (`SeqSobConcorrenciaIT`) tira poder de falha do volume e de uma invariante determinística, não do alinhamento | /tests |
| ACH-10 | relevante | teste | `SubstituicaoDeFluxoConcorrenteIT` (javadoc de `configurar`) | O javadoc justifica enviar as etapas **sem `id`** como sendo o que faz a perda silenciosa aparecer. É o inverso: sem `id` cada requisição só cria e arquiva, e o desfecho de qualquer corrida é colisão de ordem. A perda por ausência exige corpos que carreguem os `id` vigentes | /tests |
| ACH-11 | relevante | teste | — (ausência) | O caminho de concorrência **com `id`** no corpo não tem cobertura nem decisão registrada. Com o bloqueio em pé o desfecho real é `422 etapa-fora-do-fluxo` para a segunda, e nada diz se esse é o desfecho pretendido | /tests + /techspec |
| ACH-12 | relevante | teste | — (ausência) | O tradutor `409` de `TratadorDeErro` não tem teste algum. Apagá-lo, ou inverter a condição nominal, não é detectado. É o branch que decide ACH-03 | /tests |
| ACH-13 | relevante | teste | `Cenario.fluxoPadrao` | O helper não assere status e extrai `$..id` por varredura: sobre um `problem+json` devolve lista vazia sem levantar. Se a semeadura falhar, os dois testes de concorrência rodam sobre projeto **sem fluxo vigente**, o passo intermediário nunca roda, e as asserções seguem verdes. O defeito do helper é anterior; estes são os primeiros testes cuja tese depende inteiramente dele | /tests |
| ACH-14 | relevante | teste | `ProjetoRepository` (duas subconsultas de `fluxoConfigurado`) | A cláusula `and e.arquivadaEm is null` não tem poder de falha: nenhum teste tem projeto com **todas** as etapas arquivadas, que é o único estado que distingue as duas versões. Projeto esvaziado voltaria marcado como configurado | /tests |
| ACH-15 | relevante | teste | — (ausência) | `MAXIMO_DE_ETAPAS`, `TAMANHO_MAXIMO_DO_NOME`, `ORDEM_MAXIMA` e as recusas de conjunto (`etapa-repetida`, `ordem-repetida`, `etapa-ausente-na-lista`) não aparecem em teste algum. São exatamente os itens criados para fechar os bloqueantes da primeira revisão: **o conserto entrou sem verificação** | /tests |
| ACH-16 | menor | segurança | `TratadorDeErro` (log do caminho não previsto) | Exceção completa registrada em nível `INFO` num caminho alcançável pelo cliente | /implement |
| ACH-17 | menor | segurança | `EtapaRepositorioImpl` (recusa de projeto ausente) | `projetoId` concatenado na mensagem de `IllegalStateException` | /implement |
| ACH-18 | menor | código | `TratadorDeErro` (mensagens do 409) | "Conflito na posicao das etapas" e "A configuracao conflita…" são as **únicas** mensagens de usuário sem acento no sistema; todas as demais, inclusive as criadas nesta mesma task em `EtapaService` e `Etapa`, são acentuadas | /implement |
| ACH-19 | menor | teste | `SubstituicaoDeFluxoConcorrenteIT` (largada e coleta) | Nenhum limite de tempo: `await()` e `get()` sem timeout, sem `@Timeout`, e o lock sem timeout (ACH-01). Não há deadlock na forma atual — partes da barreira iguais ao tamanho do pool, ordem de aquisição única —, mas qualquer lock preso trava a build sem diagnóstico. O executor não vaza | /tests |
| ACH-20 | menor | teste | `SubstituicaoDeFluxoConcorrenteIT` (pool de threads) | 8 threads contra Hikari no padrão (10): folga nula. Elevar o número de permutações troca disputa por espera de conexão, e o sintoma seria lentidão, não falha | /tests |
| ACH-21 | menor | teste | `SubstituicaoDeFluxoConcorrenteIT` (leitura do fluxo) | Leitura sem asserção de status e por varredura profunda (`$..nome`, `$..ordem`), que sobre corpo de erro devolvem lista vazia. Não há verde falso, mas o diagnóstico aponta para o lugar errado. O resto da suíte usa caminho definido | /tests |
| ACH-22 | menor | teste | `EtapaRepositorioImpl` (duas recusas de estado) | Projeto ausente no bloqueio e etapa de outro projeto na descarga: guardas legítimas, inalcançáveis pela borda, sem verificação — mas figuram no registro da task como garantias | /tests |
| ACH-23 | menor | teste | `EtapaService.haDisputaDeOrdem` | O predicado decide se o passo intermediário roda, e nenhum teste o endereça. Erro nele reintroduz o `500` de colisão só em reordenações específicas | /tests |

### Fechamento

| Achado | Situação | Quando |
| --- | --- | --- |
| ACH-01 | **fechado** pelo `/implement` (três tetos, `503` com `Retry-After`) e pelo `/techspec` (emenda de SDR-005, TechSpec v1.13, nota de contrato). **Verificado**: compila, e a suíte segue em zero regressão — inclusive `TransacaoUnicaDeCriacaoIT`, que era o risco de colisão com o tratador novo | 2026-09-14 |
| ACH-02, ACH-03, ACH-04 | **fechados** pelo `/tests` (plano v1.7). As asserções de status passaram de `isLessThan(500)` a `200` exato, e ACH-02 foi fechado por declaração no arquivo — a mistura é inalcançável com ordens contíguas, e a asserção fica como rede e não como prova. **Verificados pela assimetria, em três execuções:** código como está, 2/2 verde; sem `bloquearProjeto`, 2/2 vermelho; com `bloquearProjeto` depois da leitura do fluxo vigente, 2/2 vermelho. Suíte em 171 / 73 / 98, zero regressão | 2026-09-14 |
| ACH-05, ACH-06, ACH-07, ACH-16, ACH-17, ACH-18 | **fechados** pelo `/implement`. Teto de corpo em filtro próprio (`LimiteDeCorpo`, `413 corpo-grande-demais`), duas guardas porque `Content-Length` é opcional; javadoc de `bloquearProjeto` sem a afirmação sobre código futuro; `TarefasAtivasPorEtapa.contarEm` recebe a coleção inteira, e é a assinatura que torna a consulta única possível; log do caminho inválido rebaixado a `INFO`/`DEBUG`; `projetoId` fora da mensagem de exceção; acentuação uniformizada em todo o `TratadorDeErro`, e não só nas duas mensagens do `409` | 2026-09-14 |
| ACH-08, ACH-11 (metade de desenho) | **fechados** pelo `/techspec`, em duas emendas a SDR-005. A janela TOCTOU da permissão fica **decidida e mantida**, com as quatro condições que a tornam aceitável aqui nomeadas para que a próxima decisão não as redescubra. O desfecho `422 etapa-fora-do-fluxo` fica decidido como pretendido, e explicitamente **não** `409` | 2026-09-14 |
| ACH-09, ACH-10, ACH-13, ACH-19, ACH-20, ACH-21 | **fechados** pelo `/tests` em `SubstituicaoDeFluxoConcorrenteIT` e `Cenario`. Repetição de 5 rodadas contra a variância da largada; javadoc de `configurar` corrigido na direção inversa; `Cenario.executarHttp` falha rápido em qualquer não-2xx e `fluxoPadrao` lê por caminho definido; tetos em barreira, `Future.get` e `@Timeout`; a relação entre `PERMUTACOES` e `maximum-pool-size` declarada no arquivo, com o que quebra ao aumentá-la; leitura do fluxo exige `200` antes de extrair | 2026-09-14 |
| ACH-11 (metade de teste), ACH-12, ACH-14, ACH-15, ACH-22, ACH-23 | **fechados** pelo `/tests` com verificação nova: `LimitesDaConfiguracaoDoFluxoIT` (11 testes), `TradutorDeIntegridadeTest` (3) e `TetoDeCorpoIT` (2). A premissa inicial de ACH-11 estava errada e o arquivo registra a correção: dois corpos com os mesmos `id` são **ambos válidos**; o `422` exige que a primeira requisição **reduza** o fluxo. **Assimetria medida em quatro mutações, todas vermelhas:** sem `liberarOrdens`, sem `and e.arquivadaEm is null`, com o tradutor `409` tornado global, e com o teto de corpo elevado acima do corpo de teste | 2026-09-14 |
| — | **0 abertos** | — |

Nota de precisão levantada pela medição de ACH-04: mover `bloquearProjeto` para
depois das validações de forma da requisição, que ainda antecedem a leitura,
mantém os dois verdes — e deve manter. O que SDR-005 exige é que o bloqueio
preceda a **leitura do fluxo vigente**, não que seja a primeira instrução do
método. A primeira tentativa de mutação errou esse alvo e passou verde por
motivo legítimo.

**Severidade:**

- **bloqueante** — impede o merge. Correção obrigatória antes do gate.
- **relevante** — merge permitido com registro; entra no backlog com prazo.
- **menor** — registrado, sem prazo.

### O que estes achados têm em comum

Dezesseis dos vinte e três são sobre **verificação**, e onze deles falam da
mesma coisa por ângulos diferentes: o conserto dos bloqueantes da primeira
revisão entrou sem teste, e a verificação que foi escrita — a única desta mão —
não reprova a ausência daquilo que ela existe para provar. Removida a
serialização de SDR-005, a suíte inteira segue verde. É o defeito que a própria
classe de teste declara, na abertura do seu javadoc, existir para impedir.

A primeira revisão nomeou o padrão como "a distância entre o que os arquivos
afirmam sobre si e o que garantem". Ele reaparece aqui em três lugares novos:
no javadoc de `bloquearProjeto` (ACH-06), no javadoc da fixture (ACH-10) e no
próprio DR, cuja frase sobre limite de espera descreve um timeout que não existe
em lugar nenhum da configuração (ACH-01).

---

## Guardrails

| Guardrail | Origem | Onde passa a valer |
| --- | --- | --- |
| Teste de concorrência prova o mecanismo pela **assimetria**: com o mecanismo removido, vermelho. Enquanto ninguém demonstrar essa rodada, o teste não é evidência — é afirmação | ACH-02, ACH-03, ACH-04 | `guidelines/_shared/verificacao-de-concorrencia.md` |
| Asserção de disputa concorrente fixa o status **exato** que a serialização produz, nunca uma faixa. Faixa absorve o código que o tradutor de erro acabou de criar | ACH-03 | `guidelines/_shared/verificacao-de-concorrencia.md` |
| Bloqueio pessimista só entra junto com o teto de espera: `lock timeout` na consulta e `statement_timeout` na sessão. Lock sem teto converte contenção em indisponibilidade, e o pool é o recurso que acaba primeiro | ACH-01 | `guidelines/backend/java/persistencia.md` |
| Achado bloqueante fechado por código novo traz **a verificação que o torna vermelho de novo**. Conserto sem teste é o mesmo achado com data posterior | ACH-15 | `guidelines/_shared/revisao.md` |
| Helper de semeadura de cenário assere o status da chamada que faz. Extração por varredura profunda sobre resposta de erro devolve vazio e transforma pré-condição quebrada em teste verde | ACH-13, ACH-21 | `guidelines/backend/java/testes.md` |
| Javadoc que justifica uma escolha de fixture é revisado como código: justificativa invertida em prosa afirmativa desencoraja a correção | ACH-10 | `guidelines/_shared/testes.md` |
| Afirmação sobre interação com código que ainda não existe não entra em javadoc. Ela entra no DR, onde tem data e status | ACH-06 | `guidelines/_shared/documentacao-no-codigo.md` |

---

## Veredicto

- **GATE-REVISAO-TECNICA:** reprovado
- **GATE-NFR:** reprovado
- **Bloqueantes em aberto:** 0 — os quatro fechados em 2026-09-14, e os quatro verificados por execução
- **Revisor humano:** pendente

GATE-REVISAO-TECNICA reprovava pelos quatro bloqueantes, e a reprovação não era
cautelar: era medida. O experimento com o bloqueio removido devolveu verde, que é
a definição operacional de teste sem poder de falha. **Os quatro estão fechados**,
e cada fechamento traz a verificação que o torna vermelho de novo — o guardrail
que esta revisão escreveu. **Os 23 estão fechados**, e o gate volta a depender
apenas do fechamento do épico, como antes desta revisão.

O fechamento dos 19 não bloqueantes acrescentou 17 verificações — a suíte vai de
171 para **189 testes, 91 verdes / 98 vermelhos**, com a lista de vermelhos
idêntica à linha de base. Quatro delas tiveram a assimetria medida por mutação
(passo intermediário de ordens, filtro de arquivadas em `fluxoConfigurado`,
tradutor nominal de `409`, teto de corpo), e as quatro ficam vermelhas quando o
mecanismo sai. O guardrail de ACH-15 — conserto sem teste é o mesmo achado com
data posterior — foi aplicado a si mesmo.

A pré-condição de medição, que este relatório declarava não satisfeita quando
foi escrito, **está satisfeita**.

GATE-NFR reprova pela razão de sempre nesta altura — sete dos dez envelopes só
se tornam mensuráveis no fechamento do épico — e por uma nova: SDR-005
introduziu espera por lock numa rota autenticada sem envelope de espera.

> Bloqueante em aberto reprova, sem exceção e sem waiver: a policy proíbe waiver
> para verificação independente e para dados.

---

## Fora deste artefato — regras negativas

- **Correção de código** — pertence ao `/implement`.
- **Correção de teste** — pertence ao `/tests`. Quinze dos vinte e três achados
  são dele, e o revisor não escreve nem ajusta asserção.
- **Decisão de arquitetura** — pertence ao `/techspec`. ACH-08 é dela, e a
  metade de desenho de ACH-01 e ACH-11 também.
- **Cenário Gherkin** — congelado; alteração exige emenda no PRD.
