# Revisão técnica — TASK-01.3

_Data: 2026-09-10 | Revisor: agente `/code-review` | Épico: EPIC-01 | PR: —_
_Commits revisados: 2df0fd6..HEAD (árvore de trabalho — a task ainda não foi commitada)_

> O revisor não escreve código. Achado é devolvido a quem implementa; correção
> feita pelo revisor faz o revisor revisar a si mesmo.

## Escopo revisado

| Item | Valor |
| --- | --- |
| Tasks | TASK-01.3 |
| Cenários entregues | nenhum — SCN-002.1 é o único declarado e depende de TASK-01.4 e TASK-01.5 |
| Arquivos | 10 de produção e migração, 1 de teste fora da suíte congelada |
| Suíte | não compila — Red de compilação, medido desde TASK-01.1 |

- **Cenários congelados alterados no período:** nenhum
- **Step definitions alterados no período:** nenhum

`git diff --stat 8346946..HEAD` sobre `docs/prd/**/*.feature` e sobre
`backend/src/test` volta vazio nas duas varreduras. A única mudança na árvore de
teste é o arquivo novo `ResolvedorDePermissaoTest.java`, que é acréscimo e não
alteração de material congelado — ver ACH-07.

## Conformidade com a especificação

| Cenário | Passa | Implementado conforme a task | Observação |
| --- | --- | --- | --- |
| SCN-002.1 | não | parcial | Exige `GET /v1/projetos` (TASK-01.5) e a rota de sessão que autoprovisiona (TASK-01.4). Inalcançável nesta task, como o histórico registra |

Os seis critérios de aceite foram reconferidos por execução: a migration aplica
em banco limpo pelo serviço dedicado, o backend sobe com `ddl-auto=validate`
contra o schema aplicado, o par usuário↔projeto não admite duplicata, os papéis
acumulam, não há tabela de catálogo de papel, e o resolvedor devolve a tabela de
permissões. A tabela papel×permissão do código é idêntica à de `data-model.md`
§3 papel a papel, incluindo as três sutilezas — `product_owner` encerra sem
configurar, é o único que reabre, e `project_admin` não reabre.

- **Escopo além do especificado:** `Usuario.espelharDoToken`,
  `Participacao.conceder` e `Participacao.revogar` realizam RF-001 e RF-019, que
  são de TASK-01.4 e do EPIC-02. Nenhum tem consumidor nem teste nesta task.

## Envelopes de RNF

| RNF | Envelope | Medido | Como | Situação |
| --- | --- | --- | --- | --- |
| RNF-004 | Autorização decidida no servidor sobre a participação real | — | nenhuma rota existe ainda | não medido |
| RNF-009 | p95 das consultas | — | nenhuma consulta de leitura exposta | não medido |

Nenhum RNF é mensurável antes do fechamento do EPIC-01: não há rota, não há
carga e a suíte não compila. A ausência de medição é o achado, e é ela que
mantém o GATE-NFR reprovado — como nas duas revisões parciais anteriores.

## Achados

| ID | Severidade | Tipo | Local | Descrição | Destino |
| --- | --- | --- | --- | --- | --- |
| ACH-01 | bloqueante | segurança | `backend/src/main/java/br/com/idsd/kanban/internal/projeto/ResolvedorDePermissao.java:31` | O ponto único de decisão de acesso ignora `admin_global`. A TechSpec §8 e o ADR-010 decidem que a administração global dispensa a participação para ver e agir em qualquer projeto, e SCN-021.1 a SCN-021.3 congelam isso; aqui quem não participa recebe conjunto vazio, e o admin global nunca participa por construção. `pode(...)` é oferecido como atalho para o chamador não reimplementar o teste, de modo que o caminho mais curto a partir deste arquivo produz recusa a quem a spec manda deixar passar — ou um bypass reescrito em cada rota, que é o que o ADR-010 existe para eliminar | /implement |
| ACH-02 | relevante | segurança | `.../ResolvedorDePermissao.java:43` | `permissoesDe(Collection<Papel>)` é público e calcula permissão a partir de papéis fornecidos pelo chamador, sem banco e sem transação. É a forma exata de autorização decidível a partir de dado do cliente que RNF-004, BDR-001 e a javadoc da própria classe proíbem. O risco não é hipotético: a revisão de TASK-01.2 encontrou papéis de negócio no token, e `permissoesDe(papeisDoToken)` é o caminho mais curto para quem implementar TASK-01.4. A visibilidade existe só para o teste, que poderia exercitar `Papel.getPermissoes()`. Não rebaixado a menor: rebaixar achado de segurança exige aprovador humano nomeado, e esta revisão parcial não tem um | /implement |
| ACH-03 | relevante | spec | `.../Papel.java:57` | O catálogo institui `user` como participação que não lê o board, e participação sem papel algum resolve para conjunto vazio. A suíte congelada afirma o contrário em comentário — "sem papel algum a pessoa participa e nada pode fazer além de ler" — e SCN-002.3 distingue participante de não-participante por recusa que não revela dado, o que a TechSpec §8 traduz em `403` contra `404`. Como está, o chamador de TASK-01.5 não tem como escolher entre os dois e vai aplicar um deles a tudo: ou vaza a existência do projeto, ou responde `404` a quem participa | /techspec |
| ACH-04 | relevante | dados | `.../Participacao.java:52` | `@ElementCollection(fetch = EAGER)` sem estratégia de fetch join, com `usuario` e `projeto` em `@ManyToOne(LAZY)`. `ParticipacaoRepository.findByUsuarioId`, declarado ali mesmo como a origem da lista de RF-002, dispara por participação uma consulta de papéis e outra de projeto, cujo nome a listagem obrigatoriamente lê. É o N+1 que `backend/java/database.md` §2 manda evitar e que `data-model.md` §6 nomeia, na primeira tela de toda navegação, sob o p95 de RNF-009 | /implement |
| ACH-05 | relevante | spec | `docs/techspec/kanban-tarefas/data-model.md:407` | A tabela de ordem das migrations continua listando `projeto.seq_atual` como conteúdo da migration 6, e a task manda criá-la na 1 — corretamente, para que a migration que a introduz não precise ser alterada depois. Migration aplicada não se altera, então quem implementar a 6 seguindo a TechSpec escreverá `ADD COLUMN seq_atual` e a migração falhará contra qualquer banco já migrado, no serviço dedicado, com o backend esperando por `service_completed_successfully`. A decisão está certa; faltou propagá-la | /techspec |
| ACH-06 | relevante | código | `.../Papel.java:83` | Código de papel fora do catálogo levanta `IllegalArgumentException`, e a desserialização de um corpo com `{"papeis": ["superusuario"]}` nem chega lá: medido nesta revisão, Jackson resolve o valor pelo `@JsonValue` e reprova o desconhecido com `InvalidFormatException`, que o Spring traduz em `400`. O contrato de participação e SCN-019.2 congelam `422` com `$.status == 422`. É armadilha plantada duas tasks à frente, com a causa neste arquivo | /implement |
| ACH-07 | relevante | código | `backend/src/test/java/br/com/idsd/kanban/internal/projeto/ResolvedorDePermissaoTest.java:25` | A única prova do critério 6 não roda no verificador do projeto: o arquivo vive em `src/test/java`, onde a suíte não compila, e os 8/8 verdes vieram de `javac` mais console do JUnit à mão. Critério de aceite cuja verificação não é reprodutível por `mvn verify` é evidência de uma execução, não teste. Agrava que o caminho que importa para RNF-004 — `permissoesNoProjeto`, que garante que a resposta sai da participação gravada — não é exercitado por teste nenhum, porque o serviço é instanciado com repositório nulo | /implement |
| ACH-08 | relevante | segurança | `backend/src/main/resources/db/migration/V2026091009__configuracao_base.sql:18` | O ADR-010 torna a unicidade do admin global uma invariante de segurança e, no mesmo documento, declara que promover outra pessoa se faz por alteração direta em banco. As duas coisas juntas põem a única garantia num serviço que ainda não existe, sujeito a check-then-act, e no canal que por definição não passa pelo serviço. Índice único parcial sobre `admin_global` a tornaria estrutural, no mesmo raciocínio que fez RN-014 virar propriedade do esquema. Não rebaixado a menor: rebaixar achado de segurança exige aprovador humano nomeado, e esta revisão parcial não tem um | /techspec |
| ACH-09 | menor | dados | `V2026091009__configuracao_base.sql:37` | Não há índice que sirva `participacao.projeto_id` isolado — o único é `(usuario_id, projeto_id)`, cujo prefixo só cobre `usuario_id` —, e listar participantes de um projeto é RF-019. A FK de `participacao_papel` não tem `ON DELETE CASCADE`: a remoção de participação de RN-027 depende de o Hibernate apagar os filhos primeiro, e falha por violação em qualquer caminho de SQL direto. A tabela de índices de `data-model.md` também não os nomeia, então o buraco é herdado | /techspec |
| ACH-10 | menor | código | `.../ResolvedorDePermissao.java:35` | `permissoesNoProjeto` e `permissoesDe` devolvem o `EnumSet` interno, e o chamador pode acrescentar `CONFIGURAR` ao conjunto que acabou de receber. `Participacao.getPapeis()` e `Papel.getPermissoes()` são imutáveis — a inconsistência ficou justamente no único que representa uma decisão de acesso | /implement |
| ACH-11 | relevante | segurança | `backend/src/main/java/br/com/idsd/kanban/internal/acesso/Usuario.java:64` | A entidade expõe `subjectId`, `email` e `criadoEm` por getter, e não há `record` de resposta em lugar nenhum, contra `backend/java/architecture.md` e `coding-standards.md`. Nada está exposto hoje, mas o caminho mais curto em TASK-01.4 é serializar a entidade, o que vaza a chave de vínculo que o ADR-010 acabou de tornar o eixo da identidade. Não rebaixado a menor: rebaixar achado de segurança exige aprovador humano nomeado, e esta revisão parcial não tem um | /implement |
| ACH-12 | menor | código | `.../Usuario.java:54` | `Instant.now()` embutido no construtor, sem `Clock` injetado, também em `Projeto`. Os épicos seguintes medem intervalos, episódios de RN-019 e p95 de RNF-009; fixar o relógio depois exigirá tocar nestas entidades | /implement |
| ACH-13 | menor | spec | `docs/tasks/kanban-tarefas/TASK-01.3-migration-configuracao.md:29` | A tabela de arquivos prescreve entidades, repositórios e serviço planos em `internal/acesso` e `internal/projeto`, sem a separação que `backend/java/architecture.md` exige. A implementação seguiu a task, então o defeito não é dela — é a terceira task seguida em que a coleção e o plano discordam sem que ninguém tenha varrido um contra o outro, mesma classe de ACH-06 da revisão de TASK-01.2. A mesma tabela pede "repositórios dos quatro agregados" e só há três, porque `participacao_papel` não é agregado | /techspec |

**Refutado nesta revisão, e vale registrar porque a suspeita era razoável:** a
desserialização do papel **está** amarrada ao código do catálogo. Medido com o
Jackson do projeto, `"project_admin"` resolve pelo `@JsonValue` e o nome da
constante `"PROJECT_ADMIN"` é recusado. O que sobrevive é só o código de
resposta, que é ACH-06.

## Análise de segurança

| Verificação | Resultado | Evidência |
| --- | --- | --- |
| Entrada validada nas fronteiras | n/a | nenhuma fronteira de entrada nesta task; a primeira é TASK-01.4 |
| Autorização verificada por operação | achado | ACH-01, ACH-02, ACH-03 — o resolvedor é o ponto único de decisão e responde errado para o admin global, aceita papéis do chamador e não distingue não-participante de participante sem papel |
| Segredo fora do código e do log | ok | `trivy --scanners secret` exit 0 nas imagens em TASK-01.2; nenhum literal nos dez arquivos |
| Dado sensível fora de log e mensagem de erro | achado | ACH-11 — `subjectId` acessível por getter sem DTO na frente |
| Dependência nova sem vulnerabilidade conhecida | n/a | nenhuma dependência acrescentada nesta task |

Conferido e correto: o vínculo de identidade é por `subject_id UNIQUE` e não há
nenhum caminho de busca por e-mail; `adminGlobal` não tem setter e nasce falso,
sem rota de escrita; o catálogo é enumeração fechada sem tabela; nenhuma coluna
de pessoa foi acrescentada a nada que agregue tempo (RN-014); não há gatilho de
`NOTIFY`.

## Guardrails extraídos

| Guardrail | Origem | Onde passa a valer |
| --- | --- | --- |
| Serviço que responde "o que esta pessoa pode" declara, no próprio arquivo, quais sujeitos ele **não** resolve. Alcance que vive fora dele vira bypass espalhado pelos chamadores | ACH-01 | guidelines/backend/java/architecture.md |
| Método que calcula autorização a partir de argumento do chamador não é público. Se o teste precisa dele, o teste exercita a fonte do dado, não o atalho | ACH-02 | guidelines/backend/java/coding-standards.md |
| Critério de aceite só se dá por cumprido por verificação que `mvn verify` reproduz. Execução manual comprova uma vez e não protege nada depois | ACH-07 | guidelines/backend/java/definition-of-done.md |
| Decisão de implementação que antecipa conteúdo de migration posterior exige, na mesma passagem, correção da ordem declarada na TechSpec — migration aplicada não se altera | ACH-05 | guidelines/backend/java/database.md |
| Coleção de guidelines elaborada depois do plano de tasks obriga varredura das tasks contra a norma nova. Terceira ocorrência da mesma classe | ACH-13 | guidelines/_shared/definition-of-done.md |

## Veredicto

- **GATE-REVISAO-TECNICA:** reprovado
- **GATE-NFR:** reprovado
- **Bloqueantes em aberto:** 0 — ACH-01 foi fechado pelo `/implement` em 2026-09-10
- **Revisor humano:** pendente — Thiago Goncalves Cavalcante

> Bloqueante em aberto reprova, sem exceção e sem waiver: a policy proíbe waiver
> para verificação independente e para dados.

Revisão parcial de task, não de épico: o GATE-NFR permaneceria reprovado mesmo
sem ACH-01, porque nenhum envelope é mensurável antes de o EPIC-01 fechar.

### Fechamento do bloqueante (2026-09-10, `/implement`)

| Achado | Como fechou | Prova |
| --- | --- | --- |
| ACH-01 | O resolvedor passou a ler `usuario.adminGlobal` e a declarar, no próprio arquivo, que resolve **dois** sujeitos. `acessoAoProjeto` devolve um `Acesso` com as permissões, a marca `porAdministracaoGlobal` que SCN-021.2 exige e o sinalizador `participa`; `pode(...)` passa a decidir pelos dois. O alcance global é o conjunto todo de `Permissao` e não se soma a papel nenhum. Criar projeto (RN-036) ficou declaradamente fora, porque não é permissão de projeto | 6 testes novos sobre o caminho que lê do banco, incluindo admin global sem participação e admin global que também participa; 14/14 verdes |
| ACH-02 | `permissoesDe(Collection<Papel>)` deixou de ser público. Quem precisa de permissão chama `acessoAoProjeto`, que lê do banco | package-private; o teste, que vive no mesmo pacote, continua exercitando o catálogo |
| ACH-10 | Os três retornos de conjunto passaram a ser imutáveis, como já eram `Participacao.getPapeis()` e `Papel.getPermissoes()` | leitura do arquivo |

ACH-07 fecha pela metade e continua aberto: o caminho que lê do banco passou a
ter cobertura — que era o buraco que mais importava para RNF-004 —, mas a suíte
segue sem compilar, então a prova continua vindo de execução isolada por `javac`
mais console do JUnit, e não de `mvn verify`. ACH-03 continua aberto com dono
`/techspec`: o código deixou de colapsar "não participa" e "participa sem papel"
num vazio indistinguível, mas **qual** dos dois recebe `403` e qual recebe `404`
é decisão de spec e não foi tomada aqui.

Reconferido depois da mudança: o contexto Spring sobe com `ddl-auto=validate`
contra o schema aplicado (`Started Aplicacao in 8.657 seconds`), o que prova que
a dependência nova está ligada; e a varredura de compilação da suíte devolve
exatamente os mesmos cinco arquivos de tasks posteriores de antes — nenhuma
regressão.

## Fora deste artefato — regras negativas

- **Correção de código** — pertence ao `/implement`. O revisor descreve o
  achado e o devolve; não conserta.
- **Requisito novo** — achado de spec vira devolução ao `/prd`, não requisito
  escrito aqui.
- **Decisão de arquitetura** — pertence ao `/techspec`.
- **Cenário Gherkin** — congelado; alteração exige emenda no PRD.
- **Reescrita do plano de tasks** — pertence ao `/tasks`.
