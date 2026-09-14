# Revisão técnica — TASK-02.2 (revisão parcial de task, não fecha o EPIC-02)

_Data: 2026-09-14 | Revisor: agente `/code-review` | Épico: EPIC-02 | PR: n/a (commit direto em `v202609041`)_
_Commits revisados: fd144c7..be09e10_

> O revisor não escreve código. Achado é devolvido a quem implementa; correção
> feita pelo revisor faz o revisor revisar a si mesmo.

---

## Escopo revisado

| Item | Valor |
| --- | --- |
| Tasks | TASK-02.2 |
| Cenários entregues | SCN-017.2 (verificável hoje); SCN-017.1, SCN-017.3 e SCN-002.4 declarados, não executáveis nesta task |
| Arquivos | 13 de produção (8 criados, 5 alterados) + 2 de frontend + 3 de registro |
| Suíte | 168 testes, 98 falhando |

- **Cenários congelados alterados no período:** nenhum
- **Step definitions alterados no período:** nenhum

O único arquivo sob `src/test` tocado no período é `alem/EsquemaDoFluxoIT.java`,
verificação além dos cenários criada em TASK-02.1 e já revisada ali. Nenhum
`.feature` e nenhum step definition de cenário congelado mudou —
GATE-VERIFICACAO-INDEPENDENTE permanece de pé e a revisão prossegue.

A tabela de arquivos da task declarava 4 e foram tocados 13 de produção. O
desvio está declarado com justificativa por arquivo no histórico da própria
task, e foi conferido à mão porque `check_escopo.py` é inutilizável (pendências
12 e 18). Cada extensão foi conferida individualmente: todas decorrem ou da
suíte congelada, ou de critério de aceite escrito na própria task. **Não há
escopo além do especificado.**

---

## Conformidade com a especificação

| Cenário | Passa | Implementado conforme a task | Observação |
| --- | --- | --- | --- |
| SCN-017.1 | não | sim | 3 dos 4 testes exercitam `GET /board` e `POST /tarefas`, de TASK-02.6 e TASK-02.5. Falham com `404` de rota inexistente, não por caminho desta task. `semPermissaoDeConfigurarE403` passa |
| SCN-017.2 | sim | sim | `EtapaServiceTest` 5/5. Não é satisfeito por acidente: `recusaNaoPersisteNada` verifica pela negativa que nenhuma escrita chega ao repositório |
| SCN-017.3 | não | parcial | Renomear preserva o `id`, e o caminho está correto; o teste depende de tarefa. O ramo de recusa por tarefa ativa é **inerte** hoje — ver ACH-10 |
| SCN-002.4 | sem teste | sim | `fluxoConfigurado` é emitido e derivado por `EXISTS` na mesma consulta. O teste não existe e escrevê-lo é do `/tests` (pendência 14) |

**Critérios de aceite da task:** 1, 2, 4, 5, 6, 7, 8, 9 e 10 conferidos.
O critério 3 está marcado como cumprido e **não é exercível** — ver ACH-10.

- **Escopo além do especificado:** nenhum

---

## Envelopes de RNF

| RNF | Envelope | Medido | Como | Situação |
| --- | --- | --- | --- | --- |
| RNF-004 | 100% das escritas reavaliadas no servidor | sim | `EtapaController` recusa na borda antes de chamar o serviço, nas duas rotas; `semPermissaoDeConfigurarE403` prova a recusa | dentro |
| RNF-008 | nenhuma operação remove evento registrado | sim | arquivamento é lógico; `EtapaRepositorio` não publica assinatura de remoção; `arquivar` é idempotente | dentro |
| RNF-010 | 120 leituras / 30 escritas por sujeito por minuto | herdado | `LimiteDeRequisicoes` classifica por método HTTP e é global; o `PUT` novo entra como escrita sem configuração adicional | dentro |
| RNF-001, RNF-002, RNF-009 | propagação, multi-instância, tempo de consulta | não | dependem de evento, board e histórico, que nascem depois | não medido — mensurável só no fechamento do épico |
| RNF-003, RNF-005, RNF-006, RNF-007 | contêiner, responsividade, acessibilidade, instrumentação | não | sem superfície nesta task | não medido — mensurável só no fechamento do épico |

O critério 8 tem instrumento próprio e foi medido: `AusenciaDeNMaisUmIT` 2/2
depois da inclusão de `fluxoConfigurado`. O campo não custa consulta por item.

---

## Achados

| ID | Severidade | Tipo | Local | Descrição | Destino |
| --- | --- | --- | --- | --- | --- |
| ACH-01 | bloqueante | código | `internal/projeto/EtapaService.java:220` | `id` repetido no corpo derruba a rota. `vigentes.remove(id)` devolve `null` na segunda ocorrência e a linha seguinte o desreferencia. `exigirQueOsIdentificadoresSejamDoFluxo` confere pertinência, nunca unicidade — e o comentário logo acima afirma que "aqui ele existe", verdadeiro para existência e falso para duplicata. Duplicar um item na tela basta: `500` onde o contrato pede `422` | /implement |
| ACH-02 | bloqueante | código | `internal/projeto/EtapaService.java:94` e `EtapaRepositorioImpl.java:64` | Duas etapas desejadas com a mesma `ordem` não são recusadas: a colisão só aparece no `flush`, como violação de `etapa_projeto_ordem_unico`. Não há `@ExceptionHandler` para `DataIntegrityViolationException` em `TratadorDeErro`, de modo que o desfecho é `500` genérico. O javadoc de `EtapaRepositorioImpl:52-55` comemora trazer a violação para dentro da chamada — ela é trazida e ninguém a traduz | /implement |
| ACH-03 | bloqueante | código | `internal/projeto/EtapaService.java:162` | Elemento `null` na lista de etapas derruba a rota. `@Valid` sobre `List<EtapaDesejada>` cascateia nos elementos não-nulos e ignora os nulos, e Jackson aceita `null` como item de array. `noneMatch(EtapaDesejada::terminal)` o desreferencia. Mesma exposição em `omitidas` e `aplicar` | /implement |
| ACH-04 | bloqueante | código | `internal/projeto/EtapaRepositorioImpl.java:26` com `FluxoRequisicao.java:50` | A faixa de trabalho é alcançável pela requisição. `ordem` só tem `@PositiveOrZero`, e `FAIXA_DE_TRABALHO` é `1_000_000`: uma etapa nova pedida em `ordem` 1.000.000 colide com a vigente de `ordem` 0 já deslocada, no mesmo flush. O javadoc de `SubstituicaoDeFluxo:27` afirma "uma faixa que nenhum fluxo real ocupa", e nada garante a afirmação. O passo que existe para evitar a colisão a reintroduz pelo outro lado, e de forma intermitente — só com etapas vigentes. Valor perto de `Integer.MAX_VALUE` estoura a soma para negativo | /implement |
| ACH-05 | bloqueante | código | `internal/projeto/EtapaService.java:93-149` e `Etapa.java` | Substituição concorrente sem controle algum. `Etapa` não tem `@Version`, a leitura do fluxo não toma lock e não há estado de origem declarado — que é exatamente o que SDR-002 institui. Dois `PUT` simultâneos: o segundo calcula sobre um fluxo já obsoleto, e a etapa criada pelo primeiro não está no conjunto vigente dele, logo não é arquivada nem reordenada. O desfecho é perda silenciosa da escrita do primeiro, ou colisão de ordem com a etapa órfã e `500` intermitente. A transação garante atomicidade; isolamento de decisão ela não garante. A escolha entre `@Version` com `409` e lock pessimista é decisão de desenho, e por isso o achado volta primeiro ao desenho | /techspec |
| ACH-06 | relevante | código | `internal/projeto/EtapaController.java:107-113` | O alcance global não confere a existência do projeto. `ResolvedorDePermissao:83-95` devolve `ALCANCE_GLOBAL` para `adminGlobal` sem consultar `projeto`. No `GET`, projeto inexistente responde `200 {"etapas":[]}`; no `PUT`, a substituição segue para o serviço e estoura na chave estrangeira de `etapa.projeto_id` — `500` e escrita tentada numa rota que deveria ter sido recusada na borda. `ProjetoController.detalhe` não tem o buraco porque consulta `alcancadoPor` e trata a lista vazia | /implement |
| ACH-07 | relevante | código | `internal/projeto/EtapaService.java:82` | `temFluxo` não tem nenhum consumidor. É pior que código morto: é uma **segunda** derivação de `fluxoConfigurado`, paralela ao `EXISTS` que o critério 8 exige, e publicada ao alcance de quem escrever a próxima rota. Reintroduzir o N+1 por ela não faria nenhum teste falhar. É a mesma classe de ACH-13 da revisão de TASK-02.1 | /implement |
| ACH-08 | relevante | código | `internal/projeto/SubstituicaoDeFluxo.java:7` | O javadoc abre com "a única escrita de `Etapa` no sistema", e não é. As escritas reais são o dirty checking sobre as entidades gerenciadas que `EtapaService` muta em `:137`, `:141` e `:221`; qualquer flush automático grava sem passar pelo fragmento. É a mesma classe de ACH-01 da revisão de TASK-02.1 — comentário que descreve uma garantia que a estrutura não dá —, agora deslocada da interface para o contexto de persistência | /implement |
| ACH-09 | bloqueante | segurança | `internal/projeto/FluxoRequisicao.java:29-52` | Sem `@Size` na lista e sem limite no `nome`, contra uma coluna `nome text` sem teto no banco. Um `PUT` com lista muito longa ou nomes de megabytes é aceito e gravado numa transação só: consumo de armazenamento sem limite por sujeito autenticado, dentro do envelope de 30 escritas por minuto. **Bloqueante por ser achado de segurança**: rebaixá-lo exigiria justificativa registrada e aprovador humano nomeado, e o revisor não pode ser nenhum dos dois | /implement |
| ACH-10 | relevante | código | `internal/projeto/EtapaService.java:232-235` e `TarefasAtivasPorEtapa.java` | O critério 3 está marcado como cumprido e o ramo é inerte: sem bean publicado, `recusarSeContemTarefaAtiva` retorna antes do laço e nenhuma etapa com tarefa ativa é recusada. A decisão está declarada no histórico da task, mas a **obrigação de publicar a porta não está escrita em TASK-02.5** — uma busca em `docs/` só a encontra no arquivo de TASK-02.2. Se TASK-02.5 não a publicar, a regra de RF-017 nunca dispara e nada acusa. Um `TarefasAtivasPorEtapa` de teste publicado no contexto verificaria hoje o caminho de recusa e o formato de `errors`, sem esperar a entidade de tarefa. Prazo: antes de TASK-02.5 começar | /tasks |
| ACH-11 | relevante | spec | `docs/tests/kanban-tarefas-verificacao.md:240-242` | O plano de verificação atribui SCN-017.1, SCN-017.2 e SCN-017.3 ao **EPIC-07**; o plano de tasks os atribui ao **EPIC-02** em `:119-121` e na entrega do épico em `:339`. O plano de tasks é o correto — são os cenários de TASK-02.2 e TASK-02.7. Enquanto a divergência existir, a invariante cenário↔épico está fechada nos dois lados contra números diferentes | /tests |
| ACH-12 | relevante | código | `internal/projeto/Etapa.java:67` com `shared/TratadorDeErro.java:86` | As invariantes da entidade lançam `IllegalArgumentException`, mapeada para `400`, enquanto toda recusa de conteúdo bem formado do sistema é `422`. Hoje as anotações de `FluxoRequisicao` cobrem os dois casos e o caminho fica encoberto; no dia em que uma escrita não vier da borda — papel que o próprio javadoc da entidade reivindica — a resposta divergirá do contrato congelado | /implement |
| ACH-13 | menor | código | `internal/projeto/SubstituicaoDeFluxo.java:48` | `substituirFluxo` recebe `projetoId`, documenta-o em `@param` e nunca o usa: nada verifica que as etapas tocadas pertencem ao projeto. A assinatura é fixada pela suíte congelada e não pode mudar, mas o `@param` promete um escopo que a implementação não exerce | /implement |
| ACH-14 | menor | código | `internal/projeto/EtapaService.java:158-160` | A guarda `requisicao == null \|\| requisicao.etapas() == null` é inalcançável: `@Valid @RequestBody` e o `@NotNull` de `FluxoRequisicao.etapas` recusam antes. O código sugere que corpo sem `etapas` sai como `fluxo-sem-etapa-terminal`, e não sai — dois slugs para a mesma classe de erro, escolhidos por quem chama | /implement |
| ACH-15 | menor | código | `internal/projeto/Etapa.java:109` | `estaArquivada()` não tem consumidor em produção nem em teste. Superfície pública de entidade sem uso, na mesma linha de ACH-13 da revisão de TASK-02.1 | /implement |
| ACH-16 | menor | código | `internal/projeto/EtapaService.java:119` | `liberarOrdens` é chamado sempre, mesmo quando nenhuma ordem muda: dois `UPDATE` por etapa vigente em toda substituição. Não é defeito de correção; é custo por operação linear no tamanho do fluxo, e nenhum envelope de RNF o cobre | /implement |
| ACH-17 | menor | código | `internal/projeto/EtapaService.java:137-141` | A restituição da ordem original antes de arquivar está correta, e por uma razão que nada fixa: depende de o Hibernate emitir `INSERT` antes de `UPDATE` na fila de ações e de a linha arquivada sair do índice parcial no mesmo statement que restaura a ordem. Um `flush` intermediário acrescentado por qualquer motivo futuro quebra o caminho, e o sintoma é o `500` de ACH-02 | /implement |

**Severidade:**

- **bloqueante** — impede o merge. Correção obrigatória antes do gate.
- **relevante** — merge permitido com registro; entra no backlog com prazo.
- **menor** — registrado, sem prazo.

Quatro dos seis bloqueantes têm a mesma forma: **entrada que o contrato manda
recusar com `422` na borda sai como `500` depois de tentar escrever.** A task escreveu
essa regra por extenso no primeiro ponto de atenção — "a verificação acontece
antes de qualquer escrita" — e o serviço a cumpre para as duas recusas que
conhece, deixando de fora quatro que o corpo pode produzir. Os outros dois são
de natureza distinta: ACH-05 é ausência de decisão sobre concorrência, e ACH-09
é falta de teto para o que uma escrita autenticada pode consumir.

---

## Análise de segurança

| Verificação | Resultado | Evidência |
| --- | --- | --- |
| Entrada validada nas fronteiras | achado | ACH-01, ACH-02, ACH-03, ACH-04, ACH-09 — `FluxoRequisicao` cobre forma de campo e não cobre a forma do conjunto |
| Autorização verificada por operação | ok, com ressalva | `EtapaController:96-115` reproduz o padrão de `ProjetoController` e recusa antes de qualquer chamada ao serviço; `ResolvedorDePermissao` segue o único ponto de decisão. Ressalva em ACH-06 |
| Travessia entre projetos (IDOR) | ok | `EtapaService:180-196` confere cada `id` contra as etapas ativas **daquele** projeto antes de qualquer escrita, e a recusa é idêntica para "id de outro projeto" e "id inexistente" — não há oráculo. Etapa nova recebe o `projetoId` do path, nunca do corpo |
| Segredo fora do código e do log | ok | nenhum segredo nos arquivos tocados |
| Dado sensível fora de log e mensagem de erro | ok | `naoEncontrado` não confirma nem nega a existência do projeto (SCN-002.3); `errors` de `recusarSeContemTarefaAtiva` devolve nome e contagem de etapa do próprio projeto, atrás de `CONFIGURAR` |
| Injeção | ok | `ProjetoRepository` é JPQL com `@Param`, sem concatenação e sem SQL nativo; `EtapaRepositorioImpl` opera só por entidade gerenciada |
| Dependência nova sem vulnerabilidade conhecida | n/a | nenhuma dependência nova |

Um achado de segurança: ACH-09, abuso de recurso por sujeito autenticado, mantido
bloqueante porque o rebaixamento exige aprovador humano nomeado. ACH-01 a ACH-04
são tipados como código e não como segurança: derrubam a própria requisição e não
o processo, e nenhum deles atravessa a autorização.

---

## Guardrails extraídos

Regras que esta revisão descobriu e que devem valer para as próximas.

| Guardrail | Origem | Onde passa a valer |
| --- | --- | --- |
| Validação de borda cobre a **forma do conjunto**, não só a de cada campo: unicidade de identificador, unicidade de chave natural e ausência de elemento nulo na coleção. Anotação por campo não alcança nenhuma das três | ACH-01, ACH-02, ACH-03 | `guidelines/backend/java/validacao.md` |
| Constante de implementação que separa faixas de valor precisa ser inalcançável **por validação**, e não por suposição sobre o uso. Comentário que afirma "nenhum caso real chega aqui" é a afirmação a transformar em restrição | ACH-04 | `guidelines/backend/java/persistencia.md` |
| Toda restrição de banco que o produto pode violar por entrada tem tradução própria no tratador de erro. Restrição sem `@ExceptionHandler` é `500` esperando acontecer | ACH-02 | `guidelines/backend/java/erros.md` |
| Javadoc que afirma exclusividade ("a única escrita de X") só vale quando a estrutura a impõe. Sob JPA, o dirty checking é uma segunda porta de escrita que nenhum comentário fecha | ACH-08 | `guidelines/backend/java/persistencia.md` |
| Porta deixada inerte para uma task futura carrega a obrigação **escrita na task futura**, não apenas no histórico de quem a criou | ACH-10 | `guidelines/_shared/dependencias-entre-tasks.md` |
| Método público sem consumidor que duplica uma derivação já existente é devolvido, e não apenas anotado: ele é o caminho por onde a propriedade medida por um teste de invariância se perde sem que o teste acuse | ACH-07 | `guidelines/backend/java/repositorios.md` |

---

## Veredicto

- **GATE-REVISAO-TECNICA:** reprovado
- **GATE-NFR:** reprovado
- **Bloqueantes em aberto:** 6
- **Revisor humano:** pendente

GATE-REVISAO-TECNICA reprova pelos seis bloqueantes. GATE-NFR reprova pela
razão de sempre nesta altura: sete dos dez envelopes só se tornam mensuráveis
quando o épico fechar. Os três medidos — RNF-004, RNF-008 e RNF-010 — estão
dentro.

> Bloqueante em aberto reprova, sem exceção e sem waiver: a policy proíbe waiver
> para verificação independente e para dados.

---

## Fora deste artefato — regras negativas

- **Correção de código** — pertence ao `/implement`. O revisor descreve o
  achado e o devolve; não conserta.
- **Requisito novo** — achado de spec vira devolução ao `/prd`, não requisito
  escrito aqui.
- **Decisão de arquitetura** — pertence ao `/techspec`. ACH-05 é dela.
- **Cenário Gherkin** — congelado; alteração exige emenda no PRD.
- **Reescrita do plano de tasks** — pertence ao `/tasks`. ACH-10 é dele.
