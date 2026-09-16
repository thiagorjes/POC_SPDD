# Revisão técnica — TASK-02.6 (leitura do board)
_Data: 2026-09-16 | Revisor: agente `/code-review` | Épico: EPIC-02 | PR: —_
_Commits revisados: 87e5f20..árvore de trabalho (os três arquivos do board ainda não commitados)_

> O revisor não escreve código. Achado é devolvido a quem implementa; correção
> feita pelo revisor faz o revisor revisar a si mesmo.

---

## Escopo revisado

| Item | Valor |
| --- | --- |
| Tasks | TASK-02.6 |
| Cenários entregues | SCN-003.1, SCN-003.2 |
| Arquivos | 5 (3 criados, 2 alterados) |
| Suíte | 198 testes, 81 falhando — **medição do `/implement`, não reexecutada nesta revisão** |

- **Cenários congelados alterados no período:** nenhum
- **Step definitions alterados no período:** nenhum

O `git status` não traz nenhum arquivo sob `docs/prd/**/*.feature` nem em
`backend/src/test/`: a Fase 0 passa, e a suíte é a mesma que foi acordada.

**A suíte não foi remedida pelo revisor.** O Docker Desktop está parado nesta
máquina (`npipe:////./pipe/dockerDesktopLinuxEngine` não responde), e o contêiner
de ADR-012 é o único caminho de medição. A linha "198 / 117 / 81" acima é o que o
`/implement` registrou, e vale como declaração dele e não como verificação desta
revisão. Onde a revisão depende de execução, isso está dito no achado.

Dois dos cinco arquivos — `CartaoResposta.java` e `CriacaoDeTarefaService.java` —
carregam também o fechamento de achados de TASK-02.5 no mesmo diff. O que é desta
task neles é apenas a assinatura de `CartaoResposta.de(...)` de quatro argumentos e
o `Responsavel` como objeto; o resto foi conferido íntegro e não é escopo daqui.

---

## Conformidade com a especificação

| Cenário | Passa | Implementado conforme a task | Observação |
| --- | --- | --- | --- |
| SCN-003.1 | nao | sim | `BoardIT.cartaoTrazAsTresDimensoes` exige tarefa movida e assumida — rotas de EPIC-03. O `/implement` registra que acenderam 2 de `BoardIT`, e este não é um deles |
| SCN-003.2 | sim | sim | `BoardIT.boardVazioDevolveAGradeCompleta`, `RaiasIT` e `ConfiguracaoDoFluxoIT` exigem célula vazia com `[]` e nunca omitida, e `BoardQuery.java:151-153` a produz |

- **Escopo além do especificado:** nenhum. A **raia sintética** (`BoardQuery.java:139-141`,
  `168-173`) é decisão de desenho nova, mas não é escopo a mais: `RaiasIT` congela o
  board de projeto sem raia configurada, e sem faixa alguma a resposta não teria onde
  pôr o cartão. Ela está declarada na task e em `BoardResposta.java:37-44`.

**Critérios de aceite da task:** 2 e 4 satisfeitos e verificados pela suíte
congelada; 5, 6 e 7 medidos por sonda descartável fora dela, que não fica no
repositório; 1 e 3 sem medição, por dependerem de mover, assumir e impedir
(EPIC-03/04). Ver ACH-04.

---

## Envelopes de RNF

| RNF | Envelope | Medido | Como | Situação |
| --- | --- | --- | --- | --- |
| RNF-009 | p95 ≤ 2 s com 12 meses de histórico e 5.000 tarefas no projeto | não | — | nao medido |
| RNF-002 | 1 a N instâncias sem divergência; 300 sessões ativas | não | — | nao medido |

A task declara RNF-009 na linha **Origem**, e o PRD (`kanban-tarefas-prd.md:1219`)
atrela o envelope de RNF-009 a **RF-015 e RF-016** — andamento e tempo por etapa —,
não a RF-003. O board, que é a leitura mais exercitada do produto e a que todo
cliente refaz a cada reconexão de WebSocket (ADR-004), **não tem envelope de tempo
de resposta em nenhum RNF do PRD**. Ver ACH-03: o problema não é só a ausência de
medição, é que não há contra o que medir.

---

## Achados

| ID | Severidade | Tipo | Local | Descrição | Destino |
| --- | --- | --- | --- | --- | --- |
| ACH-01 | bloqueante | código | `BoardResposta.java:24` | O corpo do board não traz `acessoPorAdministracaoGlobal`, e a **suíte congelada o exige sobre esta rota**: `AdminGlobalIT` afirma `$.acessoPorAdministracaoGlobal` igual a `true` e `$.etapas` com 4 elementos na **mesma** requisição ao board. O contrato do board não declara o campo, enquanto `sessao-e-projetos.md` o declara na lista e no detalhe — divergência de três pontas, e quem manda é a suíte congelada. Decidir e escrever no contrato é `/techspec`; emitir é `/implement` | /implement |
| ACH-02 | bloqueante | código | `BoardQuery.java:55-57`, `71-75`, `144` | **Cartão em etapa arquivada some do board sem erro.** A grade percorre só as etapas vigentes; as tarefas são lidas sem filtro de etapa, e a célula da etapa arquivada nunca é montada. Não é hipótese: `ContagemDeTarefasAtivas` exclui `CONCLUIDA` e `ENCERRADA_SEM_CONCLUSAO` da contagem que barra o arquivamento — **por decisão declarada**, justamente para que a etapa terminal seja arquivável —, de modo que uma operação permitida apaga da única tela todo o trabalho concluído ali. É exatamente o dano que `BoardResposta.java:40-43` invoca para justificar a raia sintética; o eixo etapa não recebeu a mesma defesa, e nada no contrato decide o que deveria acontecer | /techspec |
| ACH-03 | bloqueante | spec | `kanban-tarefas-prd.md:1219`; `BoardQuery.java:71-78` | **O board não tem envelope de desempenho, e o critério 5 é um proxy que não o substitui.** RNF-009 cobre RF-015/016 e não RF-003. A consulta traz **toda** tarefa já criada no projeto — sem recorte por condição, sem janela de data, sem paginação —, materializada como entidade gerenciada e convertida em cartão. O número de consultas é fixo, como o critério exige, mas o custo migrou para heap e payload, que crescem sem teto com a idade do projeto. Num projeto de 5.000 tarefas, que é o volume que o próprio PRD usa como referência, um participante legítimo basta para o incidente. A única contenção existente é o limite de 120 leituras/minuto por sujeito, que não limita o tamanho de cada uma | /prd |
| ACH-04 | bloqueante | código | `TASK-02.6-leitura-do-board.md:88-98` | **Cinco dos sete critérios sem verificação que fique.** 5 e 7 foram medidos por sonda descartável fora da suíte congelada, e nada no repositório os prende: trocar a montagem em memória por consulta por cartão deixa a suíte inteira verde, e o mecanismo que esta task existe para instalar não tem poder de falha. 6 está coberto pela metade — o não-participante tem teste, **projeto inexistente não tem**, e é justamente o caminho que depende de `BoardController.java:88` (ver ACH-06). 1 e 3 seguem sem medição por dependerem de EPIC-03/04, e essa metade é dependência de task e não defeito desta implementação | /tests |
| ACH-05 | relevante | spec | `contracts/board-e-tarefas.md:56` | `permanencia` é declarada **sem** `\| null`, ao contrário de `esperaTomada` e `impedimento`, e `CartaoResposta.java:112` a emite nula sempre que não houver `PERMANENCIA` aberta — o caso normal da tarefa concluída em etapa terminal, que `ReaberturaIT` coloca no board. Ou o contrato está errado, ou o cliente desreferencia nulo no caminho mais comum do board | /techspec |
| ACH-06 | relevante | código | `BoardQuery.java:53` | `em.find(Projeto.class, projetoId).getSeqAtual()` desreferencia sem checar nulo, e a guarda que o protege mora em **outra classe** (`BoardController.java:88`) — enquanto o Javadoc de `BoardQuery.java:34-36` institui que esta classe "não decide acesso". Existência é parte da decisão de acesso nesta rota. `montar` é público e é o modelo declarado do próximo leitor (TASK-02.10): `500` em vez de `404` é o desfecho quando a guarda não for repetida, e nenhum teste exercita esse caminho | /implement |
| ACH-07 | relevante | código | `BoardQuery.java:81-90`, `96-103`; `CartaoResposta.java:106-111` | O bloco de impedimento e o seu motivo vêm de **fontes diferentes** — o bloco existe porque há intervalo `IMPEDIMENTO` aberto, o motivo vem de `Impedimento` com `desfecho is null`. Divergência entre as duas produz `impedimento: { desde, decorrido, motivo: null }`, e `motivo` é `nullable = false` na entidade: um nulo ali só pode significar incoerência da projeção. A falha sai silenciosa, sem log e sem erro, e nenhum teste cobre a discordância | /implement |
| ACH-08 | relevante | spec | `contracts/board-e-tarefas.md:56` vs `:110-111`, `:31` | O cartão não tem `assumidaEm`, e o contrato se contradiz a si mesmo: descreve o cartão sem o campo e promete `assumidaEm` na saída da tomada e em `estadoAtual` do `409`. `TomadaIT` afirma `$.assumidaEm` sobre o cartão devolvido. Como **esta** task fixa a forma única do cartão reusada por todas as rotas, a omissão vence em TASK-02.7 e será descoberta lá como defeito da rota de tomada, que não é onde ela nasce | /techspec |
| ACH-09 | relevante | código | `BoardController.java:75-95` | A sequência 404 → `existsById` → 403 é cópia linha a linha de `TarefaController.java:100-122`, variando só a `Permissao` e os textos. Duas cópias já são a segunda fonte da mesma decisão de acesso, e a terceira nasce em TASK-02.10. A linha de risco é concreta: `existsById` é a mais fácil de omitir, porque só o admin global a alcança e a suíte de participante comum nunca a exercita | /implement |
| ACH-10 | menor | código | `BoardQuery.java:20-32` | O Javadoc afirma "cinco consultas" e **enumera seis** itens. ACH-03 da própria task corrigiu o texto da task para cinco de projeção mais a leitura do projeto; o Javadoc não acompanhou, e ele é o que o próximo leitor abre | /implement |
| ACH-11 | menor | código | `CartaoResposta.java:92` | `Instant.now()` é chamado **por cartão**, dentro de uma transação `readOnly` que existe declaradamente para que as consultas vejam o mesmo instante (`BoardQuery.java:46-49`). Um board grande devolve `decorrido` medidos em relógios diferentes, e o valor não é reproduzível em teste. O "agora" deveria ser um só, resolvido em `montar` | /implement |
| ACH-12 | menor | código | `BoardQuery.java:139-141` | A raia sintética **aparece e desaparece conforme os dados**: num projeto com 2 raias, arquivar a raia de uma tarefa muda o comprimento de `raias` de 2 para 3 em todas as colunas. A suíte indexa faixas por posição, e nenhum teste exercita a transição | /implement |
| ACH-13 | menor | código | `BoardQuery.java:81-84` | Os intervalos abertos não são filtrados por `episodio`. Intervalo aberto remanescente de episódio anterior — o que `ReaberturaIT` existe para vigiar — entraria no cartão do episódio corrente sem que nada acusasse | /implement |
| ACH-14 | menor | código | `BoardQuery.java:75` | A ordenação dentro da célula é `t.criadaEm asc` **sem desempate por `id`**. Tarefas criadas no mesmo instante saem em ordem não determinística entre duas leituras do mesmo board | /implement |
| ACH-15 | menor | código | `BoardQuery.java:168-172` | `raiaDaGrade` percorre a lista de raias para cada tarefa: O(tarefas × raias) em memória. Não é N+1 de banco, mas é custo que cresce com a quantidade de tarefas escondido atrás de um critério que só conta consultas. Um `Set<UUID>` montado uma vez resolve | /implement |
| ACH-16 | menor | código | `BoardController.java:88` + `BoardQuery.java:53` | O projeto é lido **duas vezes** por requisição, em transações distintas. Não quebra o critério 5, mas é ida ao banco evitável na leitura mais exercitada do produto, e as duas leituras podem discordar | /implement |
| ACH-17 | menor | código | `CartaoResposta.java:101-103` | O `left join` devolve nome nulo quando `responsavelId` aponta para usuário ausente, e o cartão sai `{id, nome: null}` — forma que o contrato não prevê. O `left join` é justamente o reconhecimento de que a linha pode faltar | /implement |

**Severidade:**

- **bloqueante** — impede o merge. Correção obrigatória antes do gate.
- **relevante** — merge permitido com registro; entra no backlog com prazo.
- **menor** — registrado, sem prazo.

---

## Análise de segurança

Conduzida pelo revisor e confirmada por leitura independente dos agentes
`security` e `qa`, que chegaram a ACH-02 por caminhos separados.

| Verificação | Resultado | Evidência |
| --- | --- | --- |
| Entrada validada nas fronteiras | ok | A rota tem uma única entrada, `projetoId`, tipada como `UUID` no `@PathVariable`: identificador malformado é recusado antes do controlador |
| Autorização verificada por operação | ok | `BoardController.java:75-95` — `404` por ausência de alcance, confirmação de existência, `403` por falta de `LER`. Nenhum `403` antes do `404`; a linha `existsById` é necessária e está no lugar certo, porque o alcance global vem da marca no usuário sem tocar `projeto` |
| Segredo fora do código e do log | ok | Nenhum literal e nenhuma chamada de log nos três arquivos criados |
| Dado sensível fora de log e mensagem de erro | ok | O `detail` das duas recusas é genérico e não carrega dado do projeto negado; `descricao` fica fora do cartão |
| Dependência nova sem vulnerabilidade conhecida | n/a | Nenhuma dependência acrescentada |

**Escopo de projeto:** as cinco consultas de projeção filtram por `:projeto`, e as
cinco entidades têm `projeto_id` próprio. **Não há caminho de vazamento
cross-projeto pelo board** — verificado consulta a consulta, e é o oposto do que a
revisão de TASK-02.5 encontrou na borda de escrita. **Injeção:** todas as consultas
são JPQL com `setParameter`; nenhuma concatenação de entrada.

Nenhum achado de segurança nesta revisão, e portanto nenhum rebaixamento a
justificar. ACH-02 é perda de visibilidade de dado legítimo e não quebra de
confidencialidade: o dado não vaza para quem não pode vê-lo, ele some para quem
pode.

---

## Guardrails extraídos

| Guardrail | Origem | Onde passa a valer |
| --- | --- | --- |
| Leitura que monta grade a partir de duas dimensões precisa decidir **as duas** bordas — o que acontece com o item cuja dimensão foi arquivada, em cada eixo. Defender um eixo e não o outro esconde dado sem erro | ACH-02 | `guidelines/backend/java/persistencia.md` |
| Consulta de coleção exposta em rota precisa de recorte declarado — condição, janela ou paginação. "Número fixo de consultas" limita idas ao banco e não o tamanho da resposta; são envelopes distintos e o segundo também precisa existir | ACH-03 | `guidelines/_shared/api-standards.md` |
| Toda rota de leitura nova nasce com verificação do caminho `404` por **identificador inexistente**, e não só por ausência de participação: é a metade que a suíte de participante comum nunca exercita | ACH-04, ACH-06 | `guidelines/_shared/verificacao.md` |
| Classe de consulta que declara não decidir acesso não pode desreferenciar o resultado de uma busca por identificador sem guarda própria. Ou ela decide, ou ela devolve ausência | ACH-06 | `guidelines/backend/java/persistencia.md` |
| Campo de resposta cuja presença depende de uma fonte e cujo conteúdo depende de outra precisa de decisão escrita para a divergência entre as duas. Nulo silencioso num campo `not null` é incoerência reportável, não ausência | ACH-07 | `guidelines/backend/java/persistencia.md` |
| O instante de leitura é **um** por requisição, resolvido na borda da transação e passado adiante — nunca `Instant.now()` dentro do laço que monta os itens | ACH-11 | `guidelines/backend/java/persistencia.md` |
| Resposta cuja estrutura muda de comprimento conforme os dados exige teste da transição, porque a suíte que indexa por posição passa nos dois estados estáveis e falha só entre eles | ACH-12 | `guidelines/_shared/verificacao.md` |

---

## Veredicto

- **GATE-REVISAO-TECNICA:** reprovado
- **GATE-NFR:** reprovado
- **Bloqueantes em aberto:** 4
- **Revisor humano:** pendente — 2026-09-16

O trabalho de montagem está correto onde foi medido, e duas decisões de desenho
desta task são acertadas e estão justificadas no código: a **raia sintética**, que
impede o cartão sem raia de sumir, e a **ausência de filtro por `condicao`**, que é
o que faz o cartão concluído continuar visível na etapa terminal. A separação entre
quem autoriza e quem consulta também está bem posta, e é ela que torna ACH-06
corrigível numa linha.

O que reprova tem duas naturezas. **ACH-01 é a suíte congelada não atendida** — o
campo que `AdminGlobalIT` exige do board não existe no corpo, e nenhuma medição
acusaria isso hoje porque aquela classe já morre adiante, em rota de EPIC-03. É o
tipo de defeito que só aparece quando o épico seguinte fecha e alguém o atribui à
task errada. **ACH-02 é a assimetria entre os dois eixos da grade**: a mesma frase
que justifica a raia sintética — cartão que não cabe na grade some, e some sem erro
— vale inteira para a etapa arquivada, e ali não há defesa nenhuma. Os outros dois
são de envelope e de verificação: o board é a leitura mais exercitada do produto e
**não tem envelope de tempo de resposta em RNF nenhum**, e o mecanismo que esta task
instala — número fixo de consultas — não tem, hoje, nenhum teste que fique vermelho
se ele for desfeito.

Registro por fim que **a suíte não foi reexecutada nesta revisão**, por ausência de
daemon Docker. Os achados acima são de leitura e de confronto com a suíte congelada,
e nenhum deles depende de execução para ser decidido — mas o veredicto de regressão
segue sendo o que o `/implement` declarou, e não o que esta revisão mediu.

---

## Fora deste artefato — regras negativas

- **Correção de código** — pertence ao `/implement`.
- **Requisito novo** — achado de spec vira devolução ao `/prd`.
- **Decisão de arquitetura** — pertence ao `/techspec`.
- **Cenário Gherkin** — congelado; alteração exige emenda no PRD.
- **Reescrita do plano de tasks** — pertence ao `/tasks`.
