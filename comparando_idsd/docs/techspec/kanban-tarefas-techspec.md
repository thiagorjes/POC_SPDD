# TechSpec — kanban-tarefas
_Versão: 1.11 | Status: Draft | Data: 2026-09-14 | Autor: agente `/techspec`_
_PRD: docs/prd/kanban-tarefas-prd.md v1.5 — cenários congelados desde a reconfirmação da emenda v1.5_

---

## 1. Visão Geral Técnica

Sistema web de acompanhamento de tarefas em board por projeto, com fluxo de etapas
configurável, handoff explícito entre etapas e apuração de tempo. O que distingue
tecnicamente esta feature de um CRUD de board são três exigências que se
condicionam: o histórico é imutável (RNF-008), as três séries de tempo nunca são
somadas nem agregadas por pessoa (RN-008, RN-014), e o estado se propaga a todas
as sessões abertas em p95 ≤ 2 s (RNF-001) com múltiplas instâncias (RNF-002).

**Sistemas afetados:** apenas o IDSD. Nenhum sistema do workspace é alterado; o
provedor de identidade corporativo é consumido, não modificado.

**Abordagem:** backend Java/Spring Boot expondo REST `/v1` e um canal
WebSocket/STOMP, sobre PostgreSQL único (ADR-002); frontend Next.js consumindo
ambos; tudo em contêiner (ADR-008). Log de eventos append-only como fonte de
verdade, com projeções derivadas para leitura (SDR-001). Escrita sempre com
estado de origem declarado e bloqueio otimista (SDR-002).

**Limitação de guidelines.** As quatro coleções de `guidelines.yaml` governam este
sistema, com duas ressalvas registradas:

- `infra/docker` **deixou de ser stub em 2026-09-09** e agora governa este
  sistema com norma própria: topologia, rede, volumes, ordem de subida,
  healthcheck, segredo, proveniência de imagem e ambiente de teste. As decisões
  que esta TechSpec vinha tomando localmente — socket do host para a suíte,
  probes separados, healthcheck do provedor de identidade, segredo por arquivo
  montado — subiram para a coleção como ADR-011, ADR-012 e ADR-013, e aqui
  passam a ser **referenciadas, não decididas**. Era a pendência 02 do
  `state.md`, fechada. Restam duas dívidas nomeadas na própria coleção e não
  bloqueantes: registry/tag de release e CI.
- `backend/java` é divergida em três eixos por **ADR-009** — Java 25, PostgreSQL e
  Docker on-premise no lugar de Java 21, Oracle e OpenShift. Em todo o resto ela
  governa sem exceção.

---

## 2. Decisões Arquiteturais

> Decisões: ADR-001 a ADR-013, BDR-001, BDR-002, SDR-001 a SDR-004.
> As sete criadas nesta etapa estão em negrito. ADR-004 é superado em um ponto por
> SDR-004, e ADR-007 é superado por ADR-010.
> ADR-011, ADR-012 e ADR-013 **não são desta etapa**: nasceram no `/guidelines` ao
> elaborar `infra/docker`, e entram aqui porque governam esta feature. É a direção
> correta — norma que vale além da feature pertence à biblioteca, e mantê-la aqui a
> esconderia dos outros sistemas.

| DR | Decisão | Impacto |
|-----|---------|---------|
| **ADR-009** | Desvio declarado da coleção `backend/java` em linguagem, banco e plataforma | Preserva ADR-004 e o desenho de RNF-001/002; obriga SDR-003 |
| **SDR-001** | Eventos imutáveis como verdade, projeções de intervalo para leitura | Satisfaz RNF-008 literalmente e RNF-009 sem cache; torna RN-014 estrutural |
| **SDR-002** | Concorrência e idempotência por estado de origem declarado | Um mecanismo para RN-012, RN-013 e RN-031; `409` devolve o estado corrente |
| **SDR-003** | Testcontainers no lugar de H2; Playwright para os cenários `e2e` | Testa o mecanismo real de `LISTEN/NOTIFY`; cobre RNF-006 no mesmo percurso |
| **BDR-002** | Desbloqueio como permissão de `product_owner` e `project_admin` | Nenhuma entidade nova de autorização; o fallback de RN-024 fica raro, mas alcançável |
| **SDR-004** | `seq` gerado no banco na transação de escrita; publicador único do `NOTIFY` | Supera ADR-004 no contador por pod; serializa escrita por projeto; fecha a janela de perda do `afterCommit` |
| **ADR-010** | Bootstrap do admin global por `sub` verificado, promoção única e auditada | Supera ADR-007; fecha a escalada por claim `email` e especifica o que o bypass contorna |
| ADR-001 | Java + Spring Boot, JPA, WebSocket/STOMP, OIDC | Base de toda a Seção 5 |
| ADR-002 | PostgreSQL único, sem cache nem broker nesta fase | Fecha a saída fácil de RNF-009 e força a projeção de SDR-001 |
| ADR-003 / ADR-006 | Keycloak autentica, permissões na aplicação; sem fallback local | RF-001, RF-002 e toda a Seção 8 |
| ADR-004 | Broadcast multi-instância por `LISTEN/NOTIFY` | RF-020, RNF-001, RNF-002 |
| ADR-005 | Flyway | Seção 8 do `data-model.md` |
| ADR-007 | Bootstrap do primeiro admin por flag e e-mail configurado | Entidade `usuario` |
| ADR-008 | Dockerização de backend e frontend | RNF-003 e o `quickstart.md` |
| ADR-011 | Migração de schema em serviço dedicado, fora do boot | Fecha a disputa de lock que o teste de RNF-002 provoca com 3 instâncias; muda a ordem de subida do `compose` |
| ADR-012 | Suíte acessa o daemon do host por socket montado; DinD descartado | Habilita Testcontainers dentro de contêiner (SDR-003), com o privilégio assumido e escopado |
| ADR-013 | Proveniência de imagem por digest, sem registry | Fixa a imagem base do build; `POSTGRES_IMAGE` única para `compose` e suíte |
| BDR-001 | RBAC por projeto, papéis acumuláveis, catálogo fechado | RF-019 e a checagem de toda escrita |

O raciocínio completo de cada decisão nova está em
[`kanban-tarefas-research.md`](kanban-tarefas-research.md) e nos DRs em
`docs/decisions/`.

---

## 3. Modelo de Dados

→ Documento completo: [data-model.md](kanban-tarefas/data-model.md)

Três anéis com regras de escrita diferentes: **verdade** (só `INSERT`),
**projeção** (derivada e reconstruível) e **configuração** (mutável, valendo dali
em diante por RN-022).

**Entidades principais:**

| Entidade | Atributos-chave | Relacionamentos |
|----------|----------------|----------------|
| `usuario` | `subject_id` do provedor, `admin_global` | participa de projetos |
| `projeto` | `nome` | tem etapas, raias, participações, tarefas |
| `etapa` | `ordem`, `terminal`, `arquivada_em` | do projeto; identidade estável (RN-021) |
| `raia` | `ordem` | do projeto; referenciada por `tarefa` (estado corrente) e por nenhuma série de tempo — é essa ausência estreita que realiza RN-023 |
| `participacao` / `participacao_papel` | par único usuário↔projeto, papéis acumuláveis | BDR-001 |
| `evento_tarefa` | `tipo`, `episodio`, `seq`, `dados` | append-only; fonte de verdade |
| `tarefa` | `etapa_id` + `condicao` (dimensões 1 e 2 de RN-002), `versao` | projeção de estado corrente |
| `intervalo_tarefa` | `tipo`, `episodio`, `inicio`, `fim` | projeção das três séries |
| `impedimento` | `motivo`, `anotacoes`, `desfecho` | um aberto por tarefa (RN-010) |

Três ausências no esquema são decisões, não esquecimentos, e valem mais que
qualquer coluna presente:

- `intervalo_tarefa` **não tem coluna de pessoa**. RN-014 deixa de depender de
  quem escreve a consulta e passa a ser propriedade do esquema.
- Nenhuma tabela de projeção **tem coluna de total**. RN-008 proíbe somar as três
  séries entre si; uma coluna de soma seria o convite a fazê-lo.
- `tarefa` **não tem coluna de impedimento**, e `IMPEDIDA` saiu do domínio de
  `condicao`. A terceira dimensão de RN-002 é derivada de `impedimento` com
  `desfecho IS NULL`. Enquanto ela era valor da condição, toda movimentação a
  sobrescrevia em silêncio contra RN-009 — era o achado INC-01. Derivada, RN-032
  passa a ser garantida pelo esquema: nenhuma escrita sobre `tarefa` alcança a
  marca, e só o registro do desfecho a apaga.

---

## 4. Contratos de API / Interface

→ Documentos completos: [contracts/](kanban-tarefas/contracts/)

| Arquivo | Cobre | Tipo |
|---|---|---|
| [`sessao-e-projetos.md`](kanban-tarefas/contracts/sessao-e-projetos.md) | RF-001, RF-002, RF-017, RF-018, RF-019, RF-021 | REST |
| [`board-e-tarefas.md`](kanban-tarefas/contracts/board-e-tarefas.md) | RF-003 a RF-013 | REST |
| [`fila-e-consultas.md`](kanban-tarefas/contracts/fila-e-consultas.md) | RF-014, RF-015, RF-016 | REST |
| [`eventos-tempo-real.md`](kanban-tarefas/contracts/eventos-tempo-real.md) | RF-020 | Event (STOMP) |

Duas formas atravessam todos os contratos REST e por isso ficam registradas aqui:

**Envelope de escrita.** Toda rota de escrita sobre tarefa recebe
`origem: { etapaId, condicao, versao }` e devolve, em `409`, um bloco
`estadoAtual` com etapa, condição, responsável e versão correntes. Um `409` sem
esse bloco reprova SCN-005.3, SCN-007.3 e SCN-020.3, que exigem que quem perdeu a
corrida saiba o que aconteceu — não que houve um erro.

**Erros.** `application/problem+json` (RFC 7807) em toda falha, com `traceId`
correlacionado ao log. `400` formato, `422` regra de negócio, `403` permissão,
`404` para recurso cuja existência não se deve revelar (SCN-002.3), `409` estado
divergente, `503` com `Retry-After` para indisponibilidade do provedor de
identidade (SCN-001.3).

---

## 5. Arquitetura e Fluxo

```
┌───────────────┐        REST /v1        ┌────────────────────────────────┐
│  Next.js      │ ─────────────────────► │  Spring Boot                   │
│  App Router   │ ◄───────────────────── │                                │
│               │                        │  controller ─► service ─┬─► repository
│  React Query  │        STOMP /ws       │   (sem regra)  (regra)  │    (JPA)
│  + STOMP      │ ◄════════════════════► │                         │        │
└───────────────┘                        │       EventoBoardPublisher      │
                                         └────────────────┬───────┼────────┘
                                                          │       │
                                          afterCommit     │       │ INSERT + projeção
                                          NOTIFY          ▼       ▼
                                         ┌────────────────────────────────┐
                                         │  PostgreSQL                    │
                                         │  evento_tarefa (append-only)   │
                                         │  tarefa / intervalo / impedim. │
                                         │  canal board_events            │
                                         └────────────────────────────────┘
                                                          ▲
                                    LISTEN board_events   │  (uma conexão por instância)
                                         ┌────────────────┴───────────────┐
                                         │  demais instâncias Spring Boot │
                                         └────────────────────────────────┘

  Keycloak ──► JWT validado localmente por JWKS em toda requisição e no CONNECT
```

Camadas conforme `backend/java/architecture.md`: entidade JPA nunca sai do
service, controller sem regra, records para todo DTO, domínios sob
`internal/<dominio>` — `projeto`, `tarefa`, `tempo`, `acesso`.

**Fluxo principal — uma escrita sobre tarefa, ponta a ponta:**

1. O controller recebe a requisição com o bloco `origem` e valida formato com Bean
   Validation. Nenhum `if` de negócio aqui.
2. O service abre transação curta, carrega a tarefa sob bloqueio otimista e
   **reavalia a origem declarada contra o estado corrente** (SDR-002). Divergência
   encerra em `409` com o estado atual; efeito já aplicado encerra em `200` sem
   novo evento (RN-031).
3. Verifica a permissão sobre a participação no projeto — não sobre papel global,
   e nunca sobre dado vindo do cliente (BDR-001, RNF-004).
3b. Obtém o `seq` do evento incrementando o contador do projeto no banco, dentro
   da mesma transação (SDR-004). É o que serializa as escritas daquele projeto —
   contenção aceita conscientemente, porque sem árbitro compartilhado duas
   instâncias produziriam o mesmo número e a detecção de lacuna do cliente viraria
   ruído.
4. Aplica a regra de negócio, grava o `evento_tarefa` e atualiza as projeções
   `tarefa`, `intervalo_tarefa` e `impedimento` **na mesma transação**. Sem isso o
   board exibiria um estado e o agregado outro.
5. Registra em `TransactionSynchronization.afterCommit` a publicação do evento
   pela porta `EventoBoardPublisher`. O `NOTIFY` sai **depois** do commit —
   anunciar dentro da transação arriscaria propagar mudança que sofre rollback.
   **Este é o único publicador**: não existe gatilho de `NOTIFY` no esquema, e a
   janela de perda entre commit e envio é fechada pela varredura de retomada do
   listener descrita em SDR-004.
6. Toda instância em `LISTEN` recebe o evento e retransmite às suas sessões
   inscritas, no tópico do board e nas filas pessoais (pesquisa I-05).
7. O cliente compara o `seq` recebido com o último conhecido; havendo lacuna ou
   reconexão, resincroniza por `GET /v1/projetos/{projetoId}/board` (ADR-004).

**A transição que mais erra.** Mover uma tarefa fecha `PERMANENCIA` e
`ESPERA_TOMADA` na origem e abre as duas no destino, mas **não toca** no
impedimento aberto — nem no intervalo, nem na marca: RN-009 manda o impedimento
acompanhar a tarefa sem que a transição encerre nem reinicie a contagem, e RN-032
nomeia o registro do desfecho como a única operação que apaga a marca. É o que
SCN-006.3 verifica, e a consequência direta da decisão Q-01 do demandante.

O passo 4 não precisa de nenhum cuidado especial para preservar a marca, e essa é
a razão de ela ser derivada em vez de coluna: a preservação virou propriedade do
esquema. Enquanto era valor de `condicao`, o passo 4 tinha de lembrar de não
sobrescrevê-la — e não lembrava, o que o achado INC-01 mostrou.

**A conclusão tem dois caminhos e uma só transição.** Chegar a etapa terminal por
`POST /movimentos` conclui a tarefa (SCN-011.1). A rota
`POST /v1/tarefas/{tarefaId}/conclusao` existe para dar superfície à recusa que
SCN-011.2 congela — tentar concluir fora de etapa terminal — e responde `200`
apenas quando a tarefa já está concluída, como idempotência de RN-031. Ela não
transiciona nada. A verificação de RN-011 (impedimento aberto impede concluir) é
uma só, compartilhada pelos dois caminhos.

---

## 6. Dependências Inter-Sistemas

| Sistema | Interface | Status | Mock? |
|---------|-----------|--------|-------|
| Provedor de identidade corporativo (Keycloak / RH-SSO) | OpenID Connect — discovery, JWKS, authorization code com PKCE | Disponível; protocolo padrão, sem contrato proprietário | Não |
| PostgreSQL | JDBC e canal `LISTEN/NOTIFY` `board_events` | Componente próprio do sistema | Não |

Nenhum mock contract é necessário, e portanto nenhuma task de substituição fica
pendente. O provedor de identidade é consumido por protocolo padronizado e
documentado, não por API proprietária — `_shared/api-security.md` §1 já fixa a
forma de validação, e não há segundo sistema do workspace envolvido.

**Consequência operacional que não é opcional.** `backend/java/testing.md` §5
registra que o autoconfigure do OAuth2 client resolve o issuer **eagerly** na
subida do contexto Spring. Todo teste que sobe contexto completo exige o provedor
acessível. Com tudo em contêiner (SDR-003), isso deixa de ser passo manual de
README e passa a ser o `compose` de teste — ver [`quickstart.md`](kanban-tarefas/quickstart.md).

---

## 7. Estratégia de Testes

Conforme `backend/java/testing.md` e `frontend/nextjs/testing.md`, com os dois
desvios de SDR-003.

| Tipo | Ferramenta | Cobertura alvo |
|------|-----------|----------------|
| Unitário — regra de negócio | JUnit 5 + Mockito, sem contexto Spring | Toda regra de RN-001 a RN-035; ciclo curto, sem contêiner |
| Unitário — schema e util (frontend) | Jest + Testing Library | Schemas Zod e funções puras |
| Integração — repositório e projeção | **Testcontainers PostgreSQL**, mesma imagem do `compose` | Migrations, índices únicos parciais, reconstrução da projeção |
| Integração — contrato REST | `MockMvc` + `spring-security-test` (`jwt()`) | Cada campo do JSON por `jsonPath`; 2xx, `403`, `409`, `422` |
| Componente e acessibilidade | Testing Library + Storybook `addon-a11y` | Estados das 11 telas, mais os cenários `integração` com asserção de apresentação — ver abaixo |
| E2E | **Playwright** | Os **9** cenários tipados `e2e`: SCN-001.1, 001.2, 003.1, 014.2, 015.2, 019.4, 020.1, 020.2, 020.3 |
| Carga | Gatling ou k6, contra o `compose` | Envelopes de RNF-001, RNF-002 e RNF-009 |
| Cobertura | JaCoCo | > 80% (`definition-of-done.md`) |

**Contagem.** O PRD v1.5 tem **70 cenários: 9 `e2e`, 56 `integração` e 5
`unitário`.** Os três últimos são SCN-022.1 a SCN-022.3, da emenda de
2026-09-10, todos `integração`: a criação de projeto não tem tela no material de
design e não produz broadcast, então nada nela exige navegador. A versão anterior desta seção e o SDR-003 falavam em 14 `e2e`
contra os 8 que o PRD v1.0 tinha; o número errado era da TechSpec. A primeira
emenda de 2026-09-09 acrescentou cinco cenários — SCN-007.4, SCN-019.4 e
SCN-021.1 a SCN-021.3 —, dos quais só SCN-019.4 é `e2e`, e é `e2e` pelo mesmo
motivo dos demais: exige socket aberto de uma sessão enquanto outra altera a
participação. A segunda emenda acrescentou SCN-012.4, de `integração`, que
exercita a recusa do encerramento com impedimento aberto.
A escolha do Playwright continua de pé pelo motivo que nunca foi massa: SCN-020.1
e SCN-020.3 exigem duas sessões simultâneas sobre o mesmo board, e SCN-015.2
exige provar que uma ação de escrita não é oferecida **nem** aceita. jsdom não
verifica nenhum dos três.

**Cenários `integração` com asserção de apresentação.** Alguns cenários tipados
`integração` afirmam algo sobre a superfície, não sobre o contrato: SCN-003.2 e
SCN-003.3 (o que o board exibe) e SCN-016.2 (o recorte por pessoa não é
*oferecido*, além de recusado). MockMvc cobre a metade servidor e não a outra.
Esses cenários recebem **também** verificação de componente, e a lista está aqui
para que `/tests` não escolha por conta própria. O tipo declarado no PRD admite a
verificação; o que faltava era a TechSpec dizer com o quê.

**Por que H2 não serve.** H2 não implementa `LISTEN/NOTIFY`, que é o mecanismo de
RF-020, nem a semântica de intervalo e os índices únicos parciais do
`data-model.md`. Um `@DataJpaTest` verde em H2 diria pouco sobre o comportamento
sob verificação, e diria com aparência de prova.

**Testes que a especificação obriga e que não caem de nenhum cenário:**

- **Reconstrução da projeção** — percorrer uma tarefa, capturar as projeções,
  reconstruir a partir do log, comparar. É o que sustenta a alegação de SDR-001 de
  que a projeção é descartável. Sem ele, a alegação não tem lastro.
- **Imutabilidade do log** (RNF-008) — tentar alterar tempo já contado por cada
  caminho exposto pelo produto e verificar a recusa, inclusive no nível da role de
  banco.
- **Ausência estrutural de recorte por pessoa** (RN-014, SCN-016.2) — verificar
  que a rota rejeita parâmetro de pessoa e que a resposta não o contém. Testar
  ausência é desconfortável e é exatamente o que impede a erosão silenciosa.
- **Broadcast multi-instância** (RNF-002) — **3 instâncias e 300 sessões**, uma
  escrita, sessões observando em instâncias diferentes. A escala é a do envelope
  do PRD; a versão anterior desta seção dizia "duas instâncias, duas sessões" e
  contradizia a própria Seção 9.
- **Carga de RNF-001** — 100 tarefas e 50 sessões no mesmo projeto, medindo p95 do
  aceite da escrita até a atualização visível. Métrica de produção observa; não
  executa a condição de medição declarada, e sem este teste RNF-001 chega ao
  GATE-NFR sem verificação.
- **Envelope de RNF-009** — carga sintética com 12 meses e 5.000 tarefas, medindo
  RF-015 e RF-016.
- **`seq` sob concorrência** (SDR-004) — escritas simultâneas em tarefas
  diferentes do mesmo projeto, verificando ausência de duplicata e de buraco após
  rollback.
- **Ortogonalidade das três dimensões** (RN-002, RN-032) — para cada operação que
  não é o registro do desfecho — mover, devolver, assumir, renomear, remover
  participação —, verificar que a marca de impedimento sobrevive. Os cenários
  congelados cobrem a movimentação (SCN-006.3), a devolução (SCN-008.3) e a
  tomada (SCN-007.4); as demais operações só têm a garantia estrutural do
  esquema, e é ela que este teste exercita.

Dois itens saíram desta lista na emenda de 2026-09-09, porque passaram a ter
cenário congelado: a **autorização de canal revogada** virou SCN-019.4 (INC-06) e
a **promoção a admin global** virou SCN-021.1 (INC-03). Ambos continuam sendo
testados; deixaram é de ser verificação sem requisito de origem.

**Onde os testes rodam.** A suíte roda em contêiner e os testes de integração
sobem PostgreSQL por Testcontainers — o que exige acesso ao daemon Docker do host.
A decisão é **montar o socket do host no serviço de teste**, com o efeito de
segurança assumido e restrito a ambiente de desenvolvimento e CI; DinD foi
descartado pelo custo de camada e pela perda de cache. Isso deixou de ser decisão
local desta feature: virou **ADR-012** e norma de `infra/docker`
(`testing.md` §3). A versão da imagem
PostgreSQL vive numa variável única consumida pelo `compose` e pelo Testcontainers:
"a mesma imagem" não pode ser convenção em prosa, que é o modo de falha que
motivou tirar o H2. Playwright roda em serviço próprio, com browsers na imagem, na
mesma rede do frontend. Ver [`quickstart.md`](kanban-tarefas/quickstart.md) §3.

Os cenários unitários continuam sem contêiner. É o que mantém curto o ciclo de
quem implementa, apesar de Docker estar no caminho crítico do resto.

---

## 8. Segurança e Observabilidade

**Segurança:**

- OAuth2 Resource Server; JWT validado localmente contra o JWKS do realm —
  assinatura, `iss`, `aud`, `exp`, allowlist de algoritmo, tolerância de relógio
  ≤ 60 s. Token opaco e `alg: none` recusados (`_shared/api-security.md` §1).
- Sem fallback de autenticação local (ADR-006). Provedor indisponível devolve
  `503` e nenhum caminho alternativo é oferecido (SCN-001.3).
- Default de rota não mapeada é **negar**. Toda rota declara a permissão exigida.
- Checagem grossa na borda, **checagem fina no serviço sobre a participação real
  no projeto** — nunca sobre id ou papel vindos do cliente (RNF-004, BDR-001).
  RN-015 exige que a recusa valha também quando a ação é solicitada por outro
  caminho, e é por isso que a verificação não pode viver só na interface. A
  formulação pressupõe rota com escopo de projeto, e há **uma exceção nomeada**:
  `POST /v1/projetos` (RN-036) não consulta participação porque não há projeto do
  qual participar antes da gravação — a checagem fina ali é a administração global
  do chamador, e ela também é do servidor, não da interface.
- `403` quando autenticado sem permissão; `404` quando revelar a existência do
  recurso já seria vazamento (SCN-002.3). **Participação é o eixo da existência e
  papel é o eixo da capacidade** (decisão de 2026-09-10): quem não participa
  recebe `404`, quem participa sem o papel exigido — inclusive sem papel algum, ou
  só com `user` — recebe `403`. A regra completa, com a razão, está em
  `contracts/sessao-e-projetos.md`. Ela obriga o resolvedor a devolver
  `participa` ao lado das permissões: os dois casos produzem conjunto vazio e
  exigem respostas opostas, então derivar a distinção de conjunto vazio é
  exatamente o erro.
- Inscrição em tópico STOMP autorizada no servidor contra a participação.
- **Admin global** (ADR-010, **RF-021 e RN-035**): promovido por `sub`
  configurado, uma única vez, com registro em `WARN`. Dispensa a participação para
  ver e agir em qualquer projeto, e a resposta de `GET /v1/projetos` marca o
  acesso como administração global em vez de participação. Não contorna RN-014 —
  não há coluna de pessoa a agregar — nem RNF-008, garantido na role de banco: é
  alcance de escopo, não imunidade. Deixou de ser capacidade sem origem: INC-03
  apontou que era a única travessia da fronteira entre visibilidade e execução sem
  requisito, e o PRD v1.1 a instituiu com SCN-021.1 a SCN-021.3. Desde o PRD v1.3
  ela deixou de ser só alcance de leitura e escrita: **criar projeto é capacidade
  exclusiva dela** (RN-036), a única do sistema que nenhum papel de projeto possui
  nem pode vir a possuir, porque nenhum papel de projeto existe antes do projeto.
- **Limite de requisições** (`_shared/api-security.md` §4): throttling **por
  sujeito autenticado**, com `429` e `Retry-After` em `problem+json`. Envelope
  inicial de 120 requisições por minuto por sujeito para leitura e 30 por minuto
  para escrita, ajustável por configuração. Não há dimensão por origem — ela foi
  removida na revisão de TASK-01.6, porque `X-Forwarded-For` é escolhido pelo
  cliente na topologia real e porque envelope compartilhado contradiz a condição
  de medição de RNF-010. **A contagem é por instância**, e o desenho prevê três
  (RNF-002): o envelope efetivo é multiplicado pelo número de réplicas, e
  contador compartilhado exigiria o armazenamento que ADR-002 recusa nesta fase.
  Ambos detalhados no contrato `sessao-e-projetos.md`. O envelope deixou de ser instituído aqui e
  passou a ser **RNF-010** (INC-05), com a ausência de base empírica declarada
  dentro do próprio requisito e revisão marcada para o fim do primeiro período de
  uso. Não existe gateway na Seção 6 de onde isso seria herdado, e o fan-out do
  `NOTIFY` amplifica o custo de uma requisição abusiva sobre RNF-001 e RNF-002 —
  é a única proteção que esses envelopes têm.
- Sem transcode de token: não há API downstream chamada em nome do usuário. **Não
  há client confidencial no backend**: ele é Resource Server puro, e nenhum
  `client secret` é configurado nele. O fluxo authorization code com PKCE vive no
  frontend, com client público.
- HTTPS na borda, CORS por allowlist explícita, limite de payload, cabeçalhos de
  endurecimento, Swagger protegido ou desligado em produção.
- Segredos — credencial de banco, sobretudo — por configuração externa. Nunca em
  imagem, `application.yml` versionado ou repositório. **Em produção, por Docker
  secret montado como arquivo**, não por variável de ambiente: variável vaza em
  inspeção de contêiner e em dump de processo. Em desenvolvimento, valor descartável é aceitável
  **desde que entre pelo mesmo mecanismo** — caminho de desenvolvimento diferente
  do de produção significa que o de produção nunca é exercitado. Deixou de ser
  decisão local: é a norma de `infra/docker` (`architecture.md` §7), que também
  proíbe segredo em `ARG` de build, onde ele sobrevive na história de camadas.
- `dados` do log em `jsonb` recebe motivo e desfecho; **nunca dado real de
  cliente** (IDSD 4.10.1).

**Observabilidade:**

- **Logs:** `@Slf4j`, níveis de `_shared/logging-and-levels.md`, com
  `X-Correlation-Id` propagado e o mesmo `traceId` que aparece no `problem+json`.
  Token nunca logado. Eventos de segurança — `403`, uso de permissão de
  configuração, remoção de participação — em `INFO`/`WARN` com sujeito e ação.
- **Métricas** (Micrometer/Actuator, sem APM nesta fase): latência do aceite da
  escrita até o `send` STOMP, para RNF-001; reconexões da conexão de escuta por
  instância, para RNF-002; p95 das consultas de RF-015 e RF-016, para RNF-009;
  divergências detectadas entre log e projeção pela rotina de reconstrução.
- **Saúde:** os dois probes são **separados**, e colapsá-los seria um erro caro.
  **Liveness** observa só o processo, nunca dependência externa. **Readiness**
  inclui banco e conexão de escuta `LISTEN` — instância com o listener caído não
  deve receber tráfego, porque serviria board que não atualiza (ADR-004). Se o
  listener entrasse no liveness, uma instabilidade do banco mataria **todas** as
  instâncias ao mesmo tempo, durante a janela de backoff em que elas se
  recuperariam sozinhas. Tolerância: a instância sai de readiness após 15 s de
  listener caído, e o backoff de reconexão segue rodando.
- **RNF-007** é requisito de produto, não de plataforma: as quatro séries de
  sucesso saem de `intervalo_tarefa` e da exportação de RF-016, não das métricas
  de infraestrutura. Elas não existem hoje e é este desenho que as cria.

---

## 9. Matriz de Rastreabilidade

| RF/RNF | Implementado em | Validado por |
|--------|----------------|-------------|
| RF-001 | `contracts/sessao-e-projetos.md` — `GET /v1/sessao`; ADR-003, ADR-006 | SCN-001.1 a SCN-001.3 |
| RF-002 | `contracts/sessao-e-projetos.md` — `GET /v1/projetos` | SCN-002.1 a SCN-002.4 |
| RF-003 | `contracts/board-e-tarefas.md` — `GET .../board`; projeções de `data-model.md` §5 | SCN-003.1 a SCN-003.3 |
| RF-004 | `contracts/board-e-tarefas.md` — `POST .../tarefas` | SCN-004.1 a SCN-004.3 |
| RF-005 | `contracts/board-e-tarefas.md` — `POST .../movimentos`; SDR-002 | SCN-005.1 a SCN-005.3 |
| RF-006 | `contracts/board-e-tarefas.md` — `POST .../movimentos`, exceção de RN-005 | SCN-006.1 a SCN-006.3 |
| RF-007 | `contracts/board-e-tarefas.md` — `POST .../tomada`; RN-033, sem pré-condição sobre impedimento | SCN-007.1 a SCN-007.4 |
| RF-008 | `contracts/board-e-tarefas.md` — `DELETE .../tomada` | SCN-008.1 a SCN-008.3 |
| RF-009 | `contracts/board-e-tarefas.md` — `POST .../impedimentos`; índice único parcial | SCN-009.1 a SCN-009.3 |
| RF-010 | `contracts/board-e-tarefas.md` — `.../resolucao`; BDR-002; RN-032 — única operação que apaga a marca | SCN-010.1 a SCN-010.3 |
| RF-011 | `contracts/board-e-tarefas.md` — efeito de mover para etapa terminal, mais `POST .../conclusao` como superfície da recusa de SCN-011.2 | SCN-011.1 a SCN-011.3 |
| RF-012 | `contracts/board-e-tarefas.md` — `POST .../encerramento`, com a verificação de RN-011 compartilhada com a conclusão e a movimentação | SCN-012.1 a SCN-012.4 |
| RF-013 | `contracts/board-e-tarefas.md` — `POST .../reabertura`, retorno à primeira etapa por RN-034; `episodio` em `data-model.md` §4 | SCN-013.1 a SCN-013.3 |
| RF-014 | `contracts/fila-e-consultas.md` — `GET /v1/fila` | SCN-014.1 a SCN-014.3 |
| RF-015 | `contracts/fila-e-consultas.md` — `GET .../andamento` | SCN-015.1 a SCN-015.3 |
| RF-016 | `contracts/fila-e-consultas.md` — `GET .../tempo-por-etapa`; SDR-001 | SCN-016.1 a SCN-016.3 |
| RF-017 | `contracts/sessao-e-projetos.md` — `PUT .../etapas` | SCN-017.1 a SCN-017.3 |
| RF-018 | `contracts/sessao-e-projetos.md` — `PUT .../raias` | SCN-018.1 a SCN-018.3 |
| RF-019 | `contracts/sessao-e-projetos.md` — participações e derrubada das sessões STOMP na mesma transação; BDR-001, BDR-002 | SCN-019.1 a SCN-019.4 |
| RF-020 | `contracts/eventos-tempo-real.md`; ADR-004, pesquisa I-05 | SCN-020.1 a SCN-020.3 |
| RF-021 | `contracts/sessao-e-projetos.md` — promoção por `sub` em `GET /v1/sessao` e alcance global em `GET /v1/projetos`; ADR-010, RN-035; Seção 8 | SCN-021.1 a SCN-021.3 |
| RF-022 | `contracts/sessao-e-projetos.md` — `POST /v1/projetos`, `projeto` e primeira `participacao` na mesma transação; RN-036 a RN-038, ADR-010 | SCN-022.1 a SCN-022.3 |
| RNF-001 | Seção 5, passos 5 a 7; `afterCommit` + `LISTEN/NOTIFY`; SDR-004 | **Teste de carga** com 100 tarefas e 50 sessões, medindo p95 aceite→repintar. Métrica Micrometer é evidência complementar, não meio de verificação |
| RNF-002 | ADR-004; SDR-004; canal único de I-05; resincronização por `seq` | Teste de broadcast multi-instância, 3 instâncias e 300 sessões (escala igual à da Seção 7) |
| RNF-003 | ADR-008; `quickstart.md` | Subida em ambiente limpo e percurso TL-03 → TL-04 → TL-06 |
| RNF-004 | Seção 5 passos 2 e 3; Seção 8; ADR-010 | Requisição forjada por operação de escrita; revogação de inscrição STOMP com socket aberto; promoção a admin global recusada quando já existe um |
| RNF-005 | `frontend/nextjs/design-system.md`, desktop-first ≥ 1024 px | Percurso das 11 telas em 1280 px e 1024 px |
| RNF-006 | DDR-005; Storybook `addon-a11y` e Playwright | Verificação automatizada nas 11 telas e percurso completo por teclado |
| RNF-007 | `intervalo_tarefa` e exportação de RF-016; Seção 8 | As quatro séries existem, são consultáveis e nenhuma é apurável por pessoa |
| RNF-008 | `evento_tarefa` append-only; concessão restrita de `SELECT, INSERT` | Tentativa de alteração por cada caminho exposto |
| RNF-009 | Índices de recorte e de duração em `intervalo_tarefa` (`data-model.md` §6); pesquisa I-06 | Carga sintética com 12 meses e 5.000 tarefas |
| RNF-010 | Seção 8, limite de requisições; `_shared/api-security.md` §4 — 120 leituras e 30 escritas por minuto por sujeito | Teste de throttling por sujeito, verificando `429` com `Retry-After` e a não contaminação entre sujeitos |

---

## 10. Questões em Aberto

| # | Questão | Responsável | Prazo |
|---|---------|------------|-------|
| Q-001 | ~~Corte concreto de "baixa massa" em RF-016 (RN-029)~~ — **resolvida em 2026-09-09 pelo demandante: `baixaMassa: true` apenas com amostra única; a partir de 2 amostras a série vem sem ressalva.** Registrada em `contracts/fila-e-consultas.md` | Demandante | Resolvida |
| Q-002 | I-07 — se o retry com backoff da conexão de escuta basta sob queda prolongada do banco. A rede de segurança já existe (resincronização por `seq`); resta medir | Implementação | Durante `/implement` |
| Q-003 | I-08 — se Playwright e Testcontainers devem subir para a biblioteca compartilhada. É decisão de `/guidelines`, e escrevê-la aqui como norma geral é o que a regra negativa desta skill proíbe | `/guidelines` | Após o primeiro release |
| Q-004 | ~~Pendência 02 do `state.md`: `infra/docker` é stub~~ — **resolvida em 2026-09-09 pelo `/guidelines`.** A coleção foi elaborada com 19 critérios verificáveis e três ADRs; as decisões locais desta TechSpec foram absorvidas por ela | `/guidelines` | Resolvida |
| Q-005 | ~~O protótipo TL-04 desabilita "mover" em cartão impedido~~ — **resolvida em 2026-09-09 pelo `/design`.** A ação foi habilitada, a ficha passou a exibir as três dimensões em campos separados e a decisão virou DDR-007. TL-03, TL-03b e TL-06 realinhados; TL-06 ganhou o item de fila impedido e assumível de SCN-007.4 | `/design` | Resolvida |
| Q-006 | Sinalização de espera de tomada usa `brand.ocean` contra o primário `#004B8D` — dois azuis possivelmente indistinguíveis em board denso. Sinalizado pelo agente prototipador e nunca resolvido | `/tasks` | Antes de `/implement` |
| Q-007 | O papel semântico de impedimento não existe na coleção `frontend/nextjs`. Acrescentá-lo afeta outros sistemas governados por ela | `/guidelines` e `/tasks` | Antes de `/implement` |
| Q-008 | ~~Nenhum cenário congelado cobre o bypass do admin global~~ — **resolvida em 2026-09-09.** O `/analyze` a promoveu a INC-03 e o PRD v1.1 criou RF-021, RN-035 e SCN-021.1 a SCN-021.3. A capacidade deixou de ser escopo órfão | `/prd` | Resolvida |
| Q-009 | Não há CI, e o `/guidelines` decidiu **não inventar plataforma**: `infra/docker` escreveu os pré-requisitos que qualquer pipeline terá de satisfazer (runner com daemon acessível, cache entre execuções, quais critérios reprovam o build) e deixou a escolha como dívida nomeada. O que resta é a escolha em si, e ela não bloqueia: todos os 19 critérios são verificáveis na máquina de quem desenvolve | Demandante e `/tasks` | Após o primeiro release |
| Q-010 | ~~Migração concorrente na subida~~ — **resolvida em 2026-09-09 por ADR-011.** A migração sai do boot e vira serviço dedicado que roda e sai; as instâncias sobem com `validate` e dependem dele por `service_completed_successfully`. O `start_period` folgado foi recusado por virar número mágico que falha de novo quando a migração crescer | `/guidelines` | Resolvida |
| Q-011 | ~~O envelope de rate limit é ponto de partida sem base empírica~~ — **resolvida em 2026-09-09.** Virou RNF-010 (INC-05), com a ausência de base empírica declarada dentro do requisito e revisão marcada para o fim do primeiro período de uso. O confronto com o uso real continua devido, agora com dono no PRD | Demandante | Após o primeiro release |

| Q-012 | A decisão sobre claim ausente (v1.8) exige trabalho que o plano vigente não prevê: a **migration de ordem 7** e o recuo de `nome` no autoprovisionamento, que vive em código já entregue pela primeira fatia do EPIC-01. Distribuí-lo pertence ao `/tasks`, e a regra negativa desta skill proíbe fazê-lo aqui. Não bloqueia o EPIC-01: enquanto o realm for o do `compose`, as duas claims chegam sempre | `/tasks` | Antes de o produto encostar em realm federado |

Nenhuma é bloqueante para o encerramento desta etapa, e **nenhuma bloqueia mais o
`/tasks`**. Q-008 e Q-011 foram fechadas pela emenda do PRD v1.1; Q-005 pelo
`/design`; Q-004 e Q-010 pelo `/guidelines`, que elaborou `infra/docker`. Q-009
deixou de ser bloqueante ao ser reescrita como dívida com pré-requisitos
declarados. Q-003 continua aberta e ficou mais estreita: o **ambiente** de teste
subiu para a biblioteca (ADR-012, `infra/docker/testing.md`); o que resta decidir é
se as ferramentas — Playwright e Testcontainers — viram norma das coleções de
stack, que é outra pergunta e não depende desta feature.

**Nenhum escopo novo foi descoberto e nenhuma devolução ao `/prd` foi consumida.**
Os 22 RFs e os 70 cenários couberam no desenho sem exigir regra que o PRD não
tivesse. A rota de conclusão que esta revisão acrescentou não é escopo novo: ela
realiza uma recusa que SCN-011.2 já congelava e que o desenho anterior deixara
sem superfície a exercitar. O caso que mais se aproximou de exigir devolução foi o fallback de RN-024
sob BDR-002 — verificou-se que SCN-019.3 continua alcançável e que não há emenda a
fazer.

---

## 11. Histórico de Revisões

| Versão | Data | Autor | Alteração |
|--------|------|-------|-----------|
| 1.0 | 2026-09-09 | agente `/techspec` | Versão inicial. Cinco DRs novos: ADR-009, BDR-002, SDR-001, SDR-002, SDR-003 |
| 1.1 | 2026-09-09 | agente `/techspec` | Comitê de análise (architect, database, security, devops, qa): 15 achados aplicados. Dois DRs novos — SDR-004 (geração do `seq` e publicador único) e ADR-010 (bootstrap do admin global por `sub`). Correções estruturais: `etapa_id` do impedimento deixa de ser nulo, `impedimento` ganha `projeto_id`, coluna gerada `duracao`, gatilho de `NOTIFY` removido. Contagem de cenários `e2e` corrigida de 14 para 8. Quatro questões novas na Seção 10 |
| 1.3 | 2026-09-09 | agente `/techspec` | Absorção da segunda emenda do PRD (v1.2), que fechou INC-11 e INC-12. O encerramento sem conclusão passa a recusar `422` com impedimento aberto (RN-011, SCN-012.4), pela mesma verificação já compartilhada pela conclusão e pela movimentação para etapa terminal — três caminhos, uma implementação. Como consequência, `TAREFA_ENCERRADA_SEM_CONCLUSAO` deixa de fechar "todos" os intervalos e fecha `PERMANENCIA` e `ESPERA_TOMADA`: não há `IMPEDIMENTO` aberto a fechar, porque o evento nunca é gravado com a marca acesa. Contagem para 66 cenários — 9 `e2e`, 52 `integração`, 5 `unitário` |
| 1.4 | 2026-09-09 | agente `/techspec` | Absorção da coleção `infra/docker`, elaborada pelo `/guidelines` no mesmo dia. Nenhuma decisão nova nesta etapa: três decisões que esta TechSpec vinha tomando localmente subiram para a biblioteca e voltaram como ADR-011 (migração em serviço dedicado), ADR-012 (socket do host, DinD descartado) e ADR-013 (digest obrigatório, sem registry). A mudança de comportamento é uma só e está no `quickstart.md`: Flyway sai do boot da aplicação e vira serviço próprio, com as instâncias dependendo dele por `service_completed_successfully`. Q-004 e Q-010 resolvidas, Q-009 reescrita como dívida não bloqueante, Q-003 estreitada. Nenhum cenário, contrato ou entidade alterado — contagem inalterada em 66 cenários |
| 1.5 | 2026-09-10 | agente `/techspec` | Absorção da emenda do PRD v1.3, que criou RF-022 e as regras RN-036 a RN-038 para fechar a lacuna encontrada pela `/tests`. Uma rota nova: `POST /v1/projetos`, única do sistema cuja autorização não consulta participação, porque não há participação a consultar antes de o projeto existir. Ela grava `projeto` com `seq_atual = 0` e a primeira `participacao` com papel `project_admin` **na mesma transação** — em duas, a falha da segunda reproduz exatamente o projeto inalcançável que a lacuna produzia. Quem cria não vira participante: inserir a participação do administrador global apagaria a marca `acessoPorAdministracaoGlobal` para ele nesse projeto, contra SCN-021.2. `403` e não `404` na recusa, porque a coleção é conhecida do chamador e não há existência a ocultar. SCN-022.3 não exigiu mecanismo novo: o `422` de projeto sem fluxo já existia em `POST /projetos/{id}/tarefas` por SCN-004.3, e a emenda apenas o torna parte declarada do caminho de instalação. Nenhuma entidade nova e nenhum evento novo — a criação não é publicada, porque o canal a que ela seria publicada pressupõe o projeto. Contagem para 69 cenários — 9 `e2e`, 55 `integração`, 5 `unitário` |
| 1.6 | 2026-09-10 | agente `/techspec` | Correção de INC-20 e propagação da tela TL-11, instituída pelo demandante ao fechar INC-18. Nenhuma decisão técnica nova. A Seção 8 afirmava "checagem fina no serviço sobre a participação real no projeto" em forma universal, e `POST /v1/projetos` não consulta participação porque não há projeto do qual participar antes da gravação — a frase ganhou a exceção **nomeada**, com a checagem fina ali sendo a administração global do chamador, também do servidor e não da interface. O marcador de admin global passou a registrar que criar projeto é capacidade exclusiva dela (RN-036), a única que nenhum papel de projeto possui nem pode vir a possuir. Universo de medição de RNF-005 e RNF-006 de 10 para **11 telas**, com o mesmo ajuste no `quickstart.md` e no SDR-003. Nenhum cenário, contrato ou entidade alterado — 69 cenários |
| 1.7 | 2026-09-10 | agente `/techspec` | Absorção da emenda do PRD v1.5, que fechou INC-22 instituindo como requerida a sinalização do fluxo não configurado. Uma decisão técnica pequena e uma consequência que vale registrar. A decisão: `fluxoConfigurado` entra na saída de `GET /v1/projetos` como booleano derivado no servidor por `EXISTS` sobre `etapa`, e não como lista de etapas que o cliente inspecionaria. Devolver as etapas ali seria carga inútil no caminho mais quente da navegação, e deixaria a marca dependendo de o cliente interpretar coleção vazia — a mesma classe de erro que RNF-004 proíbe em autorização, aplicada a apresentação. A consequência: SCN-002.4 é o primeiro cenário congelado que verifica **ausência** de configuração como dado de saída, e por isso a fixture de projeto sem etapa deixa de ser detalhe do teste de criação e passa a ser estado de primeira classe da suíte de projetos. Nenhuma entidade, evento ou rota nova — 70 cenários, 9 `e2e`, 56 `integração`, 5 `unitário` |
| 1.8 | 2026-09-10 | agente `/techspec` | Revisão estreita fechando os três achados de revisão e de implementação com dono nesta skill. Nenhum requisito novo, nenhum cenário tocado, nenhuma devolução ao `/prd`. (1) `data-model.md` §8 listava `projeto.seq_atual` na migration 6, e ele nasce na 1, que é onde a implementação corretamente o criou — quem seguisse a tabela antiga escreveria `ADD COLUMN` sobre coluna existente, e a migração falharia contra qualquer banco já migrado, no serviço dedicado, com o backend parado esperando por `service_completed_successfully`. A decisão estava certa desde a task; faltava propagá-la. (2) **`403` contra `404` passa a ter regra única e escrita**: participação é o eixo da existência, papel é o eixo da capacidade — quem não participa recebe `404`, quem participa sem o papel exigido recebe `403`, inclusive sem papel algum ou só com `user`. O que decide é que `GET /v1/projetos` lista por participação e não por permissão, então o projeto já apareceu numa resposta `200` uma rota antes: `404` ali contradiria aquele `200` e tiraria da pessoa a única informação acionável que ela tem. A regra obriga o resolvedor a devolver `participa` ao lado das permissões, porque os dois casos produzem conjunto vazio e exigem respostas opostas. (3) **Token válido sem `name` ou sem `email`** é estado alcançável em realm federado, e com as colunas `NOT NULL` a primeira entrada dessa pessoa violava restrição e o autoprovisionamento de RF-001 respondia `500` — falha de instalação disfarçada de defeito, na única rota que ela alcança. `email` passa a ser anulável, porque ADR-010 já proíbe que qualquer autorização o consulte e ele é campo de exibição; `nome` continua obrigatório e recua para `preferred_username` e depois para o `sub`, porque é exibido no cartão e na fila e nulo ali produziria interface sem quem. Recusar a entrada foi descartado: o token é legítimo, o que falta é configuração do realm, e ADR-006 não deixa caminho alternativo para quem for recusado. Claim ausente preserva o valor gravado em vez de sobrescrevê-lo com nulo, senão uma mudança no provedor esvaziaria em massa o e-mail de quem já entrou. Entra a migration de ordem 7 (`DROP NOT NULL`), e não a alteração da 1, que já foi aplicada |
| 1.9 | 2026-09-11 | agente `/techspec` | Correção estreita de ACH-08 da revisão da fatia de projetos visíveis, no contrato de `sessao-e-projetos`. Nenhum requisito novo, nenhum cenário tocado. A regra única de `403`/`404` instituída na v1.8 tinha um corolário que ficou implícito, e implícito ali é caro: se a relação de projetos é visível a quem o detalhe recusa, então **tudo o que a relação carrega é, por definição, o que alguém sem `LER` pode ver**. O corolário passa a estar escrito e a nomear os dois lados — `id`, `nome`, `papeis`, `permissoes` e as marcas são **metadado da participação**; `descricao` e todo conteúdo do projeto são **dado protegido por `LER`**, e vivem só no detalhe. `descricao` sai da saída de `GET /v1/projetos`. A leitura alternativa — proteger também o `nome` — foi considerada e recusada porque desmonta a regra da v1.8: sem `nome` a relação exibiria item inidentificável, e omitir o item faria `404` voltar a ser a resposta coerente do detalhe, que é exatamente a contradição com o `200` da rota anterior que a regra existe para impedir. É também o que torna o `403` acionável — pedir o papel a quem administra exige saber de qual projeto se fala. A rota de detalhe ganhou a linha de saída `200` e o `403` na de erros, que faltavam |
| 1.10 | 2026-09-11 | agente `/techspec` | Correção estreita de ACH-11 da revisão de TASK-01.6, na Seção 8 e no contrato de `sessao-e-projetos`. Nenhum requisito novo, nenhum cenário tocado. Duas propriedades do limite de requisições que estavam operando sem estar escritas. (1) **A dimensão de origem deixa de existir.** Ela era prescrita aqui — "throttling por sujeito e por origem" — e a premissa que a sustentava é falsa contra o `docker/compose.yaml`: não há proxy reverso em produção, o único nginx do repositório existe no arnês de broadcast, e sem proxy que o escreva `X-Forwarded-For` é escolhido pelo cliente. Disso saíam três defeitos simultâneos — a dimensão não continha nada, permitia recusar serviço a terceiros e fazia o mapa de contagem crescer sem teto real. O que decidiu, porém, não foi nenhum dos três: a condição de medição de RNF-010, escrita no PRD, exige provar que **o consumo de um sujeito não afeta a resposta de outro**, e envelope compartilhado por origem afirma exatamente o contrário — o requisito e o mecanismo eram incompatíveis por construção. Removida a dimensão, RNF-010 volta a ser satisfazível sem emenda ao PRD. (2) **A contagem é por instância**, e o desenho prevê três (RNF-002), de modo que o envelope efetivo é o número de RNF-010 multiplicado pelo número de réplicas. Isso já era verdade na implementação e não estava em lugar nenhum: contador em memória por processo é a única forma disponível, porque contador compartilhado exigiria Redis ou tabela de contagem e ADR-002 recusa armazenamento adicional nesta fase. Fica declarado com o gatilho de reabertura — quando houver medição de uso real que confronte o envelope, que é o mesmo gatilho que RNF-010 já carrega |
| 1.11 | 2026-09-14 | agente `/techspec` | Correção estreita de ACH-04 da revisão de TASK-02.1, em `data-model.md` §3 e §5. Nenhum requisito novo, nenhum cenário tocado, nenhuma devolução ao `/prd`. O §3 afirmava, como consequência de RN-023, que **nenhuma** tabela do anel de projeção referencia `raia`, e o §5 do mesmo documento declara `tarefa.raia_id` — duas afirmações opostas sobre a mesma coisa, a poucas dezenas de linhas uma da outra. A que fica é `:250`, e ela não foi escolhida por ser a mais recente: é a única sustentada pelo resto da cadeia — o diagrama ER deste próprio arquivo já liga `tarefa` a `raia (opcional)`, `board-e-tarefas.md` põe `raiaId` no cartão, e a suíte congelada `RaiasIT` monta tarefa com raia, de modo que a generalização contradizia também material fora deste documento, um deles congelado. A garantia verdadeira é **estreita e suficiente**: `raia_id` vive só no estado corrente, a série de tempo (`evento_tarefa`, `intervalo_tarefa`) não a carrega, e nenhuma rota agregada aceita `raiaId`. É essa ausência estreita que torna RN-023 propriedade do esquema, e a razão está agora escrita junto — agregar por raia exigiria juntar a série ao estado corrente, e o estado corrente não sabe em que raia a tarefa estava quando o intervalo correu. A correção precede ACH-02: era a generalização daqui que a migration de TASK-02.1 copiou para o comentário, e ela seria desmentida pela primeira tabela nascida depois dela, que é `tarefa`, em TASK-02.3 |
| 1.2 | 2026-09-09 | agente `/techspec` | Absorção da emenda do PRD v1.1. INC-07 fechado: `POST /v1/tarefas/{tarefaId}/conclusao` passa a existir como superfície da recusa que SCN-011.2 congela, sem ser um segundo caminho de conclusão. `IMPEDIDA` sai do domínio de `condicao` e o impedimento vira dimensão derivada de `impedimento` com `desfecho IS NULL` (RN-002, RN-032) — a preservação da marca deixa de ser disciplina do serviço e vira propriedade do esquema. Tomada aceita com impedimento aberto (RN-033), reabertura na primeira etapa (RN-034), RF-021 e RN-035 na Seção 8 e na matriz, RNF-010 na matriz. Contagem atualizada para 65 cenários — 9 `e2e`, 51 `integração`, 5 `unitário` — e universo de medição de RNF-005/RNF-006 para 10 telas. Q-008 e Q-011 resolvidas; Q-005 reatribuída ao `/design` |

---

## Fora deste artefato — regras negativas

O que **não** entra aqui, e para onde vai:

| Conteúdo | Pertence a | Por quê |
|---|---|---|
| Requisito funcional ou não-funcional novo | `/prd` | não passou pelo gate de spec, não tem cenário nem procedência |
| Cenário de aceite | `/prd` | congelado no gate de spec; reescrevê-lo aqui quebra a verificação independente |
| Task, épico, estimativa, sequenciamento | `/tasks` | plano de execução não é decisão de desenho |
| Dimensão do REASONS Canvas | derivado | o canvas é projetado das fontes por `derive_canvas.py` |
| Norma técnica de aplicação geral | `/guidelines` | se vale além desta feature, escrevê-la aqui a esconde dos outros sistemas |

Escopo novo descoberto durante a TechSpec **volta para o `/prd`** e consome uma
devolução. Resolver aqui é mais rápido e é justamente o que a matriz de
rastreabilidade não consegue detectar depois.
