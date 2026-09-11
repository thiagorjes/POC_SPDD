# Revisão técnica — TASK-01.6

_Data: 2026-09-11 | Revisor: agente `/code-review` | Épico: EPIC-01 | PR: n/a (revisão parcial de task)_
_Commits revisados: 103f667..a1be014_

> O revisor não escreve código. Achado é devolvido a quem implementa; correção
> feita pelo revisor faz o revisor revisar a si mesmo.

---

## Escopo revisado

| Item | Valor |
| --- | --- |
| Tasks | TASK-01.6 |
| Cenários entregues | SCN-002.3 |
| Arquivos | 5 de produção (4 criados, 1 alterado), 2 de teste fora do escopo declarado |
| Suíte | 147 testes, 101 falhando |

- **Cenários congelados alterados no período:** nenhum
- **Step definitions alterados no período:** nenhum

> Qualquer das duas linhas diferente de "nenhum" sem emenda registrada reprova
> o GATE-VERIFICACAO-INDEPENDENTE, e a revisão para aqui.

A Fase 0 teve linha de base real pela quarta vez seguida. `git diff --name-only
8346946..HEAD` sobre `*.feature` devolve vazio, e a única mudança na árvore de
teste atribuível a esta task é acréscimo: `ErrosELimitesIT.java` e
`application-test.yml`, ambos declarados no histórico da task como fora do
escopo. Nenhum arquivo `.feature`, step definition ou asserção congelada foi
tocado.

**Medição própria, em contêiner `maven:3.9-eclipse-temurin-25`** — o JDK local
não suporta *release* 25. A suíte inteira continua não compilando pelos mesmos
cinco arquivos de tasks posteriores (`ReconstrucaoDaProjecaoIT`,
`ImpedimentoServiceTest`, `EtapaServiceTest`, `CriacaoDeTarefaServiceTest`,
`TomadaServiceTest`), então a execução foi feita sobre cópia da árvore sem esses
cinco, sem tocar no repositório — mesmo arnês de ACH-07 da revisão de TASK-01.3.
Resultado: 20 unitários verdes e 127 de integração com 23 falhas e 78 erros, isto
é, **147 testes, 46 verdes / 101 vermelhos**. A aritmética confere com o que a
task declara, nos dois sentidos. `ErrosELimitesIT` isolado: **8/8 verdes**. Os
101 vermelhos são rotas de tasks posteriores, a maioria parando em
`Cenario.fluxoPadrao` (EPIC-02) — Red legítimo, nenhuma regressão.

---

## Conformidade com a especificação

| Cenário | Passa | Implementado conforme a task | Observação |
| --- | --- | --- | --- |
| SCN-002.3 | sim | sim | Verde em `SessaoEProjetosIT`. O corpo do `404` de projeto de terceiro é genérico e não nomeia o recurso — critério de aceite 5 medido, não inspecionado |

- **Escopo além do especificado:** nenhum

Os cinco arquivos de produção correspondem à tabela do guia técnico, e as quatro
decisões que o histórico da task registra são verificáveis no código. Três delas
resistem à conferência e valem registro como acerto. O `traceId` entra por
`ResponseBodyAdvice` no caminho de saída, e não como obrigação de cada rota — o
que fecha o escopo declarado sem editar `ProjetoController`, que está fora da
tabela de arquivos, e faz a rota que ainda vai nascer sair correta. A ordem dos
dois filtros está como descrita e pela razão descrita: a correlação em
`HIGHEST_PRECEDENCE` fora da cadeia de segurança, porque `401`, `503` e `429`
nascem dentro dela e nunca chegam ao despachante; o limite depois do filtro do
bearer token, porque a contagem é por sujeito autenticado. E o ponto de extensão
do `409` está previsto e não serializado, como o ponto de atenção da task exigia:
`ProblemaDetalhado.Falha` carrega um mapa de membros adicionais, por onde o bloco
`estadoAtual` entra sem reabrir o tratador.

A aritmética do contador foi conferida e **não tem erro de contorno**. O teto é
"mais de N": com 120, a 120ª leitura passa e a 121ª é recusada, que é literalmente
o que o critério 3 pede. O `Retry-After` usa piso 1 e nunca sai zero nem negativo.
A dimensão de origem é sempre contada mesmo quando o sujeito já estourou, o que é
o comportamento correto e está comentado no lugar certo.

O que não se sustenta está em volta: a premissa em que a dimensão de origem se
apoia é falsa contra o compose em disco (ACH-01), e o critério de aceite que dá
sentido à task inteira não tem verificação (ACH-02).

---

## Envelopes de RNF

| RNF | Envelope | Medido | Como | Situação |
| --- | --- | --- | --- | --- |
| RNF-010 | 120 leituras e 30 escritas por minuto por sujeito; excedente recusado com razão e indicação de quando repetir | mecanismo verde em tetos de 3/2/5/4; os números de produção nunca exercitados | `ErrosELimitesIT` com relógio parado e envelope apertado por `@TestPropertySource` | dentro do envelope após o encaminhamento — era fora na revisão, ver ACH-05 e a seção Encaminhamento |
| RNF-004 | 100% das escritas reavaliadas no servidor | não medido | não há escrita implementada antes do EPIC-02 | não medido |
| RNF-001 | p95 de 2 s para propagação | não medido | instrumentação nasce no épico do broadcast | não medido |
| RNF-002 | três instâncias simultâneas | não medido | arnês existe em `compose.test.yaml`, sem rota que o exercite | não medido |

> RNF não medido não passa por omissão. Se a instrumentação não existe, o
> GATE-NFR reprova — a ausência de medição é o achado.

RNF-010 é o único envelope que esta task poderia fechar, e ele sai **fora** por
duas razões distintas. A primeira é de cobertura: os tetos de produção não têm
guarda alguma, e trocar 120 por 5 em `SegurancaConfig` deixa a suíte inteira verde
(ACH-07). A segunda é de contrato, e é mais séria: a condição de medição que o PRD
escreveu para RNF-010 exige verificar "que o consumo de um sujeito não afeta a
resposta de outro", e a dimensão de origem faz exatamente o contrário (ACH-05).
Os demais RNFs continuam sem instrumentação pela razão das quatro revisões
anteriores — nenhum é mensurável antes do fechamento do EPIC-01.

> **Atualização após o encaminhamento.** Removida a dimensão de origem, a segunda
> razão deixou de existir: a não contaminação entre sujeitos passa a ser afirmada
> por `ErrosELimitesIT.sujeitosNaoSeContaminam`, que é literalmente a condição de
> medição escrita no PRD. RNF-010 volta a estar **dentro** do envelope quanto ao
> contrato; os números de produção seguem não exercitados, o que é limitação
> conhecida e não contradição.

---

## Achados

| ID | Severidade | Tipo | Local | Descrição | Destino |
| --- | --- | --- | --- | --- | --- |
| ACH-01 | bloqueante | segurança | `backend/src/main/java/br/com/idsd/kanban/shared/LimiteDeRequisicoes.java:40`, `:132`, `:160` | A dimensão de origem se apoia em premissa falsa e produz três defeitos de uma vez: é evadível, permite negar serviço a terceiros e faz o mapa crescer sem limite | /implement |
| ACH-02 | bloqueante | código | `backend/src/test/java/br/com/idsd/kanban/alem/ErrosELimitesIT.java`, `backend/src/main/resources/application.yml` | O critério de aceite 6 está marcado cumprido e não tem verificação alguma; o padrão de log não carrega o `traceId`, de modo que a correlação vale só nas linhas escritas à mão | /implement |
| ACH-03 | relevante | segurança | `backend/src/main/java/br/com/idsd/kanban/shared/TratadorDeErro.java:98` | `@ExceptionHandler(Exception.class)` capturará `AccessDeniedException` no dia em que houver segurança de método, e a negação sairá `500` em vez de `403`. Rebaixado a relevante porque falha fechada — nega em `500`, nunca concede — e não há segurança de método no código hoje, de modo que o caminho é inalcançável; aprovador do rebaixamento: Thiago Goncalves Cavalcante (pendente) | /implement |
| ACH-04 | relevante | segurança | `backend/src/main/java/br/com/idsd/kanban/config/SegurancaConfig.java:125`, `:96` | `/actuator/health/**` é anônimo, ilimitado e toca o banco; o limite não conta requisição sem sujeito, e a borda que o javadoc pressupõe não existe. Rebaixado a relevante porque é custo anônimo e não vazamento — `show-details: never` impede que o probe conte qualquer coisa além do estado —, e a rota é pré-condição do healthcheck do compose; aprovador do rebaixamento: Thiago Goncalves Cavalcante (pendente) | /implement |
| ACH-05 | relevante | spec | `docs/prd/kanban-tarefas-prd.md:1220`, `docs/techspec/kanban-tarefas-techspec.md:470` | A dimensão de origem que a task manda implementar contradiz a condição de medição de RNF-010, que exige não contaminação entre sujeitos | /prd |
| ACH-06 | relevante | código | `backend/src/main/java/br/com/idsd/kanban/shared/TratadorDeErro.java:203` | O `ResponseBodyAdvice`, que é a decisão central da task, não é exercitado por teste nenhum | /implement |
| ACH-07 | relevante | código | `backend/src/test/java/br/com/idsd/kanban/alem/ErrosELimitesIT.java:50` | Dos oito códigos da tabela da task, três têm teste verde com `traceId`; `400`, `409` e `422` não têm verificação verde alguma, e `403` e `503` não aferem formato nem `traceId` | /implement |
| ACH-08 | menor | código | `backend/src/test/java/br/com/idsd/kanban/alem/ErrosELimitesIT.java:132` | O teste do caminho de produção — cliente sem cabeçalho — só afirma que o cabeçalho não é nulo, e não compara com o `traceId` do corpo | /implement |
| ACH-09 | menor | código | `backend/src/test/java/br/com/idsd/kanban/alem/ErrosELimitesIT.java:151` | `header().exists("Retry-After")` nunca afere o valor; com o relógio parado ele é determinístico e a asserção custaria nada | /implement |
| ACH-10 | menor | código | `backend/src/main/java/br/com/idsd/kanban/shared/TratadorDeErro.java:98` | Erro rejeitado pelo contêiner antes do despachante sai pelo tratamento padrão do Boot, sem `traceId` e fora de problem+json | /implement |
| ACH-11 | menor | spec | `backend/src/main/java/br/com/idsd/kanban/shared/LimiteDeRequisicoes.java:72` | A contagem é por instância e o desenho prevê três; o envelope efetivo de RNF-010 é multiplicado pelo número de réplicas, e isso não está declarado em lugar nenhum | /techspec |
| ACH-12 | menor | código | `backend/src/main/java/br/com/idsd/kanban/shared/ProblemaDetalhado.java:63` | `URI.create` sobre caminho vindo do cliente pode lançar de dentro do tratador de último recurso | /implement |

**Severidade:**

- **bloqueante** — impede o merge. Correção obrigatória antes do gate.
- **relevante** — merge permitido com registro; entra no backlog com prazo.
- **menor** — registrado, sem prazo.

### ACH-01 — a dimensão de origem repousa em premissa falsa

É o achado que mais importa, e ele não é sobre um detalhe do contador: é sobre a
justificativa inteira da segunda dimensão. O arquivo afirma, em três lugares, que
o sistema fica "atrás do proxy reverso, que é a única entrada do sistema, na
topologia do compose", e é dessa afirmação que saem as duas decisões de desenho —
confiar em `X-Forwarded-For` e dar à origem um envelope dez vezes o do sujeito.

Verificado em disco, a afirmação é falsa. `docker/compose.yaml` publica o backend
direto na porta 8080 e não declara proxy algum; o único nginx do repositório é
`docker/proxy/nginx.conf`, montado exclusivamente por `compose.test.yaml` para dar
endereço único às três réplicas do arnês de broadcast de RNF-002. Não há
`ForwardedHeaderFilter`, não há lista de saltos confiáveis e não há nada que
escreva o cabeçalho antes de a requisição chegar.

Disso decorrem três defeitos simultâneos, e o desconfortável é que eles se
alimentam. **A dimensão não protege**: qualquer cliente autenticado troca o
cabeçalho a cada requisição e a origem deixa de contar coisa alguma — a proteção
é nula, não fraca. **A dimensão fere terceiros**: o mesmo cliente escolhe o
endereço de outra origem e queima o envelope dela, recusando todo mundo que a
compartilha — que é literalmente o escritório inteiro que o javadoc diz querer
proteger, agora derrubado de propósito em vez de por acidente. E **a dimensão
consome memória**: cada valor forjado cria uma chave do minuto corrente, e
`podarSeNecessario` só remove chaves de minutos **anteriores**, de modo que dentro
do minuto nada é removido; passadas as 20.000 chaves, o `removeIf` passa a varrer
o mapa inteiro em toda requisição, inclusive nas recusadas, porque a inserção
acontece antes de a recusa ser decidida. O comentário do próprio arquivo diz que a
poda existe porque "memória que só cresce transforma a proteção contra abuso na
própria forma de derrubar a instância" — a intenção está certa e o mecanismo não a
cumpre.

É bloqueante por ser de segurança e por ser resolvível de dentro: o arquivo é
declarado da task, e as três saídas conhecidas — usar `getRemoteAddr()`, exigir
proxy confiável configurado, ou compor a chave de origem com o sujeito para que a
forja só prejudique quem forjou — cabem nele. O que **não** cabe é decidir se a
dimensão de origem deve existir; isso é ACH-05.

### ACH-02 — o critério que dá sentido à task não tem verificação

O critério de aceite 6 pede "comparação entre `traceId` da resposta e a linha de
log", está marcado como cumprido, e nenhum teste da suíte o exercita: a varredura
por `OutputCapture` em `backend/src/test` devolve uma única ocorrência, em
`AdminGlobalIT`, e ela é da promoção do admin global. O que os oito testes novos
verificam é `traceId` não vazio no corpo e o eco do cabeçalho — nada toca o log.

O que torna isto mais que um buraco de cobertura é o segundo achado, que só
aparece quando se procura o mecanismo: **não existe padrão de log com
`%X{traceId}`** em `src/main/resources`. A correlação vive apenas nas linhas que
alguém lembrou de concatenar `traceId={}` à mão. Toda linha de framework — e o
stacktrace que acompanha o `500`, que é justamente o erro em que a correlação mais
vale — sai sem ela. Mesmo que o critério 6 fosse verificado, ele valeria só para
as chamadas instrumentadas uma a uma, que é a forma exata do defeito que a decisão
do `ResponseBodyAdvice` existe para evitar do lado da resposta.

Há ainda um caminho em que o par diverge em silêncio. `ProblemaDetalhado.traceId()`
devolve um UUID **novo a cada invocação** quando o MDC está vazio, e o tratador
chama o método uma vez para o log e outra, indiretamente, para o corpo — os dois
identificadores saem diferentes e ambos parecem válidos. O MDC está vazio no
despacho de erro do contêiner: `FiltroDeCorrelacao` estende `OncePerRequestFilter`,
cujo `shouldNotFilterErrorDispatch()` é verdadeiro por padrão, o registro não
declara `ERROR` entre os tipos de despacho, e o `finally` já removeu a chave.

É bloqueante pela mesma razão de ACH-01 da revisão de TASK-01.5: o mecanismo
central da task entra sem prova executável, e o critério está declarado cumprido
por inspeção. É resolvível de dentro — o arquivo de teste é da task, e o padrão de
log é uma linha de configuração.

### ACH-05 — a origem contradiz a condição de medição de RNF-010

Este não é defeito de quem implementou: a task manda, com todas as letras,
"aplicar o limite também por origem". O problema é que o requisito congelado pede
o contrário. A condição de medição de RNF-010 no PRD exige submeter rajada e
verificar "que o consumo de um sujeito não afeta a resposta de outro", e a matriz
da TechSpec repete: "a não contaminação entre sujeitos".

O teste `origemTemEnvelopeProprio` afirma a contaminação **como comportamento
correto**: ana consome três leituras, bruno consome três, nenhum dos dois estoura
o próprio envelope, e bruno recebe `429`. É a condição de medição de RNF-010
falhando, escrita como asserção verde.

A leitura de que isso é inofensivo depende de a origem discriminar bem, e ACH-01
mostra que ela não discrimina. Sem proxy que escreva o cabeçalho, o recuo é
`getRemoteAddr()`, e o dia em que um proxy for posto na frente sem confiança
configurada, **toda** requisição terá o mesmo endereço e o envelope de origem
vira um teto global de 1200/300 para a base inteira — momento em que RNF-010 se
inverte: o uso anômalo de uma conta passa a degradar o sistema para as demais,
que é exatamente o que o requisito existe para impedir.

Precisa de decisão, e ela não é de quem implementa: ou RNF-010 é emendado para
admitir a segunda dimensão e a condição de medição é reescrita, ou a dimensão de
origem sai. Dono `/prd`.

---

## Análise de segurança

| Verificação | Resultado | Evidência |
| --- | --- | --- |
| Entrada validada nas fronteiras | ok | `FiltroDeCorrelacao:36` — o cabeçalho de correlação é conferido por `[A-Za-z0-9._-]{1,64}` com casamento total, o que elimina CRLF, caractere de controle e comprimento livre; injeção em log fechada, e o eco no cabeçalho não permite *header splitting* |
| Autorização verificada por operação | achado | ACH-03 e ACH-04. A regra final é `anyRequest().authenticated()` e rota não mapeada nasce fechada — negação por padrão está correta e medida. O que há é o `403` que sairá `500` com segurança de método, e o probe anônimo sem limite |
| Segredo fora do código e do log | ok | Nenhum segredo em `application-test.yml`, em linha de log ou em corpo de erro; a senha do banco continua vindo do configtree |
| Dado sensível fora de log e mensagem de erro | ok | O `detail` de `404`, `403` e `500` é constante e genérico; a exceção vai inteira para o log e nada dela para o corpo. `ErrosELimitesIT:93` afere ausência de `trace` e `exception`, e SCN-002.3 continua verde |
| Dependência nova sem vulnerabilidade conhecida | n/a | Nenhuma dependência acrescentada; `pom.xml` não foi tocado nesta task |

Fora da tabela, três verificações que valem registro porque poderiam ter falhado e
não falharam. A captura de `AuthenticationServiceException` continua estreita:
`InvalidBearerTokenException` segue saindo em `401`, e credencial ruim não virou
`503`. As chaves do limitador usam prefixos `s|` e `o|` disjuntos, então um
`X-Forwarded-For` contendo `|` não colide com chave de sujeito. E `csrf().disable()`
vem acompanhado de `STATELESS`, que é o que o torna correto em vez de descuidado.

O achado de segurança bloqueante é ACH-01. ACH-03 e ACH-04 são relevantes e não
bloqueantes: o primeiro é armadilha latente que falha fechada — vira `500`, não
`200` —, e o segundo é custo anônimo, não vazamento.

---

## Guardrails extraídos

Regras que esta revisão descobriu e que devem valer para as próximas.

| Guardrail | Origem | Onde passa a valer |
| --- | --- | --- |
| Premissa de topologia citada em javadoc para justificar decisão de segurança é conferida contra o arquivo de composição, e o arquivo é nomeado na própria nota | ACH-01 | `requirements/guidelines/backend/java/coding-standards.md` |
| Cabeçalho de encaminhamento só é lido como origem se houver proxy confiável declarado na configuração; sem ele, a origem é `getRemoteAddr()` | ACH-01 | `requirements/guidelines/backend/java/api-security.md` |
| Estrutura de contagem alimentada por valor que o cliente escolhe tem teto duro de cardinalidade, e não apenas poda por expiração | ACH-01 | `requirements/guidelines/backend/java/coding-standards.md` |
| Correlação de requisição entra pelo padrão de log (`%X`), nunca por concatenação em cada chamada; task que institui `traceId` entrega as duas pontas | ACH-02 | `requirements/guidelines/backend/java/observability.md` |
| Critério de aceite que pede comparação entre resposta e log exige captura de log no teste; inspeção não fecha critério | ACH-02 | `requirements/guidelines/backend/java/definition-of-done.md` |
| `@ExceptionHandler(Exception.class)` em advice global declara explicitamente o reencaminhamento de `AccessDeniedException` e `AuthenticationException` | ACH-03 | `requirements/guidelines/backend/java/api-security.md` |
| Endpoint público que consulta dependência de infraestrutura fica em porta de gerenciamento separada, ou entra na contagem do limite | ACH-04 | `requirements/guidelines/infra/docker/architecture.md` |
| Valor de envelope de RNF tem teste que afirma o padrão de produção, e não apenas o mecanismo em valor apertado | ACH-07 | `requirements/guidelines/backend/java/testing.md` |

Os arquivos `api-security.md` e `observability.md` da coleção `backend/java` não
existem hoje. Criá-los é decisão de `/guidelines`, não de `/code-review` — mesmo
tratamento que a onda 4 da revisão de TASK-01.5 deu aos dois transversais de
`_shared`, e fica como dívida nomeada com esse dono. Os guardrails que têm arquivo
existente são os de `coding-standards.md`, `testing.md`, `definition-of-done.md` e
`infra/docker/architecture.md`.

---

## Encaminhamento ao dono — 2026-09-11

Os doze achados foram encaminhados e **todos estão fechados**. O que decidiu a
forma de tudo foi ACH-05, que é de spec e precedia os de código: o demandante
escolheu **remover a dimensão de origem** em vez de emendar RNF-010. Com isso
ACH-01 fechou junto e sem correção própria — não há mais cabeçalho forjável, não
há envelope de terceiro a queimar, e a cardinalidade do mapa passa a ser limitada
pelos sujeitos verificados. Nenhuma emenda de PRD foi necessária.

| Achado | Dono | Desfecho |
| --- | --- | --- |
| ACH-01 | /implement | fechado com ACH-05 — dimensão de origem removida de `LimiteDeRequisicoes` |
| ACH-02 | /implement | fechado nas duas metades: padrão de log com `%X{traceId}` em `application.yml` e `traceIdDaRespostaCasaComOLog` como verificação do critério 6 |
| ACH-03 | /implement | fechado — `naoPrevisto` relança `AccessDeniedException` e `AuthenticationException` |
| ACH-04 | /implement | fechado — probes liberados por caminho exato, sem `/**` |
| ACH-05 | /prd | fechado **sem emenda** — decisão do demandante de remover a origem restitui a condição de medição |
| ACH-06 | /implement | fechado — `corpoDeControladorERecebeTraceId` exercita o `ResponseBodyAdvice` |
| ACH-07 | /implement | fechado — `metodoNaoSuportadoSaiEmProblemJson` cobre o caminho do MVC padrão |
| ACH-08 | /implement | fechado — a asserção compara cabeçalho e corpo |
| ACH-09 | /implement | fechado — `Retry-After` aferido pelo valor |
| ACH-10 | /implement | fechado — `DispatcherType.ERROR`/`ASYNC` no registro e os dois `shouldNotFilter*Dispatch()` sobrescritos |
| ACH-11 | /techspec | fechado — contagem por instância declarada no contrato e na Seção 8; TechSpec v1.10 |
| ACH-12 | /implement | fechado — `URI.create` protegido, com recuo para membro adicional |

Medido após a correção: **151 testes, 50 verdes / 101 vermelhos**, contra a linha
de base de 147 (46/101) da própria task. Cinco testes novos, todos verdes; os 101
vermelhos são os mesmos, de rotas de tasks posteriores — nenhuma regressão.
`ErrosELimitesIT` isolado em **12/12**.

---

## Veredicto

- **GATE-REVISAO-TECNICA:** reprovado
- **GATE-NFR:** reprovado
- **Bloqueantes em aberto:** 0 (eram 2)
- **Revisor humano:** Thiago Goncalves Cavalcante — pendente

> Bloqueante em aberto reprova, sem exceção e sem waiver: a policy proíbe waiver
> para verificação independente e para dados.

O GATE-REVISAO-TECNICA reprova por ACH-01 e ACH-02, ambos resolvíveis de dentro
da task e nenhum dependente de task posterior. O GATE-NFR reprova por duas razões
independentes: RNF-010 sai **fora** do envelope, e não apenas não medido, porque a
condição de medição que o PRD escreveu é contrariada pela dimensão de origem
(ACH-05); e os demais RNFs continuam sem instrumentação, pela mesma razão das
quatro revisões anteriores — nenhum é mensurável antes do fechamento do EPIC-01.
Fechado o encaminhamento, a primeira razão caiu e o GATE-NFR segue reprovado só
pela segunda, que é de fechamento de épico e não desta task.

Esta é revisão parcial de task e não fecha épico. O que a task entregou está
correto em todo caminho conferido — negação por padrão medida, formato único
funcionando, aritmética do contador sem erro de contorno, nenhum vazamento de dado
do recurso negado, `traceId` central em vez de obrigação de cada rota — e os dois
bloqueantes estão nas bordas: um numa premissa de topologia que o compose
desmente, outro num critério declarado cumprido sem verificação.

---

## Fora deste artefato — regras negativas

- **Correção de código** — pertence ao `/implement`. O revisor descreve o
  achado e o devolve; não conserta.
- **Requisito novo** — achado de spec vira devolução ao `/prd`, não requisito
  escrito aqui.
- **Decisão de arquitetura** — pertence ao `/techspec`.
- **Cenário Gherkin** — congelado; alteração exige emenda no PRD.
- **Reescrita do plano de tasks** — pertence ao `/tasks`.
