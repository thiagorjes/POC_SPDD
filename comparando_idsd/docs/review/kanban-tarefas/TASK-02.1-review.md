# Revisão técnica — TASK-02.1 (revisão parcial de task, não fecha o EPIC-02)

_Data: 2026-09-14 | Revisor: agente `/code-review` | Épico: EPIC-02 | PR: n/a (commit direto em `v202609041`)_
_Commits revisados: 6090d41..fd144c7_

> O revisor não escreve código. Achado é devolvido a quem implementa; correção
> feita pelo revisor faz o revisor revisar a si mesmo.

---

## Escopo revisado

| Item | Valor |
| --- | --- |
| Tasks | TASK-02.1 |
| Cenários entregues | SCN-017.1 (declarado; não executável nesta task) |
| Arquivos | 5 de produção (1 migration, 2 entidades, 2 repositórios) + 2 de registro |
| Suíte | 158 testes, 99 falhando |

- **Cenários congelados alterados no período:** nenhum
- **Step definitions alterados no período:** nenhum

Fase 0 — linha de base real pela sétima vez seguida. `git diff 8346946..HEAD`
sobre `docs/prd/kanban-tarefas/*.feature` e sobre `backend/src/test` volta vazio
para o commit desta task: `fd144c7` não toca arquivo de verificação nenhum.

A medição da suíte foi **refeita aqui** e não herdada. `mvn verify` não roda na
árvore de trabalho — para em `test-compile` sobre os mesmos **cinco** arquivos de
tasks posteriores que a task declara como Red (`ReconstrucaoDaProjecaoIT`,
`ImpedimentoServiceTest`, `EtapaServiceTest`, `CriacaoDeTarefaServiceTest`,
`TomadaServiceTest`), confirmado por execução em
`maven:3.9-eclipse-temurin-25`. O número só existe excluindo os cinco, o que foi
feito em cópia descartável fora do repositório: **20 unitários (20 verdes) + 138
de integração (39 verdes / 99 vermelhos)**. Os 99 vermelhos são exatamente os 99
da linha de base de TASK-01.8 — **nenhuma regressão**, e as duas entidades novas
sobem sob `ddl-auto=validate` em todo contexto de integração, o que é a prova
mais ampla do mapeamento que esta task podia receber.

---

## Conformidade com a especificação

| Cenário | Passa | Implementado conforme a task | Observação |
| --- | --- | --- | --- |
| SCN-017.1 | não | parcial | O esquema e o mapeamento existem; o cenário exercita `EtapaService`, `FluxoRequisicao` e as rotas de etapa, todos arquivos declarados de TASK-02.2. `ConfiguracaoDoFluxoIT` sai 5 testes / 5 vermelhos, e `RaiasIT` 3 / 3 — Red legítimo. Mesma assimetria de TASK-01.3, TASK-01.5 e TASK-01.8, dono `/tasks` |

- **Escopo além do especificado:** um item. O índice único parcial
  `raia_projeto_ordem_unico` não é pedido por task, PRD, contrato nem modelo de
  dados — ver ACH-06.

---

## Envelopes de RNF

| RNF | Envelope | Medido | Como | Situação |
| --- | --- | --- | --- | --- |
| RNF-001 | p95 de propagação ≤ 2 s | — | não há caminho de broadcast nesta task | não medido |
| RNF-002 | 3 instâncias simultâneas | — | task de esquema, sem comportamento de instância | não medido |
| RNF-003 | 50 usuários simultâneos | — | sem rota nova | não medido |
| RNF-004 | autorização nunca derivada de claim | — | esta task não decide acesso | não medido |
| RNF-005 | telas legíveis a 1024 px | — | sem tela | não medido |
| RNF-006 | WCAG 2.1 AA | — | sem tela | não medido |
| RNF-007 | instrumentação das métricas de sucesso | — | depende do anel de projeção | não medido |
| RNF-008 | histórico imutável | — | `evento_tarefa` não é tocado aqui | não medido |
| RNF-009 | p95 das consultas ≤ 1 s | — | sem consulta publicada; o único finder declarado não tem consumidor | não medido |
| RNF-010 | 120 leituras / 30 escritas por minuto por sujeito | — | sem rota | não medido |

> RNF não medido não passa por omissão. Se a instrumentação não existe, o
> GATE-NFR reprova — a ausência de medição é o achado.

Nenhum envelope é mensurável numa task de esquema, e é isso que o GATE-NFR
registra. Vale a ressalva de que RNF-009 tem aqui uma decisão de desenho que o
afeta e que **não** foi medida: a ausência deliberada de um segundo índice de
leitura está corretamente justificada (o parcial `(projeto_id, ordem)` é o
caminho de leitura do fluxo vigente), mas a justificativa é analítica, não
medida — e não há como medi-la antes de a rota existir.

---

## Achados

| ID | Severidade | Tipo | Local | Descrição | Destino |
| --- | --- | --- | --- | --- | --- |
| ACH-01 | bloqueante | código | `backend/src/main/java/br/com/idsd/kanban/internal/projeto/EtapaRepositorio.java:21` | Os dois repositórios estendem `JpaRepository`, que expõe publicamente sete assinaturas de remoção física (`delete`, `deleteById`, `deleteAll`, `deleteAllById`, `deleteAllInBatch`, `deleteAllByIdInBatch`, `deleteInBatch`). O javadoc do arquivo institui, com todas as letras, que "não há método que apague etapa" e que um `delete` exposto na interface destruiria a série de tempo sem deixar teste vermelho — a garantia declarada é falsa na assinatura publicada, e o item 4 da lista de execução da task está marcado por uma ausência que não existe. É a quinta ocorrência no produto da classe "o mecanismo documentado não é o implementado". Resolvível de dentro: a suíte congelada só exige `substituirFluxo(...)` e os finders, de modo que a base pode ser estreitada para `Repository`/`CrudRepository` sem remoção | /implement |
| ACH-02 | bloqueante | código | `backend/src/main/resources/db/migration/V2026091111__etapa_e_raia.sql:49` | A migration e `Raia.java:13-16` afirmam que **nenhuma** tabela do anel de projeção carrega `raia_id`, e que é essa ausência que torna a agregação por raia inescrivível. A afirmação é falsa contra três fontes normativas: `docs/techspec/kanban-tarefas/data-model.md:250` declara `tarefa.raia_id uuid FK NULL` dentro do §5 Anel de projeção, `board-e-tarefas.md:56` põe `raiaId` no cartão, e a suíte congelada `RaiasIT` monta tarefa com raia. A garantia verdadeira é estreita — `intervalo_tarefa` não carrega raia e nenhuma rota agregada aceita filtro por raia (`fila-e-consultas.md:122`) — e a generalizada será desmentida por TASK-02.3 | /implement |
| ACH-03 | bloqueante | código | `docs/tasks/kanban-tarefas/TASK-02.1-migration-etapa-e-raia.md:80` | O critério de aceite 5 é medido por varredura de `information_schema.columns` por `%raia%`, e a varredura devolve zero. O critério é **vazio hoje** — só existem as duas tabelas recém-criadas, e nenhuma delas poderia ter coluna de raia — e passa a ser **falso** no dia em que TASK-02.3 criar `tarefa.raia_id`, que é o que a TechSpec manda criar. Verificação sem poder de falha que, além disso, instrui a próxima task a violar a especificação; classe já bloqueante em TASK-01.6, TASK-01.7 e TASK-01.8 | /implement |
| ACH-04 | bloqueante | spec | `docs/techspec/kanban-tarefas/data-model.md:126` | A TechSpec se contradiz sobre a raia: §5 afirma em `:126-128` que nenhuma tabela do anel de projeção a referencia, e `:250`, no mesmo §5, declara `tarefa.raia_id`. É a origem documental de ACH-02 — quem implementar lendo uma das duas metades escreve o oposto de quem ler a outra, e a task seguinte é exatamente a que cria `tarefa` | /techspec |
| ACH-05 | relevante | código | `docs/tasks/kanban-tarefas/TASK-02.1-migration-etapa-e-raia.md:79` | O critério 4, "renomear a etapa preserva o identificador", é medido por um `UPDATE` de `nome` seguido da observação de que o `id` não mudou. `nome` não é chave, `id` é a PK e não está na cláusula `SET`: a verificação não tem como falhar, e falharia idêntica contra qualquer esquema concebível. O que o critério existe para proteger — que reconfigurar o fluxo não troque a etapa por outra — não é medido em lugar nenhum | /implement |
| ACH-06 | relevante | código | `backend/src/main/resources/db/migration/V2026091111__etapa_e_raia.sql:45` | `raia_projeto_ordem_unico` não tem origem normativa: a task manda criar o índice único parcial de ordem por projeto no contexto de `etapa` (guia técnico de `etapa`, ponto "Índice obrigatório"), o modelo de dados §6 lista **apenas** o índice de `etapa`, e o contrato de `PUT .../raias` não prevê `422` por ordem duplicada. O índice institui uma restrição de comportamento que a especificação não pede, e ela aparecerá como erro de banco não mapeado na task que implementar a rota | /implement |
| ACH-07 | relevante | dados | `backend/src/main/resources/db/migration/V2026091111__etapa_e_raia.sql:38` | O índice único parcial **não é adiável**, e não pode ser: `DEFERRABLE` só existe em `UNIQUE CONSTRAINT`, e restrição parcial não é expressável como constraint no PostgreSQL. O contrato de `PUT /v1/projetos/{id}/etapas` (`sessao-e-projetos.md:194-215`) substitui o fluxo inteiro numa requisição, o que inclui trocar a ordem de duas etapas ativas — operação que viola o índice no meio da transação a menos que quem implemente adote um passo intermediário. A consequência é operacional, cara e recai sobre TASK-02.2; a migration declara cuidadosamente um outro gatilho de reabertura (reordenar a restrição para `(ordem, projeto_id)`) e omite este | /implement |
| ACH-08 | relevante | código | `backend/src/main/resources/db/migration/V2026091111__etapa_e_raia.sql:1` | Zero cobertura automatizada para o esquema. `ddl-auto=validate` só verifica o que as entidades declaram: remover o índice parcial, o `NOT NULL` de `terminal`, o `DEFAULT false` ou a FK para `projeto` deixa toda a suíte verde. Os cinco critérios foram medidos por execução manual contra o banco, e essa medição não se repete em execução nenhuma — é a mesma classe de "verificação que não roda de novo" que o épico vem pagando | /implement |
| ACH-09 | relevante | dados | `backend/src/main/resources/db/migration/V2026091111__etapa_e_raia.sql:1` | A garantia de não-remoção é declarada no javadoc e pretendida no esquema, mas não há `REVOKE DELETE` nem papel restrito para as duas tabelas. `data-model.md:215` exige a garantia dupla — aplicação e banco — para `evento_tarefa`, pela mesma razão que o javadoc dá aqui: a série de tempo por etapa é destruída por um `DELETE` que nenhum teste alcança. Tipado como **dados** e não como segurança, e o tipo foi conferido em vez de herdado: não há fronteira de autenticação, autorização nem exposição envolvida — o dano modelado é destruição de série de tempo, que é exatamente o que o tipo `dados` cobre, e a policy proíbe waiver para ele igualmente, de modo que a classificação não afrouxa nada. Mesmo tratamento que a revisão de TASK-01.8 deu a ACH-03. Não é bloqueante porque conceder e revogar privilégio é decisão de infraestrutura e não desta migration, mas o par "javadoc afirma, banco permite" é o que sustenta ACH-01 | /implement |
| ACH-10 | menor | spec | `backend/src/main/java/br/com/idsd/kanban/internal/projeto/Etapa.java:15` | As referências de regra estão trocadas: o javadoc cita RN-023 para a estabilidade do identificador no rename e RN-021 para a adjacência, e o PRD atribui outra coisa a cada um dos dois — RN-023 é a regra da raia que não agrega, e a adjacência com a exceção do retorno à primeira etapa é RN-005. A mesma troca está no cabeçalho da migration | /implement |
| ACH-11 | menor | código | `backend/src/main/java/br/com/idsd/kanban/internal/projeto/Etapa.java:68` | `arquivar(Instant)` atribui incondicionalmente, de modo que arquivar duas vezes reescreve o instante do arquivamento — a data de saída da etapa passa a ser a da segunda chamada. `Raia.java:59` tem o mesmo comportamento | /implement |
| ACH-12 | menor | código | `backend/src/main/java/br/com/idsd/kanban/internal/projeto/Etapa.java:49` | Os construtores públicos das duas entidades aceitam `nome` nulo ou em branco e `ordem` negativa; o `NOT NULL` do banco pega o nulo, tarde e com mensagem de driver, e nada pega o branco nem o negativo. Não é bloqueante porque não há hoje caminho que os alcance — a validação de borda chega com as rotas em TASK-02.2 | /implement |
| ACH-13 | menor | código | `backend/src/main/java/br/com/idsd/kanban/internal/projeto/RaiaRepositorio.java:19` | `RaiaRepositorio` declara um finder cujo consumidor nasce em TASK-06.1, contrariando a regra que o javadoc do repositório irmão institui na mesma entrega — "os demais nascem com o consumidor, e não antes dele" | /implement |
| ACH-14 | menor | spec | `docs/tasks/kanban-tarefas-tasks.md:349` | O plano ainda registra TASK-02.1 como `Status: pendente`, contra `concluída` no arquivo da task; e `:353` declara "Depende de: nenhuma" enquanto a migration cria `REFERENCES projeto (id)`, isto é, depende de TASK-01.3, que criou a tabela `projeto`. A ordem executada esconde o defeito, e ele reaparece em quem reordenar o épico | /tasks |

**Severidade:**

- **bloqueante** — impede o merge. Correção obrigatória antes do gate.
- **relevante** — merge permitido com registro; entra no backlog com prazo.
- **menor** — registrado, sem prazo.

Uma nota de método. A hipótese mais chamativa que chegou à revisão — um conflito
entre a assinatura do construtor de `Etapa` e a chamada de três argumentos em
`Cenario.java:216` — foi **verificada e descartada**: a chamada tem por alvo um
`record` aninhado na própria classe de suporte, não a entidade de produção.
Mesmo tratamento que as revisões de TASK-01.5 e TASK-01.7 deram a afirmações de
agente desmentidas por leitura direta.

---

## Análise de segurança

| Verificação | Resultado | Evidência |
| --- | --- | --- |
| Entrada validada nas fronteiras | ok | Não há fronteira nesta task — nenhuma rota, nenhum controlador, nenhum desserializador. Os construtores aceitam valor inválido (ACH-12), mas nenhum caminho externo os alcança |
| Autorização verificada por operação | ok | Nenhuma decisão de acesso é tomada nem alterada aqui; `ResolvedorDePermissao` não é tocado |
| Segredo fora do código e do log | ok | Varredura dos cinco arquivos: nenhuma credencial, nenhuma URL com autenticação, nenhum log |
| Dado sensível fora de log e mensagem de erro | ok | Nenhuma mensagem de erro é produzida; o único texto que sai do banco é nome de etapa ou de raia, que é configuração pública do projeto |
| Dependência nova sem vulnerabilidade conhecida | n/a | Nenhuma dependência acrescentada — `pom.xml` não foi tocado |
| Remoção física impedida no banco | achado | ACH-09: não há `REVOKE DELETE` nem papel restrito para `etapa` e `raia`, enquanto `data-model.md:215` exige a garantia dupla para a série de tempo |
| Interface de acesso não expõe remoção | achado | ACH-01: `JpaRepository` publica sete assinaturas de remoção física nos dois repositórios que afirmam não ter nenhuma |

---

## Guardrails extraídos

Regras que esta revisão descobriu e que devem valer para as próximas.

| Guardrail | Origem | Onde passa a valer |
| --- | --- | --- |
| Repositório que declara não expor uma operação escolhe a interface base que de fato não a expõe; `JpaRepository` publica remoção física, e comentário não a revoga | ACH-01 | `requirements/guidelines/backend/java/coding-standards.md` |
| Comentário de esquema afirma a garantia estreita que o esquema sustenta, nunca a generalização que soa melhor — generalização em comentário é desmentida pela primeira tabela que nasce depois dele | ACH-02 | `requirements/guidelines/backend/java/coding-standards.md` |
| Critério de aceite medido por varredura que devolve zero exige que se demonstre a existência de um estado em que ela devolveria não-zero; senão o critério é vazio | ACH-03, ACH-05 | `requirements/guidelines/_shared/definition-of-done.md` (**não existe** — dívida nomeada, dono `/guidelines`) |
| Migration que cria índice, `NOT NULL`, `DEFAULT` ou FK deixa teste que falha se algum deles for removido; `ddl-auto=validate` só cobre o que a entidade declara | ACH-08 | `requirements/guidelines/backend/java/testing.md` |
| Índice único parcial não é adiável no PostgreSQL: a migration que o cria declara, no próprio arquivo, quais operações de reordenação em lote ele torna impossíveis em uma passagem | ACH-07 | `requirements/guidelines/backend/java/coding-standards.md` |
| Restrição de unicidade criada sem origem em contrato ou modelo de dados é escopo — o erro de banco não mapeado aparece na task que implementa a rota, longe da causa | ACH-06 | `requirements/guidelines/_shared/definition-of-done.md` (**não existe** — dívida nomeada, dono `/guidelines`) |
| Marca de remoção lógica é atribuída uma vez; reatribuição silenciosa reescreve o instante de saída, que é dado da série de tempo | ACH-11 | `requirements/guidelines/backend/java/coding-standards.md` |

Pela quinta revisão seguida, dois guardrails transversais ficam impedidos de ir
para `_shared/` porque `_shared/testing.md` e `_shared/definition-of-done.md` não
existem. Criar arquivo de coleção é decisão de `/guidelines`, não de
`/code-review`; fica como dívida nomeada, com o mesmo tratamento das revisões de
TASK-01.5 a TASK-01.8.

---

## Veredicto

- **GATE-REVISAO-TECNICA:** reprovado
- **GATE-NFR:** reprovado
- **Bloqueantes em aberto:** 4
- **Revisor humano:** pendente — pendente

> Bloqueante em aberto reprova, sem exceção e sem waiver: a policy proíbe waiver
> para verificação independente e para dados.

Esta é revisão **parcial de task** e não fecha o EPIC-02 em hipótese alguma:
SCN-017.1 não passa, e nenhum envelope de RNF é mensurável numa task de esquema.
Três dos quatro bloqueantes são resolvíveis de dentro da própria task (ACH-01,
ACH-02, ACH-03) e o quarto é propagação de spec com dono `/techspec` (ACH-04) —
mas ACH-04 precede ACH-02, porque é ele que decide qual das duas afirmações sobre
a raia é a verdadeira antes de alguém reescrever o comentário.

O que a task entregou está certo no que mais importa, e foi conferido e não
presumido: as duas tabelas correspondem coluna a coluna ao `data-model.md`
§3, o parcial é parcial de propósito — restrição total impediria reusar a ordem
de uma etapa arquivada, que é operação legítima e é o critério 3 —, a decisão de
**não** criar o segundo índice de leitura está correta e bem justificada, a
remoção é lógica em ambas as entidades, o identificador é atribuído na construção
e nunca reatribuído, e as duas entidades sobem sob `ddl-auto=validate` em 138
testes de integração. Os quatro bloqueantes estão todos na distância entre o que
os arquivos **afirmam** sobre si e o que eles garantem.

---

## Fora deste artefato — regras negativas

- **Correção de código** — pertence ao `/implement`. O revisor descreve o
  achado e o devolve; não conserta.
- **Requisito novo** — achado de spec vira devolução ao `/prd`, não requisito
  escrito aqui.
- **Decisão de arquitetura** — pertence ao `/techspec`.
- **Cenário Gherkin** — congelado; alteração exige emenda no PRD.
- **Reescrita do plano de tasks** — pertence ao `/tasks`.
