# TASK-02.6 — Leitura do board

- **Status:** concluída
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 4
- **Depende de:** TASK-02.5
- **Cenários cobertos:** SCN-003.1, SCN-003.2
- **Origem:** RF-003, RN-002, RN-008, RN-015, RNF-009

#### Contexto

O board é a leitura central do produto e a que sustenta o envelope de desempenho
das consultas. Ele devolve as três dimensões em campos separados, e é essa
separação que impede o cliente de recolapsá-las — o defeito que a modelagem
inteira existe para evitar.

#### O que deve ser feito

- [x] Implementar `GET /v1/projetos/{projetoId}/board`.
- [x] Devolver etapas na ordem, com raias e cartões, incluindo etapa vazia com
      lista vazia.
- [x] Devolver `seq` do projeto no corpo.
- [x] Preencher `esperaTomada`, `impedimento` e `permanencia` como blocos
      independentes, sem nenhum campo de soma.
- [x] Montar a resposta com número fixo de consultas, sem consulta por cartão.
- [x] Devolver `404` para projeto sem participação.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/BoardController.java` | criar | rota de leitura |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/BoardQuery.java` | criar | consultas de projeção |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/BoardResposta.java` | criar | registro de saída |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/CartaoResposta.java` | alterar | acrescentado por ACH-01 em 2026-09-16: a suíte congelada exige `responsavel` como objeto `{id, nome}` e não como identificador solto, e o cartão é fixado por TASK-02.5 — o board o reusa, de modo que a correção cabe aqui |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/CriacaoDeTarefaService.java` | alterar | idem: único call site de `CartaoResposta.de(...)`, teve de acompanhar a assinatura |
| `backend/src/main/resources/db/migration/V2026091619__recorte_de_terminais_no_board.sql` | criar | acrescentado em 2026-09-16: migration de ordem 8 de SDR-007 — `tarefa.tornou_se_terminal_em`, índice parcial e população retroativa a partir do log |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/Tarefa.java` | alterar | idem: o campo projetado que sustenta RN-039 |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/Condicao.java` | alterar | idem: `terminal()` e `terminais()` — o conjunto das condições terminais passa a morar na origem, porque três consumidores o consultam |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/ContagemDeTarefasAtivas.java` | alterar | idem: passa a reusar `Condicao.terminais()` em vez da cópia local |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

`GET /v1/projetos/{projetoId}/board`

- **Saída `200`:**
  `{ seq, acessoPorAdministracaoGlobal, etapas: [ { id, nome, ordem, terminal, raias: [ { id, nome, tarefas: [ <cartão> ] } ] } ] }`.
- **Cartão:** a mesma forma fixada na criação de tarefa.
- Etapa sem tarefa vem na lista com `tarefas: []` — exibir a etapa vazia, não
  omiti-la.
- `esperaTomada` e `impedimento` podem estar **ambos** preenchidos, e a soma nunca
  é calculada nem devolvida.
- **`condicao` nunca vale `IMPEDIDA`.** O impedimento sai no bloco próprio do
  cartão e coexiste com qualquer condição não terminal. O cliente lê as três
  dimensões em campos separados: a etapa pela posição na lista, a `condicao`, e o
  bloco de impedimento presente ou ausente.
- `seq` é o último número de sequência do projeto, e é o que o cliente compara com
  o recebido pelo canal de tempo real para detectar lacuna.
- **`404`** projeto inexistente ou sem participação — nunca `403`, para não
  revelar existência.

Consultas usadas: **cinco na projeção** — etapas vigentes, raias vigentes,
tarefas do projeto com o nome do responsável por junção externa, intervalos
abertos e impedimentos abertos —, mais a leitura do projeto que resolve
participação e `seq`. Todas por projeto, nenhuma por cartão; a montagem é feita
em memória. **O que o critério 5 exige é que o número seja fixo, não que seja
este número** — corrigido de "quatro" para a contagem real em 2026-09-16
(ACH-03): etapa e raia são tabelas distintas e uni-las exigiria `union`, que não
compra nada.

A grade é o **produto cartesiano etapa × raia**, e o projeto sem raia configurada
também desenha: existe uma **raia sintética** com `id = null`, porque raia é
organização opcional (RN-023) e o cartão sem raia — ou apontando para raia
arquivada — precisa de faixa, sob pena de sumir do board sem erro.

#### Guia técnico — pontos de atenção

- **Não devolva total de tempo, nem calcule soma das séries.** Um campo de soma
  no cartão é o caminho mais curto para violar a regra que separa as três séries.
- **Consulta por cartão estoura o envelope de desempenho.** O número de consultas
  não pode crescer com a quantidade de tarefas.
- **`decorrido` é calculado a partir de `desde` no instante da leitura**, não
  armazenado.
- **A etapa vazia é informação.** Omiti-la esconde do time uma etapa configurada.
- **Etapas arquivadas não entram no board.**

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | O board devolve as etapas na ordem configurada, com os cartões em suas etapas e raias | leitura após percurso conhecido |
| 2 | Etapa sem tarefa aparece com lista vazia | configurar etapa sem tarefa e ler |
| 3 | Cartão com espera e impedimento simultâneos traz os dois blocos preenchidos e nenhuma soma | inspeção do corpo |
| 4 | Nenhum campo do corpo agrega as séries entre si | inspeção do contrato de saída |
| 5 | O número de consultas ao banco não cresce com a quantidade de tarefas | contagem de consultas com 5 e com 50 tarefas |
| 6 | Projeto sem participação devolve `404` | requisição com sujeito sem participação |
| 7 | `seq` corresponde ao último evento do projeto | comparação com o log |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
| 2026-09-15 | execução | **A leitura do board nasce, e com ela cinco critérios são medidos.** Três arquivos criados como a tabela declara. A grade é o produto cartesiano etapa × raia, montada em memória a partir de **cinco consultas fixas** — projeto, etapas vigentes, raias vigentes, tarefas com o nome do responsável por junção externa, intervalos abertos e impedimentos abertos —, todas por projeto e nenhuma por cartão. **A task fala em quatro** porque conta etapa e raia como uma; são tabelas distintas e uni-las exigiria `union`, que não compra nada. O que o critério exige é que o número seja **fixo**, e é. **Duas decisões próprias.** A **raia sintética** (`id = null`, "Sem raia") existe por duas razões independentes e não por comodidade: raia é organização opcional (RN-023), de modo que o projeto que nunca configurou nenhuma precisa desenhar mesmo assim — `RaiasIT` congela exatamente isso —, e onde há raias configuradas o cartão sem raia, ou apontando para raia arquivada, precisa de lugar, sob pena de sumir do board **sem erro**. E não há filtro por `condicao`: `ReaberturaIT` prova que cartão concluído aparece na etapa terminal; o que sai do board é a **etapa arquivada**, não o cartão terminal. **Medição no contêiner de ADR-012: 198 testes, 117 verdes / 81 vermelhos**, contra 198 / 110 / 88 — **+7 verdes, zero regressão**, confirmado contra a árvore do `HEAD` para a única classe suspeita (`CriacaoDeTarefaIT` já tinha aquela falha antes, e por dependência de rota). **Critérios 2 e 4 satisfeitos pela suíte congelada** (`BoardIT.boardVazioDevolveAGradeCompleta` e `tarefaAguardandoTomadaTrazAEspera` acenderam; o contrato não tem campo de soma); **5, 6 e 7 medidos por sonda descartável fora da suíte congelada** — 11 *prepared statements* com 5 tarefas e **os mesmos 11** com 50; `404` tanto para quem não participa quanto para projeto inexistente; `seq` do board igual ao `max(seq)` do log. **1 e 3 seguem sem medição**, e não por defeito: exigem mover, assumir e impedir, rotas de EPIC-03/04. **Um defeito real encontrado e corrigido na medição:** `Map.of().getOrDefault(chaveNula, ...)` estoura `NullPointerException`, e a chave da raia sintética é nula — o board vazio saía `500`. **Três achados registrados abaixo.** |

| 2026-09-16 | execução | **Os 17 achados da revisão fechados do lado do código, e a medição volta exatamente à base.** Materializados os quatro que a spec decidiu — `acessoPorAdministracaoGlobal` no corpo (ACH-01), a **etapa sintética "Fora do fluxo"** para o cartão em etapa arquivada (ACH-02), o recorte de RN-039 aplicado **na consulta** contra `tarefa.tornou_se_terminal_em` (ACH-03, SDR-007), `permanencia` anulável e `assumidaEm` no cartão (ACH-05, ACH-08) — e os oito menores mais ACH-06, 07, 11, 13, 14, 15 e 17. **O bloco de impedimento deixou de ser montado a partir de duas fontes desencontradas** (ACH-07): quem tem as duas é a consulta, e ela entrega ao cartão um bloco já decidido — `motivo: null` tornou-se inalcançável. `agora` é resolvido **uma vez** e desce para todo cartão (ACH-11); os intervalos abertos são filtrados pelo episódio corrente (ACH-13); a ordem ganhou desempate por `id` (ACH-14); a varredura da lista de raias por cartão virou um `Set` montado uma vez (ACH-15); o nome ausente do responsável sai como rótulo e não como `null` (ACH-17). **ACH-06 fechou pela consulta e não pela borda:** `montar` lê o `seq` por JPQL escalar e **decide existência**, devolvendo `404` em vez de estourar `NullPointerException` três quadros adiante — `montar` é o modelo declarado de TASK-02.10, e a próxima chamada podia esquecer a guarda que mora no controlador. **A medição derrubou uma decisão da spec, e esse é o achado desta mão.** A regra única que o contrato passou a declarar — faixa sintética existe **quando e só quando** há cartão que precise dela — reprova `RaiasIT.semRaiaConfiguradaOBoardAindaDesenha`, da suíte congelada: projeto sem raia alguma e **sem cartão** ainda exige a faixa única. A suíte está certa e a regra é que estava larga demais: ali a sintética não é o lugar de quem não coube, é a grade inteira, porque raia é organização opcional (RN-023). O eixo da etapa não tem equivalente — projeto sem etapa não tem cartão. Registrado como ACH-04, destino `/techspec`. **Medição no contêiner de ADR-012: 198 testes, 117 verdes / 81 vermelhos — idêntica à base, zero regressão**, com os mesmos 2 arquivos de teste que não compilam e nenhum novo. **Nenhum verde a mais, e isso é esperado:** o que esta mão acrescentou — o campo do admin global, a etapa sintética, o recorte e o bloco conciliado — **não tem verificação na suíte congelada**, que é precisamente o ACH-04 da revisão e a mão do `/tests`. |

#### Achados

| # | Severidade | Local | Descrição | Destino |
| --- | --- | --- | --- | --- |
| ACH-01 | menor | `CartaoResposta.java`, `CriacaoDeTarefaService.java` | Arquivos **fora da tabela** desta task, alterados por necessidade: a suíte congelada exige `responsavel` como objeto `{id, nome}` e não como identificador solto, e o único call site de `CartaoResposta.de(...)` teve de acompanhar a assinatura nova. Precedente: `RaiaRepositorio.java` em TASK-02.5. | `/tasks` |
| ACH-02 | relevante | plano de tasks | **`GET /v1/tarefas/{tarefaId}` não é declarada por task nenhuma.** `state.md` e dezenas de testes vermelhos a tratam como parte da superfície de leitura desta task, mas ela não está na tabela de arquivos nem nos critérios — e é onde morre a maior parte dos 81 vermelhos remanescentes. Sem dono, a ficha da tarefa não é implementada por ninguém. | `/tasks` |
| ACH-03 | menor | texto desta task, linha "Consultas usadas" | Declara **quatro** consultas; a implementação usa **cinco**, porque etapa e raia são tabelas distintas. O critério de aceite fala em número que não cresce, e esse está satisfeito — a divergência é do texto. | `/tasks` |
| ACH-04 | relevante | `contracts/board-e-tarefas.md`, regra da faixa sintética | A regra escrita na emenda de 2026-09-16 — a faixa sintética existe **quando e só quando** há cartão que precise dela — **reprova a suíte congelada**: `RaiasIT.semRaiaConfiguradaOBoardAindaDesenha` (SCN-018.1) exige a faixa única num projeto sem raia configurada e **sem cartão nenhum**. Medido: com a regra literal, 4 testes que passavam ficam vermelhos. A suíte está certa — ali a sintética é a grade inteira e não o lugar de quem não coube (RN-023) —, e o contrato precisa distinguir os dois casos do eixo da raia. A implementação já os distingue e a base foi restabelecida; o que falta é a spec dizer o mesmo. | `/techspec` |
| ACH-05 | menor | `Condicao.java`, `ContagemDeTarefasAtivas.java`, `Tarefa.java`, migration de ordem 8 | Arquivos **fora da tabela** desta task, alterados por necessidade de SDR-007: a coluna projetada, o campo da entidade e o conjunto das condições terminais, que passou a morar em `Condicao` porque três consumidores o consultam e duas cópias divergiriam no dia em que nascer uma condição terminal nova — a divergência apareceria como cartão que some do board. Todos já acrescentados à tabela. | `/tasks` |
| ACH-06 | relevante | escrita de `tarefa.tornou_se_terminal_em` | **Esta task entrega a coluna, o índice, a população retroativa e o recorte na consulta — e nenhum escritor.** Conferido por leitura: `RegistradorDeEvento` não toca campo de estado de `tarefa`, e os serviços que concluem, encerram e reabrem tarefa nascem em EPIC-03/04. Até lá a coluna só tem o valor que a migration gravou, e a tarefa que concluir depois dela ficará com `null` — que a consulta trata como "mostrar", de propósito. **Gravar o instante no desfecho e anulá-lo na reabertura (RN-019) é obrigação nomeada daquelas tasks**, sob pena de RN-039 nunca passar a valer sem que nada acuse. | `/tasks` |
| ACH-07 | menor | `BoardController.java`, `TarefaController.java` | A sequência `404 → existsById → 403` é cópia linha a linha entre os dois controladores, e a segunda leitura do projeto convive com a leitura que `BoardQuery` já faz. Extrair a guarda para `internal/projeto` exigiria alterar `TarefaController.java`, que está fora da tabela desta task e cujo `404` é exercitado por classes congeladas — não cabe no escopo de arquivo revisado. | `/tasks` |

#### Fechamento dos achados

| # | Data | Como fechou |
| --- | --- | --- |
| ACH-01 | 2026-09-16 | `CartaoResposta.java` e `CriacaoDeTarefaService.java` entram na tabela de arquivos como **alterar**, com a razão escrita. Os dois são de TASK-02.5, e é lá que o cartão é fixado; o board o reusa, então a correção de forma cabe em quem a descobriu. |
| ACH-02 | 2026-09-16 | Criada **TASK-02.10 — ficha da tarefa**, que declara `GET /v1/tarefas/{tarefaId}` com contrato, escopo de arquivo e nove critérios. Não cabia nesta task: o board e a ficha são superfícies distintas, e a ficha carrega o log, que o board não tem. |
| ACH-03 | 2026-09-16 | Texto corrigido para as cinco consultas de projeção mais a leitura do projeto, com a raia sintética declarada junto — ela era decisão de desenho que não estava escrita em lugar nenhum. |
