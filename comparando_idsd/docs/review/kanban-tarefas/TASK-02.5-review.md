# Revisão técnica — TASK-02.5 (criação de tarefa)
_Data: 2026-09-15 | Revisor: agente `/code-review` | Épico: EPIC-02 | PR: —_
_Commits revisados: 7f89b45..87e5f20_

> O revisor não escreve código. Achado é devolvido a quem implementa; correção
> feita pelo revisor faz o revisor revisar a si mesmo.

---

## Escopo revisado

| Item | Valor |
| --- | --- |
| Tasks | TASK-02.5 |
| Cenários entregues | SCN-004.1, SCN-004.2, SCN-004.3, SCN-022.3 |
| Arquivos | 5 criados, 1 alterado (produção) |
| Suíte | 198 testes, 110 verdes / 88 vermelhos — **remedida nesta revisão no contêiner de ADR-012; bate com o `/implement`** |

- **Cenários congelados alterados no período:** nenhum
- **Step definitions alterados no período:** nenhum

`git show --stat 87e5f20` não toca `docs/prd/` nem `backend/src/test/`. A Fase 0
passa.

**A suíte foi remedida nesta revisão, e confere.** O Docker Desktop está na
máquina fora do `PATH` (`AppData/Local/Programs/DockerDesktop`, daemon 29.7.2) —
mesmo padrão do Maven em `D:\CobraKai`. A suíte rodou inteira no contêiner
`maven:3.9-eclipse-temurin-25` com o socket do host montado (ADR-012),
`--add-host host.docker.internal:host-gateway` e `TESTCONTAINERS_HOST_OVERRIDE`,
sobre cópia descartável em `backend/target/medicao` sem os **2** arquivos de
teste que não compilam — os mesmos 2 já registrados, nenhum novo. Resultado:
**198 testes, 110 verdes / 88 vermelhos** (36/0 no surefire, 162/88 no
failsafe), **idêntico ao que o `/implement` registrou, com zero divergência**. A
pré-condição "suíte verde" segue não satisfeita, mas pela linha de base
conhecida e não por afirmação não verificada.

Dois achados foram além da leitura e **medidos por sonda descartável** na mesma
cópia, nunca na suíte real (ACH-01 e ACH-02); os demais são de leitura,
corroborados por dois agentes que leram do disco por caminho independente
(`security`, `qa`).

---

## Conformidade com a especificação

| Cenário | Passa | Implementado conforme a task | Observação |
| --- | --- | --- | --- |
| SCN-004.1 | não | sim | não passa hoje: `CriacaoDeTarefaIT` morre em `GET /v1/tarefas/{id}`, rota de TASK-02.6 — dependência, não defeito |
| SCN-004.2 | sim | sim | verde em `CriacaoDeTarefaServiceTest` (8/8); a variante de integração depende do board |
| SCN-004.3 | não | sim | não passa hoje; e ver ACH-08 — os dois testes que levam este ID verificam **outra** coisa |
| SCN-022.3 | sim | sim | medido pelo `/implement`; é o único ponto que exercita a recusa por ausência de fluxo |

- **Escopo além do especificado:** `CartaoResposta.etapaId` (ACH-07) e
  `Tarefa.descricao` gravada sem leitor até TASK-02.6 (ACH-13). Nada mais: os
  cinco arquivos criados são os cinco declarados na tabela da task, e a alteração
  em `RegistradorDeEvento` é a retirada de Javadoc que a própria task manda fazer.

---

## Envelopes de RNF

| RNF | Envelope | Medido | Como | Situação |
| --- | --- | --- | --- | --- |
| RNF-002 | nenhuma divergência entre instâncias; 300 sessões | não | exige perfil `broadcast` e 3 instâncias | não medido |
| RNF-004 | 100% das escritas reavaliadas no servidor | não | `CriacaoDeTarefaIT.somenteLeituraNaoCria` é o instrumento e está vermelho pela rota de board | não medido |
| RNF-008 | nenhuma operação do produto altera evento gravado | não | `ImutabilidadeDoLogIT` segue dependente da rota de leitura | não medido |
| SDR-005 (espera) | 5 s de bloqueio, 10 s de transação, 5 s de conexão | parcial | leitura: `lock_timeout` de sessão alcança esta rota; o teto de transação, não (ACH-11) | fora, de um lado |

**Nenhum envelope foi medido nesta revisão** — a suíte foi remedida por inteiro
(198 / 110 / 88), e o que ela não alcança são justamente os instrumentos destes
três RNF, todos vermelhos por dependência da rota de leitura de TASK-02.6. A
ausência de instrumentação executável é o achado — registrado em ACH-04. O GATE-NFR segue reprovado pela
razão de sempre, agora com uma razão nova: a rota de escrita que nasce aqui
entra sem o teto de transação que SDR-005 declarou para escrita.

---

## Achados

| ID | Severidade | Tipo | Local | Descrição | Destino |
| --- | --- | --- | --- | --- | --- |
| ACH-01 | bloqueante | segurança | `internal/tarefa/CriacaoDeTarefaService.java:89` | `raiaId` vem do corpo e é gravado sem conferir que a raia pertence ao projeto da rota nem que não está arquivada. A FK de `V2026091413__tarefa.sql:19` é global e não escopada por projeto: quem escreve no projeto A cria tarefa apontando para raia do projeto B. É o eixo raia↔projeto deixado aberto pelo mesmo tipo de guarda que `RegistradorDeEvento.exigirCoerencia` fecha no eixo tarefa↔projeto. **Medido por sonda descartável no contêiner:** `POST /v1/projetos/{A}/tarefas` com `raiaId` de um projeto B devolve **`201 Created`**, e o cartão de resposta devolve o `raiaId` alheio — não é risco inferido, é comportamento observado | /implement |
| ACH-02 | bloqueante | código | `internal/tarefa/CriacaoDeTarefaService.java:89` + `shared/TratadorDeErro.java:141` | `raiaId` inexistente viola a FK e sai como `5xx`: o catálogo do tradutor é nominal e só conhece `etapa_projeto_ordem_unico`. Entrada de cliente autenticado que o contrato manda recusar na borda vira erro de servidor depois de a linha de `tarefa` já ter sido gravada e descarregada — a mesma forma dos quatro bloqueantes da primeira revisão de TASK-02.2. **Medido por sonda descartável no contêiner:** `raiaId` aleatório inexistente devolve **`500 erro-interno`** | /implement |
| ACH-03 | bloqueante | código | `internal/tarefa/CriacaoDeTarefaService.java:81` | O fluxo é lido **antes** de a transação tomar qualquer bloqueio do projeto: o bloqueio de linha só chega no `UPDATE projeto SET seq_atual` de `RegistradorDeEvento`, dentro de `registrar`. Janela real — a criação lê a etapa E; um `PUT /etapas` concorrente trava o projeto, conta zero tarefas em E e a arquiva; a criação desbloqueia e insere a tarefa em E, porque o arquivamento é lógico e a FK passa. Resultado: tarefa fora do fluxo vigente, invisível no board e sem destino alcançável por RN-005. É literalmente a cláusula de SDR-005 que ACH-04 da reexecução de TASK-02.2 fixou — bloqueio **antes** da leitura —, aplicada à rota nova só pela metade | /implement |
| ACH-04 | bloqueante | código | `docs/tasks/kanban-tarefas/TASK-02.5-criacao-de-tarefa.md:critérios 1, 3, 6, 7, 8` | Cinco dos dez critérios seguem sem medição, e a razão não é ambiente: 1, 3 e 6 morrem em `GET /v1/tarefas/{id}` e `GET /board`, rotas de TASK-02.6, e 7 e 8 — os que verificam a porta que esta task existe para publicar — dependem da mesma leitura em `ConfiguracaoDoFluxoIT`. A porta foi publicada e **não há verificação passante de que ela liga a recusa de RF-017**. Não é corrigível dentro do escopo de arquivo desta task | /tasks |
| ACH-05 | relevante | segurança | `internal/tarefa/NovaTarefaRequisicao.java:26` | `titulo` e `descricao` não têm teto: o record não tem `@Size`, o controlador não usa `@Valid` — ao contrário de `EtapaController` e `ProjetoController` —, e as duas colunas são `text`. O único limite é o filtro global de 256 KiB, de modo que uma escrita autenticada grava título de ~8 KB e descrição de ~250 KB por requisição, numa tabela sem poda. `Etapa.TAMANHO_MAXIMO_DO_NOME` existe desde a reexecução de TASK-02.2 pela mesma razão; a rota nova não herdou a decisão | /implement |
| ACH-06 | relevante | código | `internal/tarefa/CriacaoDeTarefaService.java:110` | Título acima de 8 KiB é recusado pelo teto de `dados` do núcleo e sai como `400 requisicao-invalida` — "Os dados enviados não puderam ser lidos" —, depois de `em.persist`/`em.flush` da linha de `tarefa`. Três defeitos num: contradiz o Javadoc do próprio método, que promete recusa antes de qualquer escrita; não nomeia o campo em `errors`, ao contrário de `exigirTitulo`; e apresenta um limite interno do log como erro de leitura do corpo. O teto de `dados` está fazendo por acidente o papel do limite de campo que falta em ACH-05 | /implement |
| ACH-07 | relevante | spec | `docs/techspec/kanban-tarefas/contracts/board-e-tarefas.md:56` | O contrato congela o cartão sem `etapaId`, e a suíte congelada **exige** o campo (`CriacaoDeTarefaIT:57`, e `Cenario.origemDe` o lê para montar o bloco de origem de toda escrita). A implementação está certa e o contrato é que está errado — e ele é a fonte que o board de TASK-02.6 vai reusar | /techspec |
| ACH-08 | relevante | código | `backend/src/test/java/br/com/idsd/kanban/internal/tarefa/CriacaoDeTarefaIT.java:104` | Os dois testes rotulados `SCN-004.3` verificam a recusa por papel somente-leitura, e SCN-004.3 no `.feature` é **projeto sem fluxo configurado**. A recusa que o cenário descreve só é exercitada dentro do teste de SCN-022.3, e o plano de verificação marca SCN-004.3 como coberto por esta classe. O rótulo errado migrou para o Javadoc de `TarefaController` | /tests |
| ACH-09 | relevante | código | `internal/tarefa/TarefaController.java:108` e `:115` | Critério 6 — `404` para quem não participa — não é alcançado por teste nenhum: a suíte cobre `403` (gestor, e participante sem papel em `ParticipacaoIT`) e nunca `POST` de sujeito sem participação nem de projeto inexistente. Apagar os dois `if` deixa a suíte verde, inclusive o de `existsById`, escrito justamente para evitar um `500` por chave estrangeira | /tests |
| ACH-10 | menor | código | `internal/tarefa/CriacaoDeTarefaService.java:92` | Dois relógios na mesma criação: `criadaEm` é um `Instant.now()` e `ocorridoEm` é outro, e os intervalos abrem pelo segundo. `criada_em` fica milissegundos antes de `permanencia.desde`, e é campo que o log não determina — confrontar com o alcance declarado de SDR-006 | /implement |
| ACH-11 | menor | código | `internal/tarefa/CriacaoDeTarefaService.java:77` | `@Transactional` sem `timeout` na primeira borda de escrita sobre tarefa. A rota de fluxo recebeu `timeout = 10` no fechamento de ACH-01 da reexecução de TASK-02.2, e esta disputa a mesma linha de `projeto`. O `lock_timeout` de sessão alcança a espera; o teto de transação, não | /implement |
| ACH-12 | menor | código | `backend/src/test/java/br/com/idsd/kanban/internal/tarefa/CriacaoDeTarefaIT.java:89` | O critério 3 pede contagem de eventos antes e depois, e o teste confere que o board está vazio. Board vazio não distingue "nada gravado" de "evento gravado sem projeção", que é o defeito que a transação única existe para impedir | /tests |
| ACH-13 | menor | spec | `docs/tasks/kanban-tarefas/TASK-02.5-criacao-de-tarefa.md:pontos de atenção` | A task manda implementar `long contarEm(UUID etapaId)`; a porta real é `Map<UUID, Long> contarEm(Collection<UUID>)` desde ACH-07 da reexecução de TASK-02.2. Implementada a real — o artefato é que está desatualizado. Já registrado pelo `/implement` | /tasks |
| ACH-14 | menor | código | `internal/tarefa/TarefaController.java:78` | Janela TOCTOU entre a decisão de acesso e a escrita: `recusaSeNaoEscreve` roda fora da transação do serviço. É a mesma janela decidida e mantida na emenda de SDR-005 para a rota de fluxo — a diferença é que ali a decisão está registrada e aqui ela é herdada em silêncio | /techspec |

### Sobre a severidade de ACH-01 e ACH-05

Os dois são achados de segurança e por isso bloqueantes por padrão. ACH-05 fica
em **relevante** porque o teto global de 256 KiB limita a amplificação e a
recusa não é um `500` — mas o rebaixamento exige aprovador humano nomeado e
**ainda não tem um**. Até que tenha, conte-o como bloqueante.

ACH-01 não é rebaixável pelo argumento que valeu em TASK-02.4: lá o argumento
era "não há chamador externo", e o chamador externo é exatamente este arquivo.

---

## Análise de segurança

| Verificação | Resultado | Evidência |
| --- | --- | --- |
| Entrada validada nas fronteiras | achado | ACH-01, ACH-02, ACH-05 — `raiaId` sem escopo de projeto, sem tradução de FK e sem teto de campo |
| Autorização verificada por operação | ok | `TarefaController:100-124` — `404` para sem alcance, `404` para projeto inexistente sob alcance global, `403` para participante sem `ESCREVER_TAREFA`; decisão antes de qualquer escrita |
| Ator não negociado com o cliente | ok | ACH-12 de TASK-02.4 **fecha**: `NovaTarefaRequisicao` não tem campo de ator e o controlador o extrai do principal |
| `dados` sem passagem livre do corpo | ok | ACH-15 de TASK-02.4 **fecha**: `dadosDaCriacao` constrói o `ObjectNode` chave a chave a partir de §4 |
| Segredo fora do código e do log | ok | nenhum segredo nos arquivos desta task |
| Dado sensível fora de log e mensagem de erro | ok | `detail` de `404` é genérico; o broadcast leva projeto, tarefa, tipo, `seq` e instante, e nunca `dados` |
| Injeção | ok | as consultas nativas e a JPQL usam parâmetro nomeado |
| Dependência nova sem vulnerabilidade conhecida | n/a | nenhuma dependência nova |

**O rebaixamento dos cinco achados de segurança de TASK-02.4 caducou aqui, e
caducou sem dívida:** as duas metades que eram desta task fecharam
estruturalmente, como a emenda previu, e os dois Javadoc que apontavam para cá
saíram. O que esta borda trouxe de novo é outra coisa — ACH-01 e ACH-05.

---

## Guardrails extraídos

| Guardrail | Origem | Onde passa a valer |
| --- | --- | --- |
| Identificador de recurso recebido do cliente é conferido contra o escopo da rota antes de ser gravado; chave estrangeira não escopada por projeto não é validação de pertencimento | ACH-01 | `guidelines/backend/java/seguranca.md` |
| Toda restrição de banco alcançável por entrada tem tradutor nominal, e a rota que a torna alcançável entra no catálogo junto | ACH-02 | `guidelines/backend/java/erros.md` |
| Leitura que decide onde a escrita vai acontecer é feita **depois** do bloqueio que serializa a operação, e não antes | ACH-03 | `guidelines/backend/java/concorrencia.md` |
| Campo de texto gravado por escrita autenticada tem teto declarado na borda, e não herdado do teto de corpo | ACH-05 | `guidelines/backend/java/validacao.md` |
| Rótulo de cenário em teste é conferido contra o `.feature`, porque o plano de verificação é derivado dele | ACH-08 | `guidelines/_shared/verificacao.md` |

---

## Veredicto

- **GATE-REVISAO-TECNICA:** reprovado
- **GATE-NFR:** reprovado
- **Bloqueantes em aberto:** 4 (ACH-01, ACH-02, ACH-03, ACH-04), mais ACH-05
  enquanto não houver aprovador humano nomeado para o rebaixamento
- **Revisor humano:** — (pendente)

**ACH-01 e ACH-02 estão medidos**, não inferidos: `201` para raia de outro
projeto e `500` para raia inexistente, sonda descartável no contêiner, fora da
suíte congelada.

Três bloqueantes são de código e cabem no escopo de arquivo desta task; o
quarto, ACH-04, não cabe e volta ao `/tasks`, pela mesma razão que ACH-05 de
TASK-02.4: a medição dos critérios depende de rotas que nascem em TASK-02.6.

---

## Fora deste artefato — regras negativas

- **Correção de código** — pertence ao `/implement`. O revisor descreve o
  achado e o devolve; não conserta.
- **Requisito novo** — achado de spec vira devolução ao `/prd`, não requisito
  escrito aqui.
- **Decisão de arquitetura** — pertence ao `/techspec`.
- **Cenário Gherkin** — congelado; alteração exige emenda no PRD.
- **Reescrita do plano de tasks** — pertence ao `/tasks`.
