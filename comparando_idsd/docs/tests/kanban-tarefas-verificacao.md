# Plano de Verificação — kanban-tarefas

_Versão 1.2 — 2026-09-10_

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

70 cenários, cobertos um a um. A distribuição por tipo é a do PRD: 9 `e2e`,
56 `integração`, 5 `unitário`.

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
| SCN-002.4 | RF-002 | EPIC-01 | integração | `internal/acesso/SessaoEProjetosIT.java` | coberto |
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

Cobertura: 70/70. Cenário sem teste: zero. Teste de cenário sem cenário de
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

Oito verificações que a especificação obriga e que cenário algum descreve.
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
| 2026-09-10 | Acréscimo de SCN-022.1, SCN-022.2 e SCN-022.3 — `CriacaoDeProjetoIT` criado, um teste novo em `CriacaoDeTarefaIT`, suporte E2E migrado para a rota real | emenda de cenário no PRD | emenda v1.3 do PRD, que criou RF-022 e fechou a lacuna de especificação registrada nesta etapa. Nenhum cenário preexistente teve ID, redação ou teste alterados | Thiago Goncalves Cavalcante (aprovador do gate de spec na reconfirmação da emenda v1.3) |

---

## Desvios declarados nesta versão

| # | Desvio | Razão | Dono |
| --- | --- | --- | --- |
| D-01 | Os arquivos `.feature` do PRD **não** são duplicados em `features/kanban-tarefas/` | A ferramenta escolhida é JUnit/Jest/Playwright, e não Cucumber. Copiar os 21 arquivos criaria duas versões do mesmo cenário, que é a divergência que o congelamento guarda. Cada teste carrega o ID do cenário no nome que o relatório exibe, e a rastreabilidade fica pela tabela de cobertura acima | `/tests` |
| D-02 | Projeto e primeira participação são semeados por SQL direto na suíte de integração | Forma de fixture, e não ausência de rota: `POST /v1/projetos` existe desde a emenda v1.3 e é verificado por `CriacaoDeProjetoIT`. A rota **sempre** cria uma primeira `project_admin`, e vários cenários congelados exigem um conjunto de participantes exatamente igual ao declarado — inclusive projeto em que o sujeito é só `gestor`, e projeto sem participação nenhuma, que SCN-002.2 exige. Semear pela rota tornaria esses estados inalcançáveis | `/tests` |
| D-03 | O pacote raiz é fixado em `br.com.idsd.kanban` | As tasks usam marcador de posição; a suíte precisa de um nome concreto | TASK-01.1 |

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
