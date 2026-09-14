# Contrato — sessão e projetos

_Cobre RF-001, RF-002, RF-017, RF-018, RF-019, RF-022 | Base: `/v1`_
_Normas: `_shared/api-standards.md`, `_shared/api-security.md`, BDR-001, BDR-002_

Todas as rotas exigem `Authorization: Bearer <jwt>` emitido pelo realm confiável.
Rota não mapeada nega por padrão. Erro sempre em `application/problem+json`.

## `403` ou `404`: a regra é única em todo o produto

Decidida em 2026-09-10 (ACH-03 da revisão de TASK-01.3), vale para **toda** rota
de recurso de projeto, aqui e nos demais contratos. Os dois eixos são
independentes e não se misturam:

| Situação | Resposta |
| --- | --- |
| Não há `participacao` do sujeito no projeto | `404` |
| Há `participacao`, mas os papéis não concedem a permissão da rota | `403` |
| Há `participacao` sem papel algum, ou só com `user` | `403` |

**Participação é o eixo da existência; papel é o eixo da capacidade.** Quem
participa já sabe que o projeto existe — alguém o incluiu, e `GET /v1/projetos`
lista por participação e não por permissão, de modo que o projeto aparece na
relação uma rota antes. Responder `404` ali contradiria aquele `200` e tiraria da
pessoa a única informação acionável que ela tem: peça o papel a quem administra o
projeto. Do outro lado, `403` para quem não participa revelaria a existência de um
projeto que SCN-002.3 exige manter oculto.

### O corolário: `nome` é metadado da participação, o resto é dado protegido

Decidido em 2026-09-11 (ACH-08 da revisão de TASK-01.5). A regra acima só se
sustenta se o participante sem papel **vir o nome** do projeto na relação: é ele
que identifica o vínculo e é ele que torna o `403` acionável — sem o nome, a
recusa fala de um identificador opaco e a pessoa não tem o que pedir a quem
administra.

Isso obriga a separação, e ela vale para toda a superfície de leitura:

| Campo | Natureza | Quem vê |
| --- | --- | --- |
| `id`, `nome`, `papeis`, `permissoes`, `acessoPorAdministracaoGlobal`, `fluxoConfigurado` | metadado da participação | todo participante, com papel ou sem |
| `descricao` e todo o conteúdo do projeto | dado protegido por `LER` | somente quem tem a permissão |

`descricao` sai portanto do corpo de `GET /v1/projetos` e permanece apenas em
`GET /v1/projetos/{id}`, que é a rota governada por `LER`. Ela não identifica
nem é necessária para pedir papel: é o primeiro campo que carrega informação de
negócio, e mantê-la na relação entregaria a quem o detalhe recusa exatamente o
conteúdo que a recusa protege — o vazamento de SCN-002.3 na versão pequena.

A leitura alternativa, em que `nome` também seria protegido por `LER`, foi
considerada e **desmonta a regra da seção anterior**: obrigaria a relação a
exibir um item sem identificação, o que é inútil, ou a omitir o item — e um
projeto que não aparece na relação torna `404` a resposta coerente no detalhe,
que é o oposto do que esta seção decide.

A distinção depende do sinalizador `participa` que o resolvedor de permissão
devolve junto do conjunto de permissões. Derivá-la de conjunto vazio é o que a
regra proíbe: os dois casos produzem conjunto vazio e exigem respostas opostas.

O alcance de administração global (RN-035) precede os dois eixos: ele não
participa de projeto algum e ainda assim recebe `200`.

---

## `GET /v1/sessao` — quem sou eu e o que posso

Serve RF-001. Autoprovisiona o `usuario` na primeira entrada, a partir do `sub`,
`name` e `email` do token — não há cadastro local (ADR-006).

- **Entrada:** apenas o token.
- **Saída `200`:** `{ id, nome, email, adminGlobal }`.
- **Bootstrap do admin global (ADR-010, RF-021, RN-035):** a promoção casa o
  `sub` do token com a property configurada, **nunca o e-mail**, e só ocorre se
  ainda não existir nenhum admin global — a segunda tentativa pelo mesmo caminho
  não promove ninguém (SCN-021.1). Toda promoção é registrada em `WARN` com quem
  foi promovido e quando, e o registro é o histórico auditável que RN-035 exige.
  O e-mail é mutável
  no provedor e, em realm com autocadastro ou federação, atribuível por quem se
  registra — usá-lo como chave de promoção tornaria a escalada ao bypass
  universal uma única requisição.
- **Claim ausente (decisão de 2026-09-10):** token válido sem `email`
  autoprovisiona com `email` nulo; sem `name`, o nome recua para
  `preferred_username` e, faltando esse, para o próprio `sub`. A entrada **não** é
  recusada: o token é legítimo e o que falta é configuração do realm, que quem
  entra não tem como corrigir — e ADR-006 não deixa caminho alternativo. Claim
  ausente preserva o valor já gravado em vez de sobrescrevê-lo com nulo. Razões
  em `../data-model.md` §3.
- **Erros:** `401` token ausente, inválido ou expirado. `503` com `Retry-After`
  quando o provedor de identidade está indisponível e o JWKS não pode ser
  validado — SCN-001.3 exige recusa sem oferta de caminho alternativo, e `503`
  distingue indisponibilidade de credencial ruim.

## `GET /v1/projetos` — os projetos em que participo

Serve RF-002. Retorna **somente** projetos com participação; ausência de
participação devolve lista vazia, nunca a relação completa do sistema.

- **Saída `200`:** `{ conteudo: [ { id, nome, papeis: [], permissoes: [], acessoPorAdministracaoGlobal, fluxoConfigurado } ], totalElements, totalPages }`.
- **`descricao` não vem aqui.** Ela é dado protegido por `LER` e vive só no
  detalhe — ver o corolário da regra `403`/`404` acima.
- `fluxoConfigurado` é `false` enquanto o projeto não tiver etapa alguma, e é o
  que sustenta a marca de fluxo não configurado que RN-038 obriga (SCN-002.4).
  Deriva de `EXISTS (SELECT 1 FROM etapa WHERE projeto_id = ...)`, calculado no
  servidor e nunca inferido pelo cliente a partir de lista de etapas ausente da
  resposta.
- `permissoes` é derivado dos papéis no servidor. O cliente usa para não
  apresentar ação que não pode executar; a recusa real acontece no serviço
  (RNF-004, RN-015).
- SCN-002.2: lista vazia é `200` com `conteudo: []`, não `404`.
- **Admin global** recebe todos os projetos, cada um com
  `acessoPorAdministracaoGlobal: true` — o alcance é visível na resposta, não
  implícito (ADR-010, RF-021). A mesma marca acompanha `GET /projetos/{id}` e o
  board, e é ela que satisfaz "me é indicado que estou agindo pelo alcance de
  administração global" (SCN-021.2). Ele vê e age em projeto sem participação, e
  a checagem de participação de BDR-001 cede a esse alcance.
- **O alcance é de escopo, não de imunidade (RN-035, SCN-021.3).** Ele não
  contorna RN-014, que não tem o que contornar porque a coluna de pessoa não
  existe no esquema; não contorna RNF-008, garantido na role de banco, que só
  concede `SELECT, INSERT` sobre `evento_tarefa`; e nenhuma rota de alteração de
  tempo já contado existe para ele ou para qualquer outro perfil. As três
  garantias são estruturais — se dependessem de verificação por papel, o admin
  global seria justamente o papel que as dispensaria.

## `GET /v1/projetos/{projetoId}` — detalhe do projeto

- **Saída `200`:** `{ id, nome, descricao, papeis: [], permissoes: [], acessoPorAdministracaoGlobal }`.
  É aqui — e só aqui — que `descricao` aparece.
- **Erros:** `404` quando não há participação. **Não** `403` — SCN-002.3 exige que
  nenhum dado do projeto seja revelado, e `403` já revelaria sua existência.
  `api-standards.md` §2 prevê exatamente esse uso de `404`. `403` quando há
  participação e nenhum papel concede `LER`, pela regra única acima; o corpo da
  recusa é genérico e não repete nome nem descrição.

## `POST /v1/projetos` — criar projeto (RF-022)

Exige alcance de administração global. É a única rota do sistema cuja
autorização **não** consulta participação: não há participação a consultar antes
de o projeto existir.

- **Entrada:** `{ nome, descricao?, primeiroAdministradorId }`.
  `primeiroAdministradorId` é o `usuario.id` de quem passa a ser
  `project_admin`. A pessoa precisa já existir em `usuario` — ela existe a
  partir da primeira entrada, pelo autoprovisionamento de `GET /v1/sessao`.
- **Saída `201`** com `Location: /v1/projetos/{id}` e corpo `{ id, nome,
  descricao, etapas: [] }`. `etapas` vazio é a resposta correta e não um estado
  transitório: o projeto nasce sem fluxo (RN-038).
- **Efeito, numa única transação:** insere `projeto` com `seq_atual = 0` e a
  primeira `participacao` com papel `project_admin` (RN-037). Duas transações
  deixariam projeto inalcançável se a segunda falhasse, que é exatamente o
  estado que a lacuna original produzia.
- **Quem cria não vira participante** (RN-037). Nenhuma `participacao` é
  inserida para o administrador global: ele já alcança o projeto por RN-035, e
  inseri-la confundiria escopo com participação — a marca
  `acessoPorAdministracaoGlobal` deixaria de aparecer para ele nesse projeto,
  contra SCN-021.2.
- **Erros:**
  - `403` quando quem chama não é administrador global (SCN-022.2). Aqui é
    `403` e não `404`: a coleção `/v1/projetos` existe e é conhecida do
    chamador, então não há existência a ocultar — o `404` de
    `GET /projetos/{id}` protege a existência de um projeto específico, que é
    outro caso.
  - `422` nome ausente ou em branco; `422` `primeiroAdministradorId` ausente,
    **malformado** ou sem `usuario` correspondente, com `detail` dizendo que a
    pessoa precisa ter entrado ao menos uma vez no sistema.
  - **A linha entre `400` e `422`** vale para todo contrato do produto e ficou
    escrita aqui porque foi aqui que ela apareceu (ACH-04 da revisão de
    TASK-01.8): `400` é o corpo que **não pôde ser lido** — JSON quebrado ou
    ausente, que é o que SCN-004.2 congela; `422` é o corpo lido cujo **conteúdo
    é recusado**, inclusive quando o valor de um campo não converte para o tipo
    do contrato. Sem a linha, a mesma classe de erro saía com dois códigos
    conforme o desserializador conseguisse ou não ler o valor, e quem consome não
    tinha como saber qual esperar. Toda recusa de conteúdo nomeia os campos em
    `errors[].campo` (RNF-003).
- **Sem broadcast.** Não há canal a que a criação pudesse ser publicada: a
  inscrição em `/topic/projetos/{projetoId}` pressupõe o projeto.
- **Ordem de instalação, com o custo declarado (RN-038).** Criar o projeto não
  o deixa utilizável: `POST /projetos/{id}/tarefas` responde `422` enquanto não
  houver etapa (SCN-004.3, SCN-022.3). O caminho de partida tem dois passos
  obrigatórios — criar e configurar o fluxo —, e nada no primeiro lembra o
  segundo. A alternativa de nascer com fluxo padrão foi apresentada e recusada
  pelo demandante.

---

## Configuração de etapas — RF-017

Exige permissão de configuração. `403` para participante sem ela.

### `GET /v1/projetos/{projetoId}/etapas`

- **Saída `200`:** lista de `{ id, nome, ordem, terminal }`, ordenada, sem as
  arquivadas.

### `PUT /v1/projetos/{projetoId}/etapas`

Substitui o fluxo inteiro numa operação — a ordem é propriedade do conjunto, e
editar etapa a etapa permitiria estados intermediários sem etapa terminal.

- **Entrada:** `{ etapas: [ { id?, nome, ordem, terminal } ] }`. `id` ausente cria;
  `id` presente e omitido do corpo arquiva.
- **Saída `200`:** o fluxo resultante.
- **Erros:**
  - `422` quando nenhuma etapa é terminal (RN-001, SCN-017.2). O fluxo vigente não
    é alterado.
  - `422` quando uma etapa a arquivar contém tarefas ativas (RN-020, SCN-017.3),
    com `detail` informando que a etapa precisa ser esvaziada antes e `errors`
    identificando qual.
- Renomear preserva o `id` e não afeta o histórico (RN-021). Alteração vale dali
  em diante (RN-022).
- **Concorrência (SDR-005).** A entrada **não** carrega bloco `origem` — este
  contrato é a exceção nomeada ao envelope de escrita da Seção 4 da TechSpec, e a
  exceção existe porque a unidade de escrita é o conjunto e não uma linha que o
  cliente versionou. A serialização é do servidor: a transação bloqueia a linha de
  `projeto` antes de ler o fluxo vigente. Consequência visível ao cliente: duas
  configurações simultâneas não produzem `409` — a última vence inteira.
  - `503` com `Retry-After` quando a espera pelo bloqueio se esgota, e o
    `type` é `espera-por-bloqueio-esgotada`. Não é recusa do pedido e não pede
    recarregar a tela: outra configuração do mesmo projeto está em curso, e
    repetir a requisição em alguns segundos resolve. **O fluxo atual não foi
    alterado.** O teto existe porque bloqueio sem teto converte contenção em
    indisponibilidade — ver a emenda de SDR-005.

## Configuração de raias — RF-018

`GET` e `PUT /v1/projetos/{projetoId}/raias`, mesma forma e mesma permissão.
Raia não restringe transição e não entra em agregação (RN-023): nenhuma rota de
consulta agregada aceita filtro por raia, e é a ausência que SCN-018.3 verifica.

---

## Participação e permissões — RF-019

Exige permissão de configuração.

### `GET /v1/projetos/{projetoId}/participacoes`

- **Saída `200`:** `[ { id, usuario: { id, nome, email }, papeis: [] } ]`.

### `PUT /v1/projetos/{projetoId}/participacoes/{usuarioId}`

- **Entrada:** `{ papeis: [ "dev", "product_owner" ] }` — acumuláveis (BDR-001).
- **Saída `200`:** a participação resultante. Vale imediatamente (SCN-019.1).
- **Erros:** `422` papel fora do catálogo fechado.
- **Efeito no canal de eventos:** as inscrições STOMP da pessoa naquele projeto
  são invalidadas na mesma transação. "Vale imediatamente" precisa valer também
  para o que já está aberto, ou a perda de papel só surtiria efeito no `exp` do
  token.

### `DELETE /v1/projetos/{projetoId}/participacoes/{usuarioId}`

- **Saída `204`.**
- **Efeito obrigatório na mesma transação:** toda tarefa assumida pela pessoa no
  projeto volta a `AGUARDANDO_TOMADA` na etapa em que está, com evento
  `TAREFA_DEVOLVIDA` de `ator_id` nulo, e o registro de quem havia assumido é
  preservado no log (RN-027, SCN-019.2).
- Cada devolução emite `NOTIFY` como qualquer outra mudança de estado.
- **As sessões WebSocket da pessoa naquele projeto são derrubadas na mesma
  transação.** Sem isso, quem foi removido continua recebendo o board — títulos,
  responsáveis, motivos de impedimento — enquanto o socket estiver aberto.
  SCN-019.2 cobre a devolução das tarefas; a revogação do acompanhamento em tempo
  real passou a ter cenário congelado próprio, **SCN-019.4** (INC-06), e deixa de
  ser teste que a especificação obriga sem cenário de origem.

---

## Limite de requisições

Todas as rotas deste e dos demais contratos estão sob throttling **por sujeito
autenticado**, com `429` e `Retry-After` em `problem+json`
(`_shared/api-security.md` §4). O envelope numérico é **RNF-010** — 120 leituras
e 30 escritas por minuto por sujeito —, replicado na TechSpec Seção 8. Ele deixou
de ser instituído aqui: INC-05 apontou que comportamento recusável pelo usuário
precisa de requisito que o sustente, e o PRD v1.1 o criou.
Pesa mais aqui do que num CRUD: cada escrita aceita dispara fan-out de `NOTIFY`
para todas as instâncias e todas as sessões, então o custo de uma requisição
abusiva é amplificado pelo desenho de ADR-004, e RNF-001 e RNF-002 não têm outra
proteção.

**Uma dimensão só, e ela é o sujeito.** Houve uma segunda, por endereço de
origem, e ela foi removida na revisão de TASK-01.6 (ACH-01, ACH-05). Ela se
apoiava em premissa que o `docker/compose.yaml` desmente — não há proxy reverso
em produção, e o único nginx do repositório existe no arnês de broadcast de
`compose.test.yaml` —, de modo que `X-Forwarded-For` é escolhido pelo cliente:
trocá-lo a cada requisição zerava a contagem, e escolher o endereço alheio
queimava o envelope de terceiro. Mais decisivo que os dois: a condição de medição
de RNF-010 exige provar que **o consumo de um sujeito não afeta a resposta de
outro**, e envelope compartilhado por origem afirma o contrário. Conter rajada
anônima é trabalho da borda, e não há o que atribuir a ninguém antes de haver
sujeito.

**A contagem é por instância, e o desenho prevê três (RNF-002)** — o envelope
efetivo em produção é o número de RNF-010 multiplicado pelo número de réplicas.
Está declarado e não é defeito escondido: contador compartilhado exigiria Redis
ou tabela de contagem, e ADR-002 recusa armazenamento adicional nesta fase. A
condição que reabre a decisão é o envelope efetivo passar a importar — quando
houver medição de uso real que o confronte, que é o mesmo gatilho de revisão que
RNF-010 já carrega.
