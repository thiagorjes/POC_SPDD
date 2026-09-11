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
| 1 | Administrador global cria o projeto e a pessoa nomeada consta como `project_admin` | **cumprido, por outra via** — `PrimeiraParticipacaoIT.gravaExatamenteUmaParticipacao` verde. A consulta prevista é `GET /v1/projetos/{id}/participacoes`, que é rota de TASK-06.2 e não existe ainda; ver o achado no histórico |
| 2 | Quem criou não consta como participante | **cumprido** — `PrimeiraParticipacaoIT.criadorNaoViraParticipante` verde |
| 3 | Projeto nasce sem etapa | **cumprido** — `CriacaoDeProjetoIT.criaEnomeiaAPrimeiraAdministradora` verifica `etapas: []` e passa nessa asserção |
| 4 | `project_admin` de outro projeto recebe `403` ao tentar criar | **cumprido** — SCN-022.2 verde, incluindo a relação de projetos inalterada depois da tentativa |
| 5 | Falha na gravação da participação não deixa projeto órfão | **cumprido** — `PrimeiraParticipacaoIT.recusaNaoDeixaProjetoOrfao` verde: `select count(*) from projeto` zera após o `422` |
| 6 | `422` com razão explícita para nome em branco e para pessoa inexistente | **cumprido** — `CriacaoDeProjetoIT.entradaInvalidaERecusadaComRazao` verde |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-10 | criação | Task derivada da emenda do PRD v1.3 (RF-022), que fechou a lacuna encontrada pela `/tests` |
| 2026-09-11 | tentativa 1 | **Red medido:** `CriacaoDeProjetoIT` 3/3 vermelhos, todos `expected:<201\|403\|422> but was:<405>` — a rota não existe. Implementados os três arquivos declarados |
| 2026-09-11 | tentativa 2 | Os dois testes que usam `adminGlobal()` continuavam em `403`: nenhum deles passa por `GET /v1/sessao` antes, e sem conta gravada não há alcance global. A rota passou a **garantir a conta pela mesma via da sessão**, chamando `SessaoService.entrar` — reimplementar a promoção aqui criaria a segunda fonte da regra que ADR-010 existe para ter uma só |
| 2026-09-11 | tentativa 3 | Verde no que é alcançável. `CriacaoDeProjetoIT` 2/3; a falha restante é o achado abaixo. Escrita `PrimeiraParticipacaoIT` (3/3) para medir os critérios 1, 2 e 5. Suíte inteira: **154 testes, 55 verdes / 99 vermelhos**, contra a linha de base de 151 (50/101) — 3 testes novos verdes e 2 que viraram, nenhuma regressão |
| 2026-09-11 | achado | **SCN-022.1 não pode ficar verde nesta task.** A última asserção lê `GET /v1/projetos/{id}/participacoes`, que é rota de TASK-06.2 (EPIC-06), e o cenário falha em `404` depois de já ter verificado o `201`, o `Location` e `etapas: []`. Mesma classe de assimetria registrada em TASK-01.3 e TASK-01.5 — cenário congelado alocado em épico que não pode executá-lo por inteiro. Dono `/tasks`. Os critérios que dependiam dessa leitura foram medidos por `PrimeiraParticipacaoIT`, em SQL, porque o que eles afirmam é o estado gravado e não o contrato de outra rota |
| 2026-09-11 | achado | **Um arquivo de teste fora da tabela declarada, e uma correção em outro.** `alem/PrimeiraParticipacaoIT.java` é verificação além dos cenários, pelo motivo acima. E `alem/ErrosELimitesIT.metodoNaoSuportadoSaiEmProblemJson` escolhera `POST /v1/projetos` como método não suportado — verbo que passou a existir hoje, fazendo o teste medir corpo ausente (`400`) em vez de método; trocado por `DELETE`, que rota nenhuma do produto prevê. Nenhum `.feature` nem step definition tocado |
