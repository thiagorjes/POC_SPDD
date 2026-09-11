# TASK-01.8 — Criação de projeto pelo administrador global

- **Status:** concluída
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-01.5
- **Cenários cobertos:** SCN-022.1, SCN-022.2
- **Origem:** RF-022, RN-036, RN-037, RN-038, RN-035, ADR-010, BDR-001

#### Contexto

Sem esta rota o sistema recém-instalado não sai do zero: nenhum contrato criava
projeto, e a primeira participação não podia ser concedida porque não há
ninguém dentro do projeto para concedê-la. É a única rota do sistema cuja
autorização **não** consulta participação — não existe participação a consultar
antes de o projeto existir.

#### O que deve ser feito

- [x] Implementar `POST /v1/projetos`, exigindo alcance de administração global.
- [x] Gravar `projeto` (com `seq_atual = 0`) e a primeira `participacao` com
      papel `project_admin` **na mesma transação**.
- [x] **Não** inserir participação para quem cria.
- [x] Devolver `201` com `Location` e corpo com `etapas: []`.
- [x] Recusar com `403` quem não é administrador global, e com `422` nome em
      branco ou `primeiroAdministradorId` sem `usuario` correspondente.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/java/br/com/idsd/kanban/internal/projeto/ProjetoController.java` | alterar | acrescenta a rota de escrita às duas de leitura |
| `backend/src/main/java/br/com/idsd/kanban/internal/projeto/CriacaoDeProjeto.java` | criar | registro de entrada |
| `backend/src/main/java/br/com/idsd/kanban/internal/projeto/ProjetoServico.java` | criar | a transação única de projeto + participação |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

`POST /v1/projetos` — entrada:

```
{ "nome": "", "descricao": "", "primeiroAdministradorId": "" }
```

Saída `201`, com `Location: /v1/projetos/{id}`:

```
{ "id": "", "nome": "", "descricao": "", "etapas": [] }
```

- `primeiroAdministradorId` é o `id` de um `usuario` **já existente** — ele
  passa a existir na primeira entrada da pessoa, pelo autoprovisionamento da
  sessão.
- `etapas: []` é a resposta correta, não um estado transitório: o projeto nasce
  sem fluxo.

#### Guia técnico — pontos de atenção

- **Uma transação, não duas.** Se a participação for gravada fora da transação
  do projeto, a falha dela reproduz exatamente o projeto inalcançável que esta
  task existe para eliminar.
- **Quem cria não vira participante.** Inserir a participação do administrador
  global apagaria a marca `acessoPorAdministracaoGlobal` para ele nesse projeto,
  contra SCN-021.2, e confundiria alcance com participação.
- **`403`, não `404`.** Aqui a coleção é conhecida do chamador e não há
  existência a ocultar. O `404` do detalhe de projeto protege outro caso.
- **Sem broadcast.** Não há canal a que a criação pudesse ser publicada: a
  inscrição pressupõe o projeto.
- **O projeto criado ainda não aceita tarefa** (RN-038), e isso é decidido, não
  esquecido. A recusa é verificada em TASK-02.5, por SCN-022.3.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | Administrador global cria o projeto e a pessoa nomeada consta como `project_admin` | **cumprido, por outra via** — `PrimeiraParticipacaoIT.gravaExatamenteUmaParticipacao` verde, agora comparando o `usuario_id` gravado ao da pessoa nomeada e não só a quantidade (ACH-06). A consulta prevista é `GET /v1/projetos/{id}/participacoes`, que é rota de TASK-06.2 e não existe ainda; ver o achado no histórico |
| 2 | Quem criou não consta como participante | **cumprido** — `PrimeiraParticipacaoIT.criadorNaoViraParticipante` verde |
| 3 | Projeto nasce sem etapa | **cumprido** — `CriacaoDeProjetoIT.criaEnomeiaAPrimeiraAdministradora` verifica `etapas: []` e passa nessa asserção |
| 4 | `project_admin` de outro projeto recebe `403` ao tentar criar | **cumprido** — SCN-022.2 verde, incluindo a relação de projetos inalterada depois da tentativa |
| 5 | Falha na gravação da participação não deixa projeto órfão | **cumprido, e agora medido** — `TransacaoUnicaDeCriacaoIT.falhaNaParticipacaoDesfazOProjeto` força a falha **depois** da gravação do projeto e prova o poder de falha: com `@Transactional` removido do método o teste fica vermelho, e verde com ele. `PrimeiraParticipacaoIT.recusaNaoDeixaProjetoOrfao` continua verde, mas mede a recusa e não a atomicidade (ACH-02) |
| 6 | `422` com razão explícita para nome em branco e para pessoa inexistente | **cumprido** — `CriacaoDeProjetoIT.entradaInvalidaERecusadaComRazao` verde |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-10 | criação | Task derivada da emenda do PRD v1.3 (RF-022), que fechou a lacuna encontrada pela `/tests` |
| 2026-09-11 | tentativa 1 | **Red medido:** `CriacaoDeProjetoIT` 3/3 vermelhos, todos `expected:<201\|403\|422> but was:<405>` — a rota não existe. Implementados os três arquivos declarados |
| 2026-09-11 | tentativa 2 | Os dois testes que usam `adminGlobal()` continuavam em `403`: nenhum deles passa por `GET /v1/sessao` antes, e sem conta gravada não há alcance global. A rota passou a **garantir a conta pela mesma via da sessão**, chamando `SessaoService.entrar` — reimplementar a promoção aqui criaria a segunda fonte da regra que ADR-010 existe para ter uma só |
| 2026-09-11 | tentativa 3 | Verde no que é alcançável. `CriacaoDeProjetoIT` 2/3; a falha restante é o achado abaixo. Escrita `PrimeiraParticipacaoIT` (3/3) para medir os critérios 1, 2 e 5. Suíte inteira: **154 testes, 55 verdes / 99 vermelhos**, contra a linha de base de 151 (50/101) — 3 testes novos verdes e 2 que viraram, nenhuma regressão |
| 2026-09-11 | achado | **SCN-022.1 não pode ficar verde nesta task.** A última asserção lê `GET /v1/projetos/{id}/participacoes`, que é rota de TASK-06.2 (EPIC-06), e o cenário falha em `404` depois de já ter verificado o `201`, o `Location` e `etapas: []`. Mesma classe de assimetria registrada em TASK-01.3 e TASK-01.5 — cenário congelado alocado em épico que não pode executá-lo por inteiro. Dono `/tasks`. Os critérios que dependiam dessa leitura foram medidos por `PrimeiraParticipacaoIT`, em SQL, porque o que eles afirmam é o estado gravado e não o contrato de outra rota |
| 2026-09-11 | correção | **Os dois bloqueantes da revisão fechados.** ACH-01: `criar` passou a **ler** a conta antes de escrever — só provisiona quem ainda não tem registro —, e o javadoc trocou a afirmação falsa ("o `403` é decidido antes de qualquer escrita") pela fronteira verdadeira, que é nenhuma escrita do domínio de projeto antes do `403`; o único caminho em que a escrita precede a decisão (token de quem nunca entrou) ficou declarado e coberto por teste. `SessaoService.entrar` ganhou a dimensão `via`, porque a promoção auditada passou a ser alcançável por duas portas e o log não as distinguia. ACH-02: `alem/TransacaoUnicaDeCriacaoIT` força a falha **depois** da gravação do projeto, e o poder de falha foi provado removendo `@Transactional` em cópia descartável. ACH-06 fechado junto. Medido: `PrimeiraParticipacaoIT` 5/5, `TransacaoUnicaDeCriacaoIT` 1/1, `ErrosELimitesIT` 12/12, `SessaoEProjetosIT` 7/7, `ResolvedorDePermissaoTest` 20/20 — sem regressão |
| 2026-09-11 | achado | **Um arquivo de teste fora da tabela declarada, e uma correção em outro.** `alem/PrimeiraParticipacaoIT.java` é verificação além dos cenários, pelo motivo acima. E `alem/ErrosELimitesIT.metodoNaoSuportadoSaiEmProblemJson` escolhera `POST /v1/projetos` como método não suportado — verbo que passou a existir hoje, fazendo o teste medir corpo ausente (`400`) em vez de método; trocado por `DELETE`, que rota nenhuma do produto prevê. Nenhum `.feature` nem step definition tocado |
| 2026-09-11 | correção | **Os achados não bloqueantes resolvidos.** ACH-03: a fronteira passou a validar por anotação (`@Valid` no controlador, `@NotBlank`/`@NotNull` no registro de entrada); a verificação equivalente do serviço **fica**, e a duplicação é deliberada — a anotação protege o contrato HTTP e nomeia o campo em `errors` para a tela, a do serviço é a invariante de quem grava. ACH-04: a linha entre `400` e `422` foi decidida e **escrita no contrato** — `400` é corpo que não pôde ser lido (o que SCN-004.2 congela), `422` é corpo lido cujo conteúdo é recusado, inclusive valor que não converte; `TratadorDeErro` passou a mapear `MethodArgumentNotValidException` para `422` e a devolver `422` nomeando o campo quando a causa é `MismatchedInputException` com caminho. Isso paga adiantado por `ParticipacaoIT`, `CriacaoDeTarefaIT` e `ImpedimentoIT`, que já asseram `errors[*].campo`. ACH-07 registrado no javadoc de `Criado` com gatilho, e virou critério 9 de TASK-02.2. ACH-08 declarado no plano de verificação, que passou a inventariar `PrimeiraParticipacaoIT` e `TransacaoUnicaDeCriacaoIT` entre as verificações além dos cenários. ACH-09 virou critério 9 de TASK-06.2, com o custo declarado nos dois arquivos. Medido: **29 testes, 1 falha** — só a pré-existente `CriacaoDeProjetoIT:62`, que é o `404` da rota de TASK-06.2 |
