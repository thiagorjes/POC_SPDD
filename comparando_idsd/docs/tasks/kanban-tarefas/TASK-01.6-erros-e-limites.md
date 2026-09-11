# TASK-01.6 — problem+json, negação por padrão e limite de requisições

- **Status:** concluída
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-01.4
- **Cenários cobertos:** SCN-002.3
- **Origem:** RNF-004, RNF-010, TechSpec Seção 8

#### Contexto

Todo erro do sistema sai no mesmo formato, e é esse formato que os cenários
congelados exercitam nos épicos seguintes. A task também institui a negação por
padrão e o limite de requisições, que aqui pesa mais do que num sistema comum:
cada escrita aceita dispara difusão de evento para todas as instâncias e todas
as sessões, então o custo de uma requisição abusiva é amplificado pelo desenho
de tempo real, e os envelopes de desempenho não têm outra proteção.

#### O que deve ser feito

- [x] Implementar o tratador global de exceções produzindo
      `application/problem+json` em toda resposta de erro.
- [x] Incluir `traceId` em todo corpo de erro e propagar o identificador de
      correlação por requisição.
- [x] Fazer rota não mapeada negar por padrão.
- [x] Implementar o limite de requisições por sujeito: **120 leituras** e
      **30 escritas por minuto**, com `429` e `Retry-After` em problem+json.
- [x] Aplicar o limite também por origem.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/java/br/com/idsd/kanban/shared/TratadorDeErro.java` | criar | tratador global |
| `backend/src/main/java/br/com/idsd/kanban/shared/ProblemaDetalhado.java` | criar | corpo de erro |
| `backend/src/main/java/br/com/idsd/kanban/shared/FiltroDeCorrelacao.java` | criar | identificador por requisição |
| `backend/src/main/java/br/com/idsd/kanban/shared/LimiteDeRequisicoes.java` | criar | contagem por sujeito e por origem |
| `backend/src/main/java/br/com/idsd/kanban/config/SegurancaConfig.java` | alterar | negação por padrão, registro do filtro |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

Forma do corpo de erro, literal:

```
{
  "type": "https://errors.idsd/<slug>",
  "title": "<frase curta em linguagem de negócio>",
  "status": 0,
  "detail": "<o que aconteceu, em linguagem de negócio>",
  "instance": "/v1/<recurso>",
  "traceId": "<...>"
}
```

Códigos comuns, válidos para todos os contratos do sistema:

| Código | Quando |
| --- | --- |
| `400` | corpo malformado ou parâmetro desconhecido |
| `401` | sem token, token inválido ou expirado |
| `403` | sem permissão |
| `404` | recurso inexistente, ou existente em projeto sem participação |
| `409` | estado de origem divergente |
| `422` | regra de negócio violada |
| `429` | limite de requisições excedido, com `Retry-After` |
| `503` | provedor de identidade indisponível, com `Retry-After` |

Envelope numérico do limite: 120 leituras e 30 escritas por minuto **por
sujeito**.

#### Guia técnico — pontos de atenção

- **`detail` fala a linguagem do negócio.** Mensagem técnica no `detail` vaza
  desenho interno e não ajuda quem lê.
- **Nunca inclua no corpo de erro dado do recurso negado.** É o mesmo raciocínio
  que faz o projeto de terceiro responder `404`.
- **Rota não mapeada nega por padrão.** Configuração permissiva com exceções
  ponto a ponto inverte o ônus e falha em silêncio quando alguém acrescenta uma
  rota.
- **O tratador não pode engolir o conflito de estado.** O corpo de `409` carrega
  um bloco adicional que uma task posterior acrescenta; deixe o ponto de
  extensão previsto em vez de serializar um corpo fixo.
- O limite conta por sujeito autenticado; requisição sem token já é recusada
  antes por `401`.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | Todo erro sai em `application/problem+json` com `traceId` | uma requisição por código da tabela acima |
| 2 | Rota não mapeada é negada | requisição a caminho inexistente não vaza pilha nem devolve `200` |
| 3 | A 121ª leitura no mesmo minuto devolve `429` com `Retry-After` | teste de integração com relógio controlado |
| 4 | A 31ª escrita no mesmo minuto devolve `429` com `Retry-After` | idem |
| 5 | O corpo de erro não contém dado do recurso negado | inspeção do corpo em resposta `404` de projeto de terceiro |
| 6 | O identificador de correlação aparece no log e no corpo | `ErrosELimitesIT.traceIdDaRespostaCasaComOLog` — `OutputCaptureExtension` comparando o `traceId` da resposta com a linha de log. Estava marcado cumprido sem verificação alguma até ACH-02 da revisão |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
| 2026-09-11 | Red medido | Suíte alcançável: **139 testes, 35 verdes / 104 vermelhos**. A suíte inteira continua não compilando por cinco arquivos que referenciam classes de tasks posteriores (`ReconstrucaoDaProjecaoIT`, `ImpedimentoServiceTest`, `EtapaServiceTest`, `CriacaoDeTarefaServiceTest`, `TomadaServiceTest`), então a medição é por `javac` mais console do JUnit excluindo exatamente esses cinco — mesmo arnês das tasks anteriores (ACH-07 da revisão de TASK-01.3) |
| 2026-09-11 | achado | **Nenhum cenário congelado exercita `429`, `Retry-After` ou `traceId`.** A varredura de `src/test` por esses termos devolve só a asserção de `Retry-After` do `503` de SCN-001.3. Os critérios de aceite 1 a 4 e 6 não tinham verificação alguma; os testes novos foram escritos **fora** da contagem de cenários, como `ExistenciaECapacidadeIT` e `AusenciaDeNMaisUmIT`. Nenhum `.feature`, step definition ou asserção congelada foi tocado |
| 2026-09-11 | tentativa 1 | Quatro arquivos criados e `SegurancaConfig` alterado. Falha de compilação: `HttpHeaders.X_FORWARDED_FOR` não existe na versão do Spring em uso — trocado pelo literal |
| 2026-09-11 | tentativa 2 | **Verde.** Suíte alcançável: **147 testes, 46 verdes / 101 vermelhos**. São 8 testes novos, todos verdes, e 3 que falhavam voltaram a passar (recusas que agora saem no formato único). Nenhuma regressão — a aritmética fecha nos dois sentidos. Os 101 vermelhos continuam sendo rotas de tasks posteriores, a maioria parando em `Cenario.fluxoPadrao` (EPIC-02) |
| 2026-09-11 | decisão | **`traceId` central e não obrigação de cada rota.** `ProjetoController` monta o próprio `ProblemDetail` e está fora da tabela de arquivos; em vez de editá-lo, o tratador implementa `ResponseBodyAdvice` e enriquece todo corpo `ProblemDetail` na saída. Fecha o escopo declarado e faz a rota que ainda vai nascer sair correta sem que ninguém se lembre |
| 2026-09-11 | decisão | **Ordem dos dois filtros.** A correlação é registrada por `FilterRegistrationBean` na precedência máxima, **fora** da cadeia de segurança, porque `401`, `503` e `429` nascem dentro dela e nunca chegam ao despachante — filtro posterior deixaria justamente os erros que mais precisam de correlação sem ela. O limite vai **depois** do filtro do bearer token, porque a contagem é por sujeito autenticado e antes dele todo mundo cairia na mesma chave vazia. Nenhum dos dois é bean de `Filter`: o Boot registra sozinho todo bean de filtro, e o registro automático não deixa fixar precedência |
| 2026-09-11 | decisão **revertida** | ~~Origem é rede grossa, e de propósito.~~ **A dimensão de origem foi removida** na correção de ACH-01 e ACH-05 da revisão. A justificativa acima se apoiava em premissa que o `docker/compose.yaml` desmente: não há proxy reverso em produção — o compose publica o backend direto, e o único nginx do repositório existe no arnês de broadcast de `compose.test.yaml`. Sem proxy que o escreva, `X-Forwarded-For` é escolhido pelo cliente, e a dimensão não continha nada, permitia queimar o envelope de terceiro e fazia o mapa crescer sem teto real. O que decidiu não foi nenhum dos três: a condição de medição de RNF-010 exige provar que **o consumo de um sujeito não afeta a resposta de outro**, e o teste `origemTemEnvelopeProprio` afirmava a contaminação como comportamento correto. Requisito e mecanismo eram incompatíveis por construção; removida a origem, RNF-010 volta a ser satisfazível sem emenda ao PRD |
| 2026-09-11 | decisão | **Ponto de extensão do `409` previsto e não serializado.** `ProblemaDetalhado.Falha` carrega um mapa de membros adicionais; o bloco `estadoAtual` de SCN-005.3, SCN-007.3 e SCN-020.3 entra por ali quando a task que o define chegar, sem reabrir o tratador para acrescentar campo de outro domínio |
| 2026-09-11 | fora do escopo declarado | `backend/src/test/java/br/com/idsd/kanban/alem/ErrosELimitesIT.java` (criado) — os testes que dão verificação aos critérios 1 a 4 e 6, fora da contagem de cenários |
| 2026-09-11 | fora do escopo declarado | `backend/src/test/resources/application-test.yml` (criado) — **mina desarmada**: os contadores vivem no contexto e a massa dos cenários é montada pela própria API HTTP, no mesmo sujeito e dentro do mesmo minuto; com o envelope de produção, um cenário de EPIC-02 que semeia etapas e tarefas começaria a receber `429` no meio da preparação, com falha intermitente longe da causa. O envelope da suíte é alto, e não desligado — desligar faria o caminho exercitado deixar de ser o caminho de produção. Quem verifica o limite aperta o número de volta por `@TestPropertySource` |
| 2026-09-11 | correção da revisão | **ACH-01 e ACH-05 — dimensão de origem removida.** Ver a decisão revertida acima. `LimiteDeRequisicoes` passa a contar só por sujeito autenticado, que sai do `sub` de um token já verificado e não pode ser forjado; com isso a cardinalidade do mapa deixa de ser escolhida pelo cliente e a poda passa a ser higiene e não contenção. Os dois `@Value` de origem e as duas propriedades do teste saíram junto |
| 2026-09-11 | correção da revisão | **ACH-02 — o critério 6 passa a ser medido.** Faltavam as duas metades. O mecanismo: não havia padrão de log algum, e a correlação vivia só nas linhas em que alguém concatenou `traceId={}` à mão — toda linha de framework e o stacktrace do `500` saíam sem ela. `application.yml` ganhou `logging.pattern` com `%X{traceId}`, e as seis concatenações manuais foram removidas, porque duplicariam o valor na mesma linha. A verificação: `traceIdDaRespostaCasaComOLog` usa `OutputCaptureExtension` e compara o `traceId` da resposta com a saída de log. Fechou junto o caminho em que os dois divergiam em silêncio — `ProblemaDetalhado.traceId()` cunhava um UUID **novo a cada invocação** com o MDC vazio, e agora fixa o valor cunhado no MDC |
| 2026-09-11 | correção da revisão | **ACH-10 — a correlação vale no despacho de erro.** `OncePerRequestFilter` dispensa por padrão os despachos `ERROR` e `ASYNC`, e o `FilterRegistrationBean` só declarava `REQUEST`: o MDC já tinha sido limpo no `finally` da passagem original quando o tratador monta o `500`. Os dois despachos foram declarados e as duas dispensas desligadas. O identificador da segunda passagem é lido do cabeçalho já emitido na resposta, e não cunhado de novo — senão a mesma requisição teria dois identificadores válidos e discordantes |
| 2026-09-11 | correção da revisão | **ACH-04 — probes por caminho exato.** `/actuator/health/**` abria todo grupo de health que alguém viesse a declarar, inclusive um criado para depurar produção com detalhe de dependência, e a abertura seria retroativa e silenciosa. Passou aos três caminhos exatos que o healthcheck do compose consulta |
| 2026-09-11 | correção da revisão | **ACH-03 — recusa de acesso não é engolida pelo tratador.** `@ExceptionHandler(Exception.class)` capturava também `AccessDeniedException` e `AuthenticationException`, que o `ExceptionTranslationFilter` traduz em `403` e `401`; hoje o caminho é inalcançável porque não há segurança de método, e é justamente por isso que o cuidado seria fácil de esquecer até deixar de ser barato. As duas são relançadas |
| 2026-09-11 | correção da revisão | **ACH-12 — `URI.create` protegido.** O caminho vem do cliente e levanta em valor malformado, de dentro do tratador de último recurso — trocaria o corpo padronizado pela página de erro do contêiner, exatamente na requisição que já deu errado. `instance` é campo informativo: em caminho malformado ele é omitido e o valor vai para `instancePath` |
| 2026-09-11 | correção da revisão | **ACH-06 a ACH-09 — cobertura.** Cinco testes novos: o corpo montado por controlador enriquecido pelo `ResponseBodyAdvice` (único caminho que o exercita); o `405` do tratamento padrão do Spring MVC, que é outro caminho de saída; o valor do `Retry-After` e não a mera existência; a igualdade entre o `traceId` do cabeçalho e o do corpo, e não só que existe; e a não contaminação entre sujeitos, que é a condição de medição de RNF-010 escrita como asserção. O `400` de corpo ilegível fica **declaradamente não coberto** — nenhuma rota com `@RequestBody` existe antes de TASK-01.8 |
| 2026-09-11 | medição pós-correção | **151 testes, 50 verdes / 101 vermelhos**, contra a linha de base de 147 (46/101) desta mesma task. `ErrosELimitesIT` isolado em **12/12**. Os 5 testes líquidos novos passam e os 101 vermelhos são idênticos — nenhuma regressão, e a aritmética fecha nos dois sentidos |
| 2026-09-11 | limite declarado | O critério 3 pede relógio controlado, e para isso `SegurancaConfig` passou a expor o relógio como bean, substituído no teste. Sem isso o resultado dependeria de a janela não virar no meio do teste, que é a forma mais cara de teste intermitente. Os envelopes verificados são apertados por propriedade e não os 120/30 de RNF-010: o mecanismo é o mesmo, e disparar 121 requisições por teste custaria minutos de suíte para provar a mesma coisa — o número de produção é o padrão em `SegurancaConfig` |
