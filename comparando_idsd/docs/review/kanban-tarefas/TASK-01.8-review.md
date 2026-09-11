# Revisão técnica — TASK-01.8 (revisão parcial de épico)

_Data: 2026-09-11 | Revisor: agente | Épico: EPIC-01 | PR: não há — trabalho direto em `v202609041`_
_Commits revisados: a1be014..317c2f8, mais a árvore de trabalho não commitada_

> O revisor não escreve código. Achado é devolvido a quem implementa; correção
> feita pelo revisor faz o revisor revisar a si mesmo.

---

## Escopo revisado

| Item | Valor |
| --- | --- |
| Tasks | TASK-01.8 |
| Cenários entregues | SCN-022.1, SCN-022.2 |
| Arquivos | 5 de código (3 declarados, 2 de teste fora da tabela) |
| Suíte | 154 testes, 99 falhando |

- **Cenários congelados alterados no período:** nenhum
- **Step definitions alterados no período:** nenhum

> Qualquer das duas linhas diferente de "nenhum" sem emenda registrada reprova
> o GATE-VERIFICACAO-INDEPENDENTE, e a revisão para aqui.

A Fase 0 teve linha de base real pela quinta revisão seguida: `git diff
8346946..HEAD` sobre `docs/prd/kanban-tarefas/*.feature` e sobre
`backend/src/test` devolve apenas acréscimo na árvore de teste, e nenhuma
alteração de cenário congelado.

A medição foi **refeita aqui** e confere com a declarada nos dois sentidos:
154 testes, 55 verdes e 99 vermelhos, com `CriacaoDeProjetoIT` em 2/3,
`PrimeiraParticipacaoIT` em 3/3 e `ErrosELimitesIT` em 12/12. Ela só é
obtenível excluindo cinco arquivos de teste de tasks posteriores, que não
compilam: `mvn verify` — o verificador que o critério 1 da TASK-01.1 nomeia —
para em `test-compile`. O fato já está registrado como assimetria de plano com
dono `/tasks`, e não é achado novo desta task; a exclusão foi feita em cópia
descartável fora do repositório, sem tocar a árvore de trabalho.

---

## Conformidade com a especificação

| Cenário | Passa | Implementado conforme a task | Observação |
| --- | --- | --- | --- |
| SCN-022.1 | não | sim | Falha em `404` na última asserção, que lê `GET /v1/projetos/{id}/participacoes` — rota de TASK-06.2. O `201`, o `Location` e `etapas: []` já foram verificados antes. Ver ACH-09 |
| SCN-022.2 | sim | sim | A recusa é `403` com corpo `problem+json` e `detail` genérico. A asserção de "nada foi criado" é fraca — ver ACH-08 |

- **Escopo além do especificado:** nenhum no código de produção. Em teste, dois
  arquivos fora da tabela declarada: `alem/PrimeiraParticipacaoIT.java`
  (verificação além dos cenários, declarada) e a correção em
  `alem/ErrosELimitesIT.java`. Esta segunda carrega, sem declaração, cinco
  alterações de teste que pertencem à correção de TASK-01.6 — ver ACH-05.

---

## Envelopes de RNF

| RNF | Envelope | Medido | Como | Situação |
| --- | --- | --- | --- | --- |
| RNF-004 | nenhuma escrita se completa com verificação feita apenas do lado do cliente | recusa efetiva | SCN-022.2 verde; `ProjetoController.java:183-187` lê `adminGlobal` do registro gravado e nunca de claim | dentro |
| RNF-010 | 120 leituras e 30 escritas por minuto por sujeito | 12/12 em `ErrosELimitesIT` | suíte medida nesta revisão | dentro — herdado de TASK-01.6, e a rota nova não abre exceção ao limite |
| RNF-002 | três instâncias simultâneas | não medido | `BroadcastMultiInstanciaIT` em erro, depende de EPIC-03 | não medido |
| RNF-009 | p95 das consultas | não medido | não há instrumentação antes do fechamento do épico | não medido |

> RNF não medido não passa por omissão. Se a instrumentação não existe, o
> GATE-NFR reprova — a ausência de medição é o achado.

Esta é a primeira rota de escrita do produto, e por isso o primeiro ponto em
que RNF-004 é mensurável de fato: SCN-022.2 é exatamente a requisição forjada
por quem não tem o alcance. Os demais envelopes seguem não mensuráveis antes do
fechamento do EPIC-01, que é a razão pela qual o GATE-NFR vem reprovado em toda
revisão parcial deste épico.

---

## Achados

| ID | Severidade | Tipo | Local | Descrição | Destino |
| --- | --- | --- | --- | --- | --- |
| ACH-01 | bloqueante | segurança | `backend/src/main/java/br/com/idsd/kanban/internal/projeto/ProjetoController.java:104` | O javadoc da rota institui, em linhas 93-95, que "o 403 é decidido antes de qualquer escrita", e a primeira coisa que a rota faz é chamar `entrar(...)`, que delega a `SessaoService.entrar` — método transacional que provisiona a conta, espelha nome e e-mail e pode promover a administração global. A escrita, portanto, precede a decisão: requisição recusada com `403` deixa efeito gravado em disco, e o caminho recusado não tem teste algum sobre esse efeito. Some-se que a promoção auditada em `SessaoService` registra quem foi promovido e não por qual porta, de modo que a promoção passa a ser alcançável por uma rota que não é a da sessão sem que o log distinga | /implement |
| ACH-02 | bloqueante | código | `backend/src/test/java/br/com/idsd/kanban/alem/PrimeiraParticipacaoIT.java:80` | O critério de aceite 5 — "falha na gravação da participação não deixa projeto órfão" — está marcado cumprido, e o teste que o mede não tem poder de falha. Em `ProjetoServico.java:64-66` a verificação da pessoa nomeada **precede** o primeiro `save`, então no caminho exercitado nada chega a ser gravado e a asserção `select count(*) from projeto` zeraria igualmente com `@Transactional` removido do método. A invariante que dá nome à task — uma transação, não duas — é a única coisa que a task existe para garantir, e é a única que nenhum teste verifica | /implement |
| ACH-03 | relevante | código | `backend/src/main/java/br/com/idsd/kanban/internal/projeto/ProjetoController.java:106` | `@RequestBody` sem `@Valid`, e não há uma única anotação de validação em todo `backend/src` embora `spring-boot-starter-validation` esteja declarado em `backend/pom.xml:52`. A validação de fronteira vive inteira dentro do serviço, por disciplina de quem escreveu; a próxima rota de escrita copia o padrão, e o dia em que alguém esquecer não haverá nada falhando | /implement |
| ACH-04 | relevante | spec | `backend/src/main/java/br/com/idsd/kanban/shared/TratadorDeErro.java:146` | `primeiroAdministradorId` sintaticamente malformado sai em `400` pelo tratamento de corpo ilegível, e o contrato de `POST /v1/projetos` prescreve `422` para primeiro administrador inválido sem distinguir malformado de inexistente. Quem consome recebe dois códigos para a mesma classe de erro de entrada, e o contrato não diz qual é o certo | /techspec |
| ACH-05 | relevante | código | commit `317c2f8` | O commit da task carrega cinco alterações de teste que pertencem à correção de TASK-01.6 — envelope de origem, `traceId` no log e corpo enriquecido pelo `ResponseBodyAdvice` — enquanto o código de produção que as torna verdes segue **não commitado** na árvore de trabalho. Em `317c2f8` os testes existem sem o mecanismo que eles medem, e o histórico da task declara apenas a troca de verbo em um teste. Fase 0 de revisão futura passa a comparar contra uma árvore que nunca foi verde | /implement |
| ACH-06 | relevante | código | `backend/src/test/java/br/com/idsd/kanban/alem/PrimeiraParticipacaoIT.java:42` | O critério de aceite 1 afirma que "a pessoa nomeada consta como `project_admin`", e o teste que o mede lê `usuario_id` na consulta e nunca o compara ao identificador de quem foi nomeado: ele afirma que existe **uma** participação, não que é a dela. A comparação existe no teste vizinho, por outro caminho, o que torna o buraco fácil de não ver | /implement |
| ACH-07 | menor | código | `backend/src/main/java/br/com/idsd/kanban/internal/projeto/CriacaoDeProjeto.java:32` | A fábrica da saída devolve `List.of()` constante para `etapas`, de modo que a asserção `etapas: []` do critério 3 não tem poder de falha e continuará verde depois que etapas existirem, no EPIC-02. O valor é o correto hoje; o que não existe é a verificação | /implement |
| ACH-08 | menor | spec | `backend/src/test/java/br/com/idsd/kanban/internal/projeto/CriacaoDeProjetoIT.java:99` | A asserção de SCN-022.2 que afirma "nenhum projeto é criado" lê a relação de projetos do próprio sujeito recusado, que não veria o projeto nem se ele tivesse sido criado — o alcance dele não o alcançaria. A asserção é verdadeira por construção e não distingue os dois desfechos | /tests |
| ACH-09 | relevante | spec | `docs/tasks/kanban-tarefas/TASK-01.8-criacao-de-projeto.md:8` | SCN-022.1 é cenário do escopo da task e não pode ficar verde dentro dela: a última asserção depende de `GET /v1/projetos/{id}/participacoes`, rota de TASK-06.2. Terceira ocorrência da mesma assimetria, depois de TASK-01.3 e TASK-01.5 — cenário congelado alocado em épico que não pode executá-lo por inteiro. Registrado pela própria task; consta aqui porque é o cenário do escopo que não passa | /tasks |

**Severidade:**

- **bloqueante** — impede o merge. Correção obrigatória antes do gate.
- **relevante** — merge permitido com registro; entra no backlog com prazo.
- **menor** — registrado, sem prazo.

Nenhum achado de segurança foi rebaixado nesta revisão, e ACH-03 merece nota de
método. Ele chegou dos agentes tipado como segurança e bloqueante, e foi
**retipado como código**, não rebaixado dentro do tipo: a verificação foi
refeita arquivo a arquivo e nenhuma entrada inválida atravessa hoje — nome em
branco e pessoa inexistente são recusados no serviço, e o único desvio de
código de resposta está isolado em ACH-04. O que existe é dívida de padrão e de
durabilidade, e chamá-la de vulnerabilidade tornaria a palavra inútil na
próxima vez. O único achado de segurança da revisão, ACH-01, está bloqueante.

---

## Análise de segurança

| Verificação | Resultado | Evidência |
| --- | --- | --- |
| Entrada validada nas fronteiras | achado | ACH-03 e ACH-04 — validação inteiramente dentro do serviço, e entrada malformada sai com código diferente do contratado |
| Autorização verificada por operação | achado | ACH-01 — a checagem de alcance global está correta e é lida do registro gravado, mas ocorre depois de uma escrita que o javadoc afirma não existir |
| Segredo fora do código e do log | ok | nenhum literal de credencial nos arquivos da task; a senha do banco segue por `configtree` |
| Dado sensível fora de log e mensagem de erro | ok | o `detail` do `403` é genérico e não nomeia projeto nem pessoa; o `422` da pessoa inexistente não devolve identificador |
| Dependência nova sem vulnerabilidade conhecida | n/a | a task não acrescentou dependência |

---

## Guardrails extraídos

Regras que esta revisão descobriu e que devem valer para as próximas.

| Guardrail | Origem | Onde passa a valer |
| --- | --- | --- |
| Javadoc que afirma ordem em caminho de autorização — o que acontece antes de quê — é afirmação verificável e exige teste do caminho recusado, não só do concedido | ACH-01 | requirements/guidelines/backend/java/coding-standards.md |
| Rota que garante conta por efeito colateral declara isso na assinatura do que chama, e o efeito no caminho recusado é medido | ACH-01 | requirements/guidelines/backend/java/api-standards.md |
| Critério de aceite que afirma atomicidade só está cumprido por teste cuja falha seja alcançável: a falha exercitada precisa ocorrer **depois** da primeira gravação | ACH-02 | requirements/guidelines/backend/java/testing.md |
| Asserção de "nada foi criado" precisa ler de uma fonte que enxergaria o objeto criado; ler pelo alcance do sujeito recusado é verde por construção | ACH-08 | requirements/guidelines/backend/java/testing.md |
| Commit de task carrega a task: código de produção e o teste que o mede vão no mesmo commit, e alteração de outra task não entra sem declaração no histórico | ACH-05 | requirements/guidelines/backend/java/definition-of-done.md |
| Starter declarado e não usado é dívida: ou a fronteira valida por anotação, ou a dependência sai do POM | ACH-03 | requirements/guidelines/backend/java/definition-of-done.md |

Dois desses guardrails são transversais e caberiam melhor em `_shared/`, que
não tem `testing.md` nem `definition-of-done.md`. Criar arquivo novo de coleção
é decisão de `/guidelines` e não de `/code-review`: foram escritos nos
equivalentes de `backend/java`, e a promoção fica como dívida nomeada com dono
`/guidelines` — mesmo tratamento dado nas revisões de TASK-01.5 e TASK-01.6.

---

## Veredicto

- **GATE-REVISAO-TECNICA:** reprovado
- **GATE-NFR:** reprovado
- **Bloqueantes em aberto:** 2
- **Revisor humano:** pendente — 2026-09-11

> Bloqueante em aberto reprova, sem exceção e sem waiver: a policy proíbe waiver
> para verificação independente e para dados.

ACH-01 e ACH-02 são ambos resolvíveis de dentro da task e não dependem de
etapa posterior. O GATE-NFR reprova por razão própria e já conhecida: dois dos
quatro envelopes desta tabela não são mensuráveis antes do fechamento do
EPIC-01.

---

## Fora deste artefato — regras negativas

- **Correção de código** — pertence ao `/implement`. O revisor descreve o
  achado e o devolve; não conserta.
- **Requisito novo** — achado de spec vira devolução ao `/prd`, não requisito
  escrito aqui.
- **Decisão de arquitetura** — pertence ao `/techspec`.
- **Cenário Gherkin** — congelado; alteração exige emenda no PRD.
- **Reescrita do plano de tasks** — pertence ao `/tasks`.
