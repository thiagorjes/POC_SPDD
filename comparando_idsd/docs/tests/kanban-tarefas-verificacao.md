# Plano de Verificação — kanban-tarefas

_Versão 1.3 — 2026-09-10_

Feature: `kanban-tarefas`
Origem: PRD v1.5 (70 cenários congelados), TechSpec v1.7, Tasks (8 épicos, 43 tasks)
Gates: GATE-VERIFICACAO-INDEPENDENTE, GATE-GHERKIN-CONGELADO

---

## Declaração de independência

- **Implementação existente no momento da escrita:** não
- **Arquivos de produção lidos:** nenhuma
- **Arquivos de produção alterados nesse commit:** nenhuma
- **Commit da suíte:** `pendente-de-registro` — a suíte é commitada ao fim desta
  etapa e o SHA é selado pelo `/evidence`

A suíte foi escrita **antes** da implementação e **sem acesso** a ela.

Não existe implementação a acessar: o repositório não tem `pom.xml`,
`package.json`, `compose.yaml` nem código de produção da feature. Esses
artefatos nascem em TASK-01.1 e TASK-01.2, cujo dono é o `/implement`. A
independência aqui não é disciplina declarada — é uma condição de fato,
verificável pelo estado do repositório no momento em que esta suíte foi
escrita.

Arquivos de produção lidos durante a etapa: zero.
Código de produção escrito durante a etapa: zero, incluindo esboço para
compilar. Onde a suíte precisou do formato de uma resposta, ela o tirou do
contrato da TechSpec; onde o contrato calava, o silêncio virou lacuna
registrada na seção de desvios, e não invenção.

A única exceção declarada, e ela não é de produção: o arnês de topologia em
`backend/src/test/java/br/com/idsd/kanban/alem/` sobe contextos completos da
aplicação para verificar o broadcast entre instâncias. Ele vive no fonte de
teste, a aplicação não o importa, e o que ele conhece do sistema é apenas o que
o contrato congelado já expõe.

### Correção de 2026-09-10 (v1.3) — achados da revisão de TASK-01.5

Também **não independente**, e pela mesma razão: foi executada depois de
TASK-01.5 estar em disco e por quem havia lido o código de produção. O
GATE-VERIFICACAO-INDEPENDENTE da v1.0 **não cobre** o que se tocou aqui.

Nenhum `.feature`, ID, redação ou tipo de cenário foi alterado.

- **SCN-002.1 tinha uma asserção insatisfazível por qualquer resposta**
  (ACH-05). Caminho JsonPath com filtro devolve uma coleção cujo único elemento
  é o próprio array de `permissoes`, e `hasItem` comparava contra o array, nunca
  contra os itens: a forma positiva reprovava com a permissão presente. O `[*]`
  achata a coleção. A asserção negativa de `gestor` sofria do defeito espelhado
  — sobre coleção vazia ela passa sem verificar nada —, e por isso passou a ser
  precedida de uma positiva (ACH-04).
- **A única asserção da marca de alcance global nunca executava** (ACH-01, lado
  de integração). Em `AdminGlobalIT` ela estava depois de `Cenario.fluxoPadrao`,
  que chama rota de EPIC-02 e derruba o teste antes da requisição — o critério
  de aceite ficava cumprido por inspeção. A relação e o detalhe foram movidos
  para antes da semeadura, que é onde não dependem de etapa alguma, e o detalhe
  ganhou a asserção que faltava.
- **SCN-002.4 perdeu a marca de coberto** (ACH-06). A emenda v1.5 propagou para
  a tabela de cobertura sem que o teste existisse, e ele não é escrevível antes
  da tabela `etapa`.
- **`ExistenciaECapacidadeIT` acrescentado** (ACH-07), fora da contagem de
  cenários, na seção própria.

### Correção de 2026-09-10 (v1.1) — e o que ela custa à independência

A v1.0 acima descreve a escrita original, e continua valendo para ela. Esta
seção registra uma correção posterior, feita **depois** de TASK-01.1 a TASK-01.4
terem sido implementadas, e por isso **não independente**: quem a executou havia
lido o código de produção da sessão e da configuração de segurança na mesma
sessão de trabalho. O GATE-VERIFICACAO-INDEPENDENTE aprovado na v1.0 **não
cobre** os três testes tocados aqui — SCN-001.3, SCN-021.1 e o suporte comum.
Registrar isso é o ponto: uma correção não independente escondida dentro de uma
suíte aprovada é pior que o defeito que ela conserta, porque herda uma
credencial que não é dela.

Nenhum `.feature` foi tocado, e `check_congelamento.py` continua em exit 0.

O que estava errado, e por quê:

- **SCN-021.1 verificava uma rota que a especificação nega.** O step definition
  consultava `GET /v1/administracao/promocoes`, que não existe em PRD, contrato,
  TechSpec nem em nenhuma das 43 tasks. O cenário congelado exige apenas que "a
  promoção fica registrada com quem foi promovido e quando"; o RF-021 declara
  que a promoção **não tem interface no produto**, e o contrato de sessão nomeia
  o log `WARN` como o histórico auditável que RN-035 exige. A rota foi invenção
  da suíte. Passa a verificar o log, casando por conteúdo — nível, identificador
  promovido e instante — e nunca pela redação da mensagem.
- **SCN-001.3 era inalcançável, e teria ficado verde afirmando o contrário.** O
  provedor simulado não estava registrado em property nenhuma, e todos os testes
  usam o pós-processador `jwt()`, que injeta a autenticação pronta e **nunca
  chega ao decodificador**. Com o provedor fora do ar o teste receberia `200`. O
  suporte passa a apontar `jwk-set-uri` para o provedor simulado, e este cenário
  — só ele — envia um token real no cabeçalho. O token precisa ser bem formado:
  cadeia inválida é recusada antes de o JWKS ser procurado, e o teste ficaria
  verde por credencial malformada, sem que indisponibilidade alguma ocorresse.
- **A fixture ausente não podia existir.** O provedor apontava para um
  `identidade/descoberta-200.json` que nunca esteve em disco. O corpo da
  descoberta contém URLs absolutas e a porta é sorteada a cada execução: um
  arquivo estático estaria errado por construção. O corpo passa a ser montado em
  código, e a referência ao arquivo sai.

Dois defeitos vieram de carona, ambos latentes porque nada exercitava este
caminho:

- **A dependência do WireMock não trazia servidor HTTP.** `org.wiremock:wiremock`
  espera Jetty 11 e o BOM do Spring Boot gerencia Jetty na linha 12; o provedor
  falhava no arranque e derrubava o contexto de **todo** teste de integração.
  Trocada por `wiremock-standalone`, que traz o servidor sombreado.
- **A suíte não tinha isolamento entre testes.** O contêiner é reusado e o schema
  aplicado uma vez só, sem limpeza. SCN-021.1 afirma "ainda não existe
  administrador global no sistema", e essa pré-condição passava a ser falsa
  assim que qualquer outro teste entrasse como administrador global — o cenário
  passaria ou falharia conforme a ordem em que o JUnit resolvesse executar as
  classes. O suporte comum passa a esvaziar as tabelas antes de cada teste,
  varrendo o catálogo do próprio banco em vez de uma lista escrita à mão, que
  envelheceria calada na próxima migration.

Medido: os dois cenários **passam**, e as demais falhas das duas classes são
`No static resource v1/projetos` — Red legítimo, pelas rotas de TASK-01.5 em
diante. A execução exigiu excluir, numa cópia temporária, os cinco arquivos de
teste que ainda não compilam por dependerem de tasks posteriores; nada foi
removido do repositório.

### Costura interna fixada pelos testes unitários

Cinco cenários são tipados `unitário` no PRD, e um teste unitário
necessariamente fixa uma interface interna: forma de construtor, nome de
exceção, presença de acessores. Escrevê-los antes da implementação transfere
essa decisão da etapa de implementação para esta.

A costura foi assumida e está declarada como **restrição das tasks
correspondentes**, e não como sugestão:

| Costura fixada | Onde | Task restringida |
| --- | --- | --- |
| `CriacaoDeTarefaService`, construtor por dependências | `internal/tarefa` | TASK-03.2 |
| `TomadaService`, construtor por dependências | `internal/tarefa` | TASK-03.5 |
| `ImpedimentoService(registrador, aplicador, destaque)` | `internal/impedimento` | TASK-04.2 |
| `EtapaService(repositorio)` e `FluxoRequisicao.EtapaDesejada` | `internal/projeto` | TASK-07.2 |
| `RegraDeNegocioViolada` como exceção de recusa de regra | transversal | TASK-02.3 |
| Pacote raiz `br.com.idsd.kanban` | transversal | TASK-01.1 |

Mitigação aplicada: os cinco cenários são cobertos **também** por MockMvc contra
o contrato. Se a costura interna mudar durante a implementação, o cenário
continua verificado pelo teste que não depende dela, e o desvio aparece na
tabela de congelamento em vez de silenciosamente reescrever a prova.

---

## Cenários congelados

70 cenários. A distribuição por tipo é a do PRD: 9 `e2e`,
56 `integração`, 5 `unitário`. **69 cobertos** — SCN-002.4 ficou sem
verificador, e a razão está na tabela de cobertura e na revisão de TASK-01.5
(ACH-06): ele exige `fluxoConfigurado`, que é derivado por existência sobre a
tabela `etapa`, criada no EPIC-02. A marca de coberto foi retirada em vez de
mantida com um teste que não exercita o cenário: cobertura afirmada e não
executada é o modo mais discreto de um gate virar formulário.

- **Alterados desde o gate de spec:** nenhuma — os IDs e a redação são os do
  PRD v1.5, reconfirmado pelo aprovador em 2026-09-10. A emenda v1.3
  **acrescentou** SCN-022.1, SCN-022.2 e SCN-022.3, e a emenda v1.5
  acrescentou SCN-002.4; nenhuma das duas tocou em cenário algum já congelado

| Cenário | Requisito | Épico | Tipo | Arquivo de teste | Situação |
| --- | --- | --- | --- | --- | --- |
| SCN-001.1 | RF-001 | EPIC-01 | e2e | `frontend/e2e/entrada.spec.ts` | coberto |
| SCN-001.2 | RF-001 | EPIC-01 | e2e | `frontend/e2e/entrada.spec.ts` | coberto |
| SCN-001.3 | RF-001 | EPIC-01 | integração | `internal/acesso/SessaoEProjetosIT.java` | coberto |
| SCN-002.1 | RF-002 | EPIC-01 | integração | `internal/acesso/SessaoEProjetosIT.java` | coberto |
| SCN-002.2 | RF-002 | EPIC-01 | integração | `internal/acesso/SessaoEProjetosIT.java` | coberto |
| SCN-002.3 | RF-002 | EPIC-01 | integração | `internal/acesso/SessaoEProjetosIT.java` | coberto |
| SCN-002.4 | RF-002 | EPIC-01 | integração | — | **não coberto** — depende de `fluxoConfigurado`, que exige a tabela `etapa` (EPIC-02). ACH-06 da revisão de TASK-01.5 |
| SCN-003.1 | RF-003 | EPIC-02 | e2e | `frontend/e2e/board.spec.ts` | coberto |
| SCN-003.2 | RF-003 | EPIC-02 | integração | `internal/tarefa/BoardIT.java` + `CartaoDeTarefa.test.tsx` | coberto |
| SCN-003.3 | RF-003 | EPIC-02 | integração | `internal/tarefa/BoardIT.java` + `CartaoDeTarefa.test.tsx` | coberto |
| SCN-004.1 | RF-004 | EPIC-02 | integração | `internal/tarefa/CriacaoDeTarefaIT.java` | coberto |
| SCN-004.2 | RF-004 | EPIC-02 | unitário | `internal/tarefa/CriacaoDeTarefaServiceTest.java` + `CriacaoDeTarefaIT.java` | coberto |
| SCN-004.3 | RF-004 | EPIC-02 | integração | `internal/tarefa/CriacaoDeTarefaIT.java` | coberto |
| SCN-005.1 | RF-005 | EPIC-03 | integração | `internal/tarefa/MovimentacaoIT.java` | coberto |
| SCN-005.2 | RF-005 | EPIC-03 | integração | `internal/tarefa/MovimentacaoIT.java` | coberto |
| SCN-005.3 | RF-005 | EPIC-03 | integração | `internal/tarefa/MovimentacaoIT.java` | coberto |
| SCN-006.1 | RF-006 | EPIC-03 | integração | `internal/tarefa/MovimentacaoIT.java` | coberto |
| SCN-006.2 | RF-006 | EPIC-03 | integração | `internal/tarefa/MovimentacaoIT.java` | coberto |
| SCN-006.3 | RF-006 | EPIC-04 | integração | `internal/tarefa/MovimentacaoIT.java` | coberto |
| SCN-007.1 | RF-007 | EPIC-03 | integração | `internal/tarefa/TomadaIT.java` | coberto |
| SCN-007.2 | RF-007 | EPIC-03 | unitário | `internal/tarefa/TomadaServiceTest.java` + `TomadaIT.java` | coberto |
| SCN-007.3 | RF-007 | EPIC-03 | integração | `internal/tarefa/TomadaIT.java` | coberto |
| SCN-007.4 | RF-007 | EPIC-04 | integração | `internal/tarefa/TomadaIT.java` | coberto |
| SCN-008.1 | RF-008 | EPIC-03 | integração | `internal/tarefa/DevolucaoIT.java` | coberto |
| SCN-008.2 | RF-008 | EPIC-03 | integração | `internal/tarefa/DevolucaoIT.java` | coberto |
| SCN-008.3 | RF-008 | EPIC-04 | integração | `internal/tarefa/DevolucaoIT.java` | coberto |
| SCN-009.1 | RF-009 | EPIC-04 | integração | `internal/tarefa/ImpedimentoIT.java` | coberto |
| SCN-009.2 | RF-009 | EPIC-04 | unitário | `internal/impedimento/ImpedimentoServiceTest.java` + `ImpedimentoIT.java` | coberto |
| SCN-009.3 | RF-009 | EPIC-04 | integração | `internal/tarefa/ImpedimentoIT.java` | coberto |
| SCN-010.1 | RF-010 | EPIC-04 | integração | `internal/tarefa/ImpedimentoIT.java` | coberto |
| SCN-010.2 | RF-010 | EPIC-04 | integração | `internal/tarefa/ImpedimentoIT.java` | coberto |
| SCN-010.3 | RF-010 | EPIC-04 | unitário | `internal/impedimento/ImpedimentoServiceTest.java` + `ImpedimentoIT.java` | coberto |
| SCN-011.1 | RF-011 | EPIC-05 | integração | `internal/tarefa/DesfechosIT.java` | coberto |
| SCN-011.2 | RF-011 | EPIC-05 | integração | `internal/tarefa/DesfechosIT.java` | coberto |
| SCN-011.3 | RF-011 | EPIC-05 | integração | `internal/tarefa/DesfechosIT.java` | coberto |
| SCN-012.1 | RF-012 | EPIC-05 | integração | `internal/tarefa/DesfechosIT.java` | coberto |
| SCN-012.2 | RF-012 | EPIC-05 | integração | `internal/tarefa/DesfechosIT.java` | coberto |
| SCN-012.3 | RF-012 | EPIC-05 | integração | `internal/tarefa/DesfechosIT.java` | coberto |
| SCN-012.4 | RF-012 | EPIC-05 | integração | `internal/tarefa/DesfechosIT.java` | coberto |
| SCN-013.1 | RF-013 | EPIC-05 | integração | `internal/tarefa/ReaberturaIT.java` | coberto |
| SCN-013.2 | RF-013 | EPIC-05 | integração | `internal/tarefa/ReaberturaIT.java` | coberto |
| SCN-013.3 | RF-013 | EPIC-05 | integração | `internal/tarefa/ReaberturaIT.java` | coberto |
| SCN-014.1 | RF-014 | EPIC-06 | integração | `internal/tempo/FilaPessoalIT.java` | coberto |
| SCN-014.2 | RF-014 | EPIC-06 | e2e | `frontend/e2e/fila.spec.ts` | coberto |
| SCN-014.3 | RF-014 | EPIC-06 | integração | `internal/tempo/FilaPessoalIT.java` | coberto |
| SCN-015.1 | RF-015 | EPIC-06 | integração | `internal/tempo/AndamentoIT.java` | coberto |
| SCN-015.2 | RF-015 | EPIC-06 | e2e | `frontend/e2e/somente-leitura.spec.ts` | coberto |
| SCN-015.3 | RF-015 | EPIC-06 | integração | `internal/tempo/AndamentoIT.java` | coberto |
| SCN-016.1 | RF-016 | EPIC-06 | integração | `internal/tempo/TempoPorEtapaIT.java` | coberto |
| SCN-016.2 | RF-016 | EPIC-06 | integração | `internal/tempo/TempoPorEtapaIT.java` + `TempoPorEtapa.test.tsx` | coberto |
| SCN-016.3 | RF-016 | EPIC-06 | integração | `internal/tempo/TempoPorEtapaIT.java` | coberto |
| SCN-017.1 | RF-017 | EPIC-07 | integração | `internal/projeto/ConfiguracaoDoFluxoIT.java` | coberto |
| SCN-017.2 | RF-017 | EPIC-07 | unitário | `internal/projeto/EtapaServiceTest.java` + `ConfiguracaoDoFluxoIT.java` | coberto |
| SCN-017.3 | RF-017 | EPIC-07 | integração | `internal/projeto/ConfiguracaoDoFluxoIT.java` | coberto |
| SCN-018.1 | RF-018 | EPIC-07 | integração | `internal/projeto/RaiasIT.java` | coberto |
| SCN-018.2 | RF-018 | EPIC-07 | integração | `internal/projeto/RaiasIT.java` | coberto |
| SCN-018.3 | RF-018 | EPIC-07 | integração | `internal/tempo/TempoPorEtapaIT.java` | coberto |
| SCN-019.1 | RF-019 | EPIC-07 | integração | `internal/projeto/ParticipacaoIT.java` | coberto |
| SCN-019.2 | RF-019 | EPIC-07 | integração | `internal/projeto/ParticipacaoIT.java` | coberto |
| SCN-019.3 | RF-019 | EPIC-07 | integração | `internal/projeto/ParticipacaoIT.java` | coberto |
| SCN-019.4 | RF-019 | EPIC-08 | e2e | `frontend/e2e/tempo-real.spec.ts` | coberto |
| SCN-020.1 | RF-020 | EPIC-08 | e2e | `frontend/e2e/tempo-real.spec.ts` | coberto |
| SCN-020.2 | RF-020 | EPIC-08 | e2e | `frontend/e2e/tempo-real.spec.ts` | coberto |
| SCN-020.3 | RF-020 | EPIC-08 | e2e | `frontend/e2e/tempo-real.spec.ts` | coberto |
| SCN-021.1 | RF-021 | EPIC-01 | integração | `internal/acesso/AdminGlobalIT.java` | coberto |
| SCN-021.2 | RF-021 | EPIC-01 | integração | `internal/acesso/AdminGlobalIT.java` | coberto |
| SCN-021.3 | RF-021 | EPIC-01 | integração | `internal/acesso/AdminGlobalIT.java` | coberto |
| SCN-022.1 | RF-022 | EPIC-01 | integração | `internal/projeto/CriacaoDeProjetoIT.java` | coberto |
| SCN-022.2 | RF-022 | EPIC-01 | integração | `internal/projeto/CriacaoDeProjetoIT.java` | coberto |
| SCN-022.3 | RF-022 | EPIC-02 | integração | `internal/tarefa/CriacaoDeTarefaIT.java` | coberto |

Cobertura: 69/70. Cenário sem teste: SCN-002.4, e um só. Teste de cenário sem cenário de
origem: zero — o que a especificação obriga sem cenário está na seção própria,
em pacote separado.

---

## Estratégia

- **Framework:** JUnit 5 + AssertJ + Mockito (backend); Jest + Testing Library
  (frontend); Playwright (E2E); k6 (carga)
- **Runner:** Maven Surefire para unitário e Failsafe para integração, com
  Testcontainers; `next test` para o frontend; `playwright test` para E2E
- **Cobertura mínima exigida:** 80% de linhas por JaCoCo, conforme a coleção
  `backend/java`; a marca é piso de gate e não meta de qualidade

### Ferramentas, e por que cada uma

| Camada | Ferramenta | Razão |
| --- | --- | --- |
| Unitário backend | JUnit 5 + Mockito, sem contexto Spring | Os 5 cenários tipados assim verificam decisão de regra isolada; subir contexto os tornaria lentos sem verificar mais |
| Integração backend | Testcontainers PostgreSQL + MockMvc + `jwt()` | H2 foi recusado por SDR-003: ele não implementa `LISTEN/NOTIFY`, e um teste verde sobre ele teria aparência de prova sobre o mecanismo menos verificado do sistema |
| Componente frontend | Testing Library + addon de acessibilidade | Três cenários têm afirmação de apresentação que o contrato não alcança |
| E2E | Playwright | Os 9 cenários tipados exigem navegador real; quatro deles exigem **duas sessões simultâneas** no mesmo board, que não existe sem segundo contexto de navegador |
| Carga | k6 | Os envelopes de RNF-001 e RNF-009 são de tempo sob concorrência, e não de correção |
| Cobertura | JaCoCo, piso 80% | Da coleção `backend/java` |

### Montagem de cenário

A montagem passa **pela API** para tudo que o produto sabe fazer: etapas,
raias, tarefas, movimentos, tomadas, impedimentos. Construir por dentro seria
mais rápido e deixaria a suíte cega a mudança de contrato — o defeito que
importa é justamente a rota que muda sem que a prova perceba.

A exceção — projeto e primeira participação por SQL — está declarada na seção
de desvios e escrita no próprio código de suporte, para que ninguém a tome por
conveniência. Desde a emenda v1.3 ela deixou de ser contorno de lacuna e passou
a ser escolha de forma de fixture: a rota existe, é verificada por
`CriacaoDeProjetoIT`, e semear por ela tornaria inalcançáveis estados que
cenários congelados exigem.

### Dependências simuladas

| Dependência | Motivo | Como |
| --- | --- | --- |
| Provedor de identidade (integração) | O autoconfigure do OAuth2 resolve o issuer OIDC **eagerly na subida do contexto**: sem alguém respondendo na URL configurada, teste algum que sobe contexto completo inicializa. Também é o que permite SCN-001.3 exercitar a indisponibilidade | WireMock em `ProvedorSimulado` |
| Autenticação (integração) | O token carrega identidade e não carrega permissão, porque por ADR-003 o provedor autentica e as permissões são modeladas na aplicação por projeto. Conceder papel por claim verificaria um desenho que não é o deste sistema | `jwt()` do spring-security-test |
| Assinatura (topologia) | As instâncias do arnês falam por HTTP e WebSocket reais; ali o token precisa ser verificável de verdade | `TokenDeTeste`, par efêmero e JWKS local |
| Banco | Nunca simulado: metade das garantias deste sistema é propriedade de esquema | Testcontainers, imagem por variável |
| Relógio | Simular relógio esconderia erro de fuso e de precisão na própria gravação | Não simulado; `recuarInicioDoIntervalo` recua o início gravado |

O provedor de identidade **não** é simulado nos cenários `e2e`: ali sobe o
provedor de verdade, porque SCN-001.1 verifica o redirecionamento de ida e
volta, e injetar token no armazenamento local removeria do cenário exatamente
o que ele prova.

---

## Testes além dos cenários

Nove verificações que a especificação obriga e que cenário algum descreve.
Vivem em `br.com.idsd.kanban.alem` e em `backend/src/test/carga`, separadas de
propósito: elas não são cobertura de cenário, e misturá-las faria a contagem
de 70 parecer maior do que é.

| Teste | Origem | O que se perderia sem ele |
| --- | --- | --- |
| `ReconstrucaoDaProjecaoIT` | SDR-001 | A separação entre verdade e leitura só vale se a projeção for descartável. Uma escrita que altere a projeção sem gravar o evento passa em todo cenário, porque todo cenário lê a projeção |
| `ImutabilidadeDoLogIT` | RNF-008 | Ausência de rota não é imutabilidade. A garantia é verificada na role que a aplicação usa, com a contraparte de que a projeção continua gravável — sem ela, uma role somente-leitura passaria |
| `AusenciaDeRecortePorPessoaIT` | RN-014 | Superfície se conserta com um commit. Verifica que a projeção não tem coluna de pessoa e que visão alguma cruza projeção com usuário — a visão é a porta dos fundos |
| `SeqSobConcorrenciaIT` | SDR-004 | Duplicata destrói a detecção de lacuna no cliente; buraco faz o cliente concluir perda que não houve. Cenário algum descreve escrita simultânea |
| `OrtogonalidadeDasDimensoesIT` | RN-002 | Os cenários verificam a ortogonalidade um par por vez. Este verifica a propriedade no esquema, que é onde ela se sustenta |
| `BroadcastMultiInstanciaIT` | RNF-002 | 3 instâncias, 300 sessões. Suíte de uma instância só passa em verde sobre o desenho que ADR-004 existe para resolver |
| `rnf-001-tempo-real.js` | RNF-001 | 100 tarefas, 50 sessões; mede do aceite da escrita até a chegada à sessão que observa, e não a latência do próprio clique |
| `ExistenciaECapacidadeIT` | TechSpec v1.8 (regra única `403`/`404`) | Os cenários cobrem as duas pontas — quem participa e lê, quem não participa e recebe `404` — e deixam de fora o meio: o participante sem papel. Trocar o `403` dele por `404`, ou removê-lo deixando `200`, não deixava teste algum vermelho. Origem: ACH-07 da revisão de TASK-01.5 |
| `rnf-009-consultas.js` | RNF-009 | 12 meses, 5.000 tarefas. A massa é semeada por `massa-12-meses.sql`, com a contrapartida declarada no próprio arquivo |

---

## Execução inicial (Red)

**Não executada.** Registrada como pendente, com a causa nomeada.

A suíte não compila nem roda hoje porque o projeto ainda não existe: `pom.xml`,
`package.json`, `compose.yaml` e a configuração de Playwright e k6 nascem em
TASK-01.1 e TASK-01.2, cujo dono é o `/implement`. Executar aqui exigiria
escrever esses arquivos, e isso é implementação.

A tabela abaixo registra o que **se espera** da primeira execução, e não uma
medição feita. A coluna de falhas é previsão declarada; ela é substituída pelo
número medido quando a execução acontecer, e a substituição é a primeira
obrigação da primeira task que o `/implement` fechar.

| Suíte | Total de testes | Falhando (esperado) | Motivo da falha | Condição de execução |
| --- | --- | --- | --- | --- |
| Unitário backend | 22 | 22 | classes de serviço ainda não existem | após TASK-01.1 |
| Integração backend | 94 | 94 | contexto não sobe: aplicação e esquema ainda não existem | após TASK-01.1 |
| Além dos cenários (backend) | 12 | 12 | esquema, projeção e publicador ainda não existem | após TASK-01.1 |
| Componente frontend | 10 | 10 | componentes ainda não existem | após TASK-01.2 |
| E2E | 14 | 14 | aplicação não sobe: ambiente ainda não existe | após TASK-01.2 e o ambiente |
| Carga | 2 | 2 | ambiente ainda não existe; são gate de RNF e não de Red | após o ambiente |

A primeira execução é obrigação da primeira task que o `/implement` fechar, e a
saída dela entra aqui. O resultado esperado é **falha por ausência de
implementação** — falha por erro de compilação da própria suíte, por ausência
de tabela ou por exceção de configuração indica defeito no plano, e não
progresso.

Regra que fica valendo: teste de cenário que passar em verde antes de existir
implementação está verificando outra coisa, e precisa ser corrigido antes de a
task correspondente ser aberta.

---

## Congelamento da suíte

A partir deste ponto a suíte não é alterada pela implementação.

Alteração legítima exige uma de duas coisas, e as duas ficam registradas aqui:
emenda de cenário no PRD, com o ID preservado; ou desvio aprovado por revisor
humano. Ajuste feito para o teste passar, sem uma dessas duas coisas, é o
defeito que este congelamento existe para impedir.

| Data | Item | Tipo | Motivo | Aprovador |
| --- | --- | --- | --- | --- |
| 2026-09-10 | Suíte congelada na v1.0 — 66 cenários, 8 verificações além dos cenários | congelamento inicial | fecho da etapa `/tests`; a partir daqui a suíte só muda por emenda de cenário ou desvio aprovado | agente `/tests` (congelamento inicial não é alteração e não exige aprovação humana) |
| 2026-09-10 | Correção de SCN-021.1 (verificava rota inexistente; passa a verificar o log auditável) e de SCN-001.3 (era inalcançável; passa a exercitar o decodificador), mais o suporte comum: `jwk-set-uri` do provedor simulado, isolamento entre testes e troca do artefato do WireMock | correção de defeito da suíte | os dois cenários verificavam outra coisa que não o que o Gherkin congelado afirma. Nenhum `.feature` tocado, nenhum ID alterado, nenhuma asserção afrouxada — SCN-021.1 continua exigindo "quem e quando", agora onde o registro de fato vive. **Não independente**: ver a declaração de independência | Thiago Goncalves Cavalcante (autorizou a correção; a natureza não independente foi declarada antes da execução) |
| 2026-09-10 | Correção das asserções de SCN-002.1 (`[*]` no caminho com filtro, positiva antes da negativa) e da ordem de `AdminGlobalIT` em SCN-021.2; SCN-002.4 deixa de ser declarado coberto; `ExistenciaECapacidadeIT` acrescentado além dos cenários | correção de defeito da suíte | achados ACH-01, ACH-04, ACH-05, ACH-06 e ACH-07 da revisão de TASK-01.5: duas asserções não verificavam o que afirmavam e uma cobertura era declarada sem teste. Nenhum `.feature` tocado, nenhum ID alterado, nenhuma asserção afrouxada. **Não independente**: ver a declaração de independência | Thiago Goncalves Cavalcante (autorizou a correção dos achados; a natureza não independente foi declarada antes da execução) |
| 2026-09-10 | Acréscimo de SCN-022.1, SCN-022.2 e SCN-022.3 — `CriacaoDeProjetoIT` criado, um teste novo em `CriacaoDeTarefaIT`, suporte E2E migrado para a rota real | emenda de cenário no PRD | emenda v1.3 do PRD, que criou RF-022 e fechou a lacuna de especificação registrada nesta etapa. Nenhum cenário preexistente teve ID, redação ou teste alterados | Thiago Goncalves Cavalcante (aprovador do gate de spec na reconfirmação da emenda v1.3) |

---

## Desvios declarados nesta versão

| # | Desvio | Razão | Dono |
| --- | --- | --- | --- |
| D-01 | Os arquivos `.feature` do PRD **não** são duplicados em `features/kanban-tarefas/` | A ferramenta escolhida é JUnit/Jest/Playwright, e não Cucumber. Copiar os 21 arquivos criaria duas versões do mesmo cenário, que é a divergência que o congelamento guarda. Cada teste carrega o ID do cenário no nome que o relatório exibe, e a rastreabilidade fica pela tabela de cobertura acima | `/tests` |
| D-02 | Projeto e primeira participação são semeados por SQL direto na suíte de integração | Forma de fixture, e não ausência de rota: `POST /v1/projetos` existe desde a emenda v1.3 e é verificado por `CriacaoDeProjetoIT`. A rota **sempre** cria uma primeira `project_admin`, e vários cenários congelados exigem um conjunto de participantes exatamente igual ao declarado — inclusive projeto em que o sujeito é só `gestor`, e projeto sem participação nenhuma, que SCN-002.2 exige. Semear pela rota tornaria esses estados inalcançáveis | `/tests` |
| D-03 | O pacote raiz é fixado em `br.com.idsd.kanban` | As tasks usam marcador de posição; a suíte precisa de um nome concreto | TASK-01.1 |
| D-05 | A correção de 2026-09-10 alterou `backend/pom.xml`, que é arquivo da TASK-01.1 e não da suíte | A troca de `wiremock` por `wiremock-standalone` é a única forma de o provedor simulado subir: o artefato anterior espera Jetty 11 contra o Jetty 12 do BOM, e a falha derruba o contexto de todo teste de integração. Alteração restrita à declaração de uma dependência de escopo `test` | `/tests`, com efeito em TASK-01.1 |

---

## Lacuna de especificação encontrada — fechada em 2026-09-10

**Não existia rota de criação de projeto.** Contrato algum a declarava, e
requisito algum a cobria; a primeira participação também não podia ser
concedida por ninguém, porque ainda não havia ninguém dentro do projeto para
conceder. Um sistema recém-instalado não saía do zero por meios próprios, e o
admin global de ADR-010 alcançava projetos que ninguém conseguia criar.

**Fechada pela emenda v1.3 do PRD:** RF-022 e as regras RN-036 (criar projeto é
capacidade exclusiva da administração global, porque papel de projeto nenhum
existe antes do projeto), RN-037 (a criação nomeia a primeira `project_admin`
na mesma operação, e quem cria não vira participante) e RN-038 (o projeto nasce
sem fluxo). Contrato em
`docs/techspec/kanban-tarefas/contracts/sessao-e-projetos.md`, implementação em
TASK-01.8, verificação em SCN-022.1 a SCN-022.3.

Custo assumido pelo demandante ao recusar o fluxo padrão: o caminho de partida
tem **dois passos obrigatórios** — criar o projeto e configurar o fluxo — e nada
no primeiro lembra o segundo. SCN-022.3 existe para que essa consequência seja
verificada, e não apenas prevista.

---

## Fora deste artefato — regras negativas

- **Não** contém código de produção, nem esboço para a suíte compilar. Esboço
  necessário é teste mal desenhado ou contrato incompleto, e nos dois casos a
  saída é devolver.
- **Não** decide como implementar. Onde a suíte fixou costura interna, isso está
  declarado como restrição de task e não como desenho.
- **Não** altera cenário congelado, ID de cenário nem redação de critério de
  aceite. Divergência entre cenário e contrato vira devolução, e não ajuste.
- **Não** reabre decisão de PRD, TechSpec ou Decision Record. A lacuna de
  criação de projeto foi registrada, e não resolvida aqui.
- **Não** cria requisito. O que a especificação obriga sem cenário está na seção
  própria, apontando para a RNF ou o DR que o obriga.
- **Não** substitui o gate de RNF. As cargas estão escritas; medi-las é
  atividade de quem tiver o ambiente, e o resultado entra no `/evidence`.
