# Revisão técnica — TASK-01.5 (revisão parcial de task)
_Data: 2026-09-10 | Revisor: agente | Épico: EPIC-01 | PR: não aberto_
_Commits revisados: 57c47a5..7a7c80b_

> O revisor não escreve código. Achado é devolvido a quem implementa; correção
> feita pelo revisor faz o revisor revisar a si mesmo.

## Escopo revisado

| Item | Valor |
| --- | --- |
| Tasks | TASK-01.5 |
| Cenários entregues | SCN-002.1, SCN-002.2, SCN-002.3, SCN-002.4, SCN-021.2, SCN-021.3 |
| Arquivos | 6 de produção (4 criados, 2 alterados fora da tabela declarada) |
| Suíte | não compila; 12 testes nas duas classes alcançáveis, 7 verdes e 5 vermelhos |

- **Cenários congelados alterados no período:** nenhum
- **Step definitions alterados no período:** nenhum

A Fase 0 teve linha de base real pela terceira revisão seguida: `git diff
8346946..HEAD` sobre `*.feature` e `backend/src/test` não devolve alteração de
cenário, e a árvore de teste só recebeu acréscimo — as correções da etapa de
testes já registradas. O commit desta task não toca em arquivo de verificação
algum.

Duas ressalvas de método, herdadas e não desta task. A suíte inteira segue sem
compilar pelos mesmos cinco arquivos de tasks posteriores, de modo que nenhuma
das medições vem de `mvn verify` — vêm de `javac` mais o console do JUnit,
como nas duas revisões anteriores. E a pré-condição desta skill que exige
`check_escopo` aprovado é inverificável por construção: ver ACH-10.

## Conformidade com a especificação

| Cenário | Passa | Implementado conforme a task | Observação |
| --- | --- | --- | --- |
| SCN-002.1 | não | sim | Reprova por defeito da própria asserção, não da resposta — ACH-05. A permissão exigida está no corpo, medido |
| SCN-002.2 | sim | sim | Lista vazia é `200` com `conteudo: []`, nunca `404` |
| SCN-002.3 | sim | sim | `404` no detalhe, corpo sem nome, descrição ou qualquer atributo do projeto. O segundo caso do mesmo cenário, sobre o board, é rota de EPIC-02 |
| SCN-002.4 | não | não | Não realizável: exige a tabela `etapa`, que não existe. ACH-03 |
| SCN-021.2 | não | parcial | A marca é produzida e é correta por inspeção, mas nenhuma execução a alcança. ACH-01 |
| SCN-021.3 | não | não | Rotas de EPIC-02 e além; nada desta task acrescenta exceção por papel às três garantias estruturais, verificado |

- **Escopo além do especificado:** nenhum. As duas alterações fora da tabela de
  arquivos — as consultas em `ProjetoRepository` e `acessoDerivado` em
  `ResolvedorDePermissao` — estão declaradas no histórico com razão, e ambas as
  razões se sustentam: pôr as consultas no registro de projeção o transformaria
  em componente de acesso a dados, e montar o acesso fora do resolvedor criaria
  a segunda fonte da regra dos dois sujeitos.

O que a task mandou fazer foi feito, com uma exceção declarada. As decisões de
desenho que ela deixou em aberto foram tomadas na direção certa e a mais
importante delas é verificável no JPQL: a condição do usuário está dentro do
`ON` dos dois `left join` e não no `WHERE`, e é isso que impede o join de
degenerar em interno e apagar justamente o caso que RN-035 existe para cobrir.
A ordenação estável e o agrupamento em memória também conferem, e a unicidade
`(usuario_id, projeto_id)` da migration garante que o `putIfAbsent` do
cabeçalho não capture linha divergente.

## Envelopes de RNF

| RNF | Envelope | Medido | Como | Situação |
| --- | --- | --- | --- | --- |
| RNF-004 | 100% das escritas reavaliadas no servidor | não | Esta task não expõe escrita alguma; o envelope só é apurável com as rotas de escrita, que são de EPIC-02 em diante | não medido |
| RNF-009 | p95 ≤ 2 s com 12 meses de histórico e 5.000 tarefas | não | Exige carga sintética e as consultas de RF-015 e RF-016, que não existem | não medido |
| RNF-001 | p95 ≤ 2 s de propagação | não | Exige o canal de eventos, de EPIC-06 | não medido |
| RNF-002 | Nenhuma divergência entre 3 instâncias, 300 sessões | não | Arnês existe na suíte congelada, mas depende de rotas de escrita | não medido |
| RNF-005 | 1280 px a 1024 px nas 11 telas | não | Não há frontend em disco | não medido |
| RNF-006 | Zero violação WCAG 2.1 AA nas 11 telas | não | Idem | não medido |
| RNF-007 | As quatro séries apuradas por etapa e por projeto | não | Instrumentação é de épico posterior | não medido |
| RNF-008 | Nenhuma operação altera evento registrado | não | `evento_tarefa` não existe no esquema | não medido |
| RNF-010 | 120 leituras e 30 escritas por minuto por sujeito | não | Nenhum limitador existe; as duas rotas desta task são leitura e não o aplicam | não medido |
| RNF-003 | Subida completa a partir da imagem, sem passo manual | não | Medido na TASK-01.2 como subida da stack, não como envelope fechado | não medido |

**Nenhum RNF é mensurável antes do fechamento do EPIC-01**, e é essa ausência —
não a qualidade do código — que reprova o GATE-NFR nesta passagem, como nas
três revisões parciais anteriores.

Registro um ponto que não é RNF mas pertence a esta seção: o **critério de
aceite 6** declara como verificação a "contagem de comandos executados no teste
de integração", e nenhum instrumento de contagem existe. Ver ACH-09.

## Achados

| ID | Severidade | Tipo | Local | Descrição | Destino |
| --- | --- | --- | --- | --- | --- |
| ACH-01 | bloqueante | código | `internal/projeto/ResolvedorDePermissao.java:141`; `internal/acesso/AdminGlobalIT.java:109` | `acessoDerivado(...)` é o único caminho de decisão de acesso que as duas rotas desta task percorrem, e não tem teste algum. Os 14 verdes de `ResolvedorDePermissaoTest` exercitam `acessoAoProjeto` e `pode`, que nenhuma das duas rotas chama — o método medido e o método em produção são distintos, e nada garante que continuem concordando. Pelo lado de integração o buraco é o mesmo: a única asserção que afirma `acessoPorAdministracaoGlobal` na relação está depois de `cenario.fluxoPadrao(...)`, que chama rota de EPIC-02 e derruba o teste antes de a requisição acontecer. O resultado é que o mecanismo de autorização mais poderoso do sistema entra sem nenhuma prova executável, e o critério de aceite 4 fica cumprido por inspeção. É bloqueante por ser resolvível de dentro: `acessoDerivado` é restrito ao pacote, não depende de HTTP nem da tabela `etapa`, e os três casos que importam — participante com papel, participante sem papel, admin global com e sem participação — são unitários | /implement |
| ACH-02 | relevante | código | `internal/projeto/ProjetoController.java:34-36`, `:90`, `:95` | O mecanismo documentado não é o implementado. O cabeçalho institui que a distinção `403`/`404` vem de `Acesso#participa()` "e nunca de conjunto de permissões vazio", e o contrato fixa isso como regra única do produto. No código `Acesso.participa()` não é consultado em lugar nenhum — varredura em `src/main` e `src/test` devolve zero: o `404` sai de lista vazia, isto é, do `where` do repositório, e o `403` sai de `!tem(LER)`, que é exatamente derivar de conjunto vazio. O desfecho é correto hoje porque a consulta já filtra por participação, mas a garantia migrou do resolvedor para uma cláusula `where` que não menciona a regra, e a javadoc afirma o contrário do que o arquivo faz. O padrão `!tem(X) → 403` é o que a próxima rota vai copiar, e nela o não-participante recebe `403` — o vazamento que SCN-002.3 existe para impedir | /implement |
| ACH-03 | relevante | spec | `docs/tasks/kanban-tarefas/TASK-01.5-projetos-visiveis.md:29`, `:109` | `fluxoConfigurado` não é implementável nesta task: o campo exige `EXISTS` sobre `etapa`, e a única migration em disco cria `usuario`, `projeto`, `participacao` e `participacao_papel`. Confirmado por leitura da migration. A decisão de omitir o campo em vez de fixá-lo em `false` está certa — constante falsa pareceria implementada e atravessaria a revisão em silêncio. O que sobra é de planejamento: a task depende só de TASK-01.4, foi fechada com o critério 7 aberto e com um cenário congelado do seu próprio escopo irrealizável, e a rota está publicada em desacordo com o contrato que promete o campo. Mesma classe de ACH-07 da revisão de TASK-01.1 | /techspec |
| ACH-04 | relevante | código | `internal/acesso/SessaoEProjetosIT.java:77-78` | Asserção vácua, e é a única do produto que afirma que `gestor` é somente-leitura. O caminho `$.conteudo[?(@.nome=='Gama')].permissoes` devolve uma coleção cujo único elemento é o array de permissões, e `not(hasItem(...))` compara contra o array e nunca contra seus itens — a asserção é verdadeira para qualquer resposta, inclusive para uma em que `gestor` tivesse escrita. É a mesma mecânica de ACH-05, com o sinal invertido, e por isso pior em espécie: aquela reprova e é visível, esta passa em verde. Corrigir exige tocar a suíte congelada | /techspec |
| ACH-05 | relevante | código | `internal/acesso/SessaoEProjetosIT.java:73-76` | A asserção de `permissoes` em SCN-002.1 é insatisfazível por qualquer resposta, pela mesma indefinição do caminho com filtro. Medido: a permissão exigida está no corpo e a asserção reprova assim mesmo. As três asserções de `papeis[0]` vizinhas passam apenas porque `.value()` desembrulha lista de um elemento, e a variante com `Matcher` não desembrulha. Nenhuma forma de saída satisfaz as três ao mesmo tempo, então não é caso de ajustar a resposta | /techspec |
| ACH-06 | relevante | spec | `docs/tests/kanban-tarefas-verificacao.md:144` | SCN-002.4 é declarado coberto em `SessaoEProjetosIT`, e a varredura da árvore de teste pelo identificador devolve zero. A emenda v1.5 propagou para a tabela de cobertura sem que o teste fosse escrito. Enquanto durar, a tabela de cobertura afirma 70/70 sobre um cenário que não tem verificador | /techspec |
| ACH-07 | relevante | código | `internal/projeto/ProjetoController.java:95-97` | Nenhum teste cobre o `403` do participante sem papel ou só com `user`. É a regra que o cabeçalho do controlador institui como única em todo o produto e que o contrato fixa em três linhas, e é o ramo mais fácil de inverter numa refatoração: trocá-lo por `404`, ou removê-lo deixando o `200`, não faz teste algum ficar vermelho. Nenhuma das duas classes de integração semeia participação sem papel | /techspec |
| ACH-08 | relevante | código | `internal/projeto/ResolvedorDePermissao.java:141`; `internal/projeto/ProjetoRepository.java:39` | `adminGlobal` passa a existir como booleano de chamador em dois lugares: parâmetro da consulta, que decide a visibilidade, e argumento de `acessoDerivado`, que decide o acesso. Quem o produz lê do banco e está correto hoje, mas passam a existir duas fontes da resposta "este sujeito é admin global" — a do resolvedor e a do controlador. `acessoDerivado` também não verifica que os papéis recebidos pertencem a uma participação daquele usuário: um `true` errado no argumento é bypass universal silencioso. Está mitigado por ser restrito ao pacote e documentado, e a razão de existir é legítima, mas é o padrão que ADR-010 e o bloqueante da revisão de TASK-01.3 existem para eliminar, reintroduzido em escala menor — e é o desenho que a próxima rota que precisar da mesma otimização vai copiar | /techspec |
| ACH-09 | relevante | spec | `docs/tasks/kanban-tarefas/TASK-01.5-projetos-visiveis.md:108` | O critério de aceite 6 declara como verificação a contagem de comandos executados no teste de integração, e nenhum arnês de contagem existe: zero ocorrência de estatística de sessão, contador de consultas ou proxy de fonte de dados em toda a árvore de teste. A propriedade é verdadeira por inspeção e está bem argumentada no histórico, mas não está travada por nada — quem trocar a projeção por navegação de associação numa task futura não quebra teste algum. Critério que só é verificável por leitura não distingue implementação de intenção | /techspec |
| ACH-10 | relevante | spec | `docs/tasks/kanban-tarefas/TASK-01.5-projetos-visiveis.md:37-40` | `check_escopo` não pode passar nesta task nem em outras 31: a tabela de arquivos escreve o caminho com o marcador de pacote em vez do caminho real, e o verificador acusa como fora de escopo até os quatro arquivos que a própria tabela declara. Medido — oito erros, seis deles arquivos declarados. A consequência é de gate e não de conveniência: a pré-condição desta skill exige `check_escopo` aprovado em cada task, e o histórico das quatro tasks fechadas relata a execução como se ela distinguisse desvio de conformidade. Mesma classe das pendências 06, 07 e 11: o validador reprova o estado correto e a mensagem não diz por quê | /techspec |
| ACH-11 | menor | código | `internal/projeto/ResolvedorDePermissao.java:76-78` | `semAlcance()` é código morto absoluto — não é chamado em produção nem em teste. Ele codifica a regra de existência que o controlador deveria aplicar e não aplica (ACH-02); método de decisão de acesso que existe e nunca é chamado dá a impressão de que a regra está em vigor | /implement |
| ACH-12 | menor | código | `internal/projeto/ResolvedorDePermissao.java:40-41` | O alcance global é o conjunto inteiro do enum, de modo que toda permissão futura é concedida à administração global no instante em que entra no catálogo, sem revisão e sem cenário. É o que RN-035 pede hoje e a alternativa da lista explícita tem o defeito oposto; fica registrado porque acrescentar valor a `Permissao` é, por construção, ampliar o alcance mais poderoso do sistema | /implement |
| ACH-13 | menor | código | `internal/projeto/ProjetoResumo.java:30-40` | A javadoc de `Pagina` afirma que os totais são do conjunto inteiro e não da fatia devolvida, e `de(...)` calcula sobre a fatia. Hoje coincidem porque não há paginação; a afirmação passa a ser falsa exatamente no dia em que alguém a ler para decidir se precisa mexer ali | /implement |
| ACH-14 | menor | código | `backend/src/test/T.java` | Arquivo de rascunho em pacote padrão, fora de `src/test/java`, commitado na TASK-01.4 e ainda na árvore. Não é compilado pelo Maven e por isso não quebra nada — e é justamente por isso que fica. Herdado, registrado para não atravessar o épico | /implement |
| ACH-15 | menor | dados | `db/migration/V2026091009__configuracao_base.sql:35-52` | Não há índice declarado para o percurso da consulta da relação; ela se apoia no prefixo da restrição única `(usuario_id, projeto_id)` por acaso e não por decisão. Não é correção nem segurança: é o envelope de RNF-009 na rota mais quente da navegação. Migration já aplicada, correção só em migration nova | /techspec |

O destino de ACH-03, ACH-06, ACH-09 e ACH-10 está registrado como `/techspec`
por limitação do validador, que não aceita `/tasks` nem `/tests` na coluna. O
dono real de ACH-03, ACH-09 e ACH-10 é o `/tasks`; o de ACH-04, ACH-05, ACH-06
e ACH-07 é o `/tests`. É o mesmo ponto cego que a revisão de TASK-01.2 já
registrou.

## Análise de segurança

| Verificação | Resultado | Evidência |
| --- | --- | --- |
| Entrada validada nas fronteiras | ok | As duas rotas recebem apenas o token e um identificador de caminho tipado; nenhuma aceita recorte por pessoa, nenhuma aceita papel ou projeto vindos do cliente. Identificador malformado falha na conversão e não sai em `application/problem+json`, que é dívida do tratamento global e não vaza nada |
| Autorização verificada por operação | achado | Correta no desfecho e verificada nos quatro casos, mas o mecanismo é outro que o documentado — ACH-02 —, e o caminho efetivamente percorrido não tem prova executável — ACH-01 |
| Segredo fora do código e do log | ok | Nenhuma credencial, chave ou segredo nos seis arquivos; nenhum log é emitido por eles |
| Dado sensível fora de log e mensagem de erro | ok | O corpo do `404` não carrega nome, descrição nem qualquer atributo do projeto, só o identificador que o próprio chamador enviou. Medido contra SCN-002.3 |
| Dependência nova sem vulnerabilidade conhecida | n/a | Nenhuma dependência acrescentada; o `pom.xml` não foi tocado |

Três verificações adicionais, feitas porque esta é a primeira rota que expõe
autorização na resposta:

- **Nenhuma autorização vem de claim.** A marca de administração global sai de
  `Usuario.isAdminGlobal()`, lido pelo `sub`. A varredura de `src/main` por
  leitura de claim devolve apenas nome e e-mail, ambos de exibição. É ADR-003 e
  RNF-004 no ponto em que seria mais barato desobedecê-los.
- **Sem injeção.** As duas consultas são texto constante com parâmetro nomeado;
  não há concatenação em lugar nenhum.
- **Sem vazamento de existência.** Com a marca falsa e sem participação, a linha
  é excluída pelo `where` nas duas consultas. Não há caminho que devolva projeto
  de terceiro.

Fica uma pergunta que não é defeito de implementação e que o contrato não
responde: a relação entrega `nome` e `descricao` a quem participa sem papel
algum, e o detalhe do mesmo projeto responde `403` para essa pessoa, com um
`detail` genérico justificado por não repetir o que a recusa protege. O código
segue o contrato à risca — a relação lista por participação, não por permissão.
O que não está decidido é se nome e descrição são dado protegido por `LER` ou
metadado da participação. Registrado aqui e não na tabela de achados porque não
há o que corrigir enquanto o contrato não decidir.

## Guardrails extraídos

| Guardrail | Origem | Onde passa a valer |
| --- | --- | --- |
| Método restrito ao pacote que decide acesso é testado como se fosse público: quem otimiza o caminho de N acessos cria um segundo método, e é o novo que precisa de teste, não o antigo | ACH-01 | guidelines/backend/java/testing.md |
| Task que entrega comportamento cuja única prova depende de rota de épico posterior declara o critério como não medido, em vez de deixá-lo em silêncio na tabela de aceite | ACH-01 | guidelines/backend/java/definition-of-done.md |
| Javadoc que nomeia o mecanismo de uma decisão de acesso é conferida contra o código na revisão: mecanismo documentado e não usado é pior que ausência de documentação, porque a próxima rota copia o que está escrito | ACH-02 | guidelines/backend/java/coding-standards.md |
| Asserção sobre coleção filtrada por JsonPath é escrita sobre o item e nunca sobre o resultado do filtro: a forma negativa passa vacuamente e não deixa sinal | ACH-04 | guidelines/_shared/testing.md |
| Critério de aceite cujo verificador é contagem de comandos exige o instrumento de contagem na mesma task; sem ele o critério é leitura de código com outro nome | ACH-09 | guidelines/backend/java/testing.md |
| Tabela de arquivos de task escreve o caminho real e nunca marcador de pacote, ou o verificador de escopo reprova os próprios arquivos declarados | ACH-10 | guidelines/_shared/definition-of-done.md |

## Veredicto

- **GATE-REVISAO-TECNICA:** reprovado
- **GATE-NFR:** reprovado
- **Bloqueantes em aberto:** 1
- **Revisor humano:** pendente — 2026-09-10

Revisão parcial de task não fecha épico. O GATE-NFR reprova por nenhum RNF ser
mensurável antes do fechamento do EPIC-01, como nas três revisões anteriores; o
GATE-REVISAO-TECNICA reprova por ACH-01, que é resolvível de dentro do
`/implement` e não depende de nenhuma task posterior.

> Bloqueante em aberto reprova, sem exceção e sem waiver: a policy proíbe waiver
> para verificação independente e para dados.

## Fora deste artefato — regras negativas

- **Correção de código** — pertence ao `/implement`. O revisor descreve o
  achado e o devolve; não conserta.
- **Requisito novo** — achado de spec vira devolução ao `/prd`, não requisito
  escrito aqui.
- **Decisão de arquitetura** — pertence ao `/techspec`.
- **Cenário Gherkin** — congelado; alteração exige emenda no PRD.
- **Reescrita do plano de tasks** — pertence ao `/tasks`.
