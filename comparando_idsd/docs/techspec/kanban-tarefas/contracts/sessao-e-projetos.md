# Contrato — sessão e projetos

_Cobre RF-001, RF-002, RF-017, RF-018, RF-019, RF-022 | Base: `/v1`_
_Normas: `_shared/api-standards.md`, `_shared/api-security.md`, BDR-001, BDR-002_

Todas as rotas exigem `Authorization: Bearer <jwt>` emitido pelo realm confiável.
Rota não mapeada nega por padrão. Erro sempre em `application/problem+json`.

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
- **Erros:** `401` token ausente, inválido ou expirado. `503` com `Retry-After`
  quando o provedor de identidade está indisponível e o JWKS não pode ser
  validado — SCN-001.3 exige recusa sem oferta de caminho alternativo, e `503`
  distingue indisponibilidade de credencial ruim.

## `GET /v1/projetos` — os projetos em que participo

Serve RF-002. Retorna **somente** projetos com participação; ausência de
participação devolve lista vazia, nunca a relação completa do sistema.

- **Saída `200`:** `{ conteudo: [ { id, nome, descricao, papeis: [], permissoes: [], fluxoConfigurado } ], totalElements, totalPages }`.
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

- **Erros:** `404` quando não há participação. **Não** `403` — SCN-002.3 exige que
  nenhum dado do projeto seja revelado, e `403` já revelaria sua existência.
  `api-standards.md` §2 prevê exatamente esse uso de `404`.

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
  - `422` nome ausente ou em branco; `422` `primeiroAdministradorId` ausente ou
    sem `usuario` correspondente, com `detail` dizendo que a pessoa precisa ter
    entrado ao menos uma vez no sistema.
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

Todas as rotas deste e dos demais contratos estão sob throttling por sujeito e
por origem, com `429` e `Retry-After` em `problem+json`
(`_shared/api-security.md` §4). O envelope numérico é **RNF-010** — 120 leituras
e 30 escritas por minuto por sujeito —, replicado na TechSpec Seção 8. Ele deixou
de ser instituído aqui: INC-05 apontou que comportamento recusável pelo usuário
precisa de requisito que o sustente, e o PRD v1.1 o criou.
Pesa mais aqui do que num CRUD: cada escrita aceita dispara fan-out de `NOTIFY`
para todas as instâncias e todas as sessões, então o custo de uma requisição
abusiva é amplificado pelo desenho de ADR-004, e RNF-001 e RNF-002 não têm outra
proteção.
