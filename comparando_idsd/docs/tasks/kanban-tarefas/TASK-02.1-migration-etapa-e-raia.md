# TASK-02.1 — Migration 2 e entidades de etapa e raia

- **Status:** concluída
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-01.3 — as duas tabelas nascem com `REFERENCES projeto
  (id)`, e a migration que cria `projeto` e o anel de configuração é a de ordem
  1, dela (ACH-14 da revisão)
- **Cenários cobertos:** SCN-017.1
- **Origem:** RF-017, RF-018, RN-021, RN-023, modelo de dados §3

#### Contexto

A etapa é a primeira das três dimensões de estado da tarefa, e sua `ordem` é o
que define adjacência para a regra de transição. A identidade da etapa precisa
ser estável: renomear não pode afetar o histórico, porque a série de tempo segue
o identificador, e não o nome.

#### O que deve ser feito

- [x] Criar a migration de ordem 2 com as tabelas `etapa` e `raia`.
- [x] Criar o índice único parcial de ordem por projeto.
- [x] Criar as entidades JPA e os repositórios.
- [x] Implementar remoção **lógica** por `arquivada_em`; não existe remoção
      física de etapa nem de raia. A ausência precisa ser real na interface, e
      não apenas afirmada no javadoc: os repositórios estendem
      `org.springframework.data.repository.Repository`, que é marcadora e só
      publica o que for declarado. `JpaRepository` publicaria sete assinaturas
      de remoção física (ACH-01 da revisão).

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/resources/db/migration/V<ANO><MES><DIA><HORA>__etapa_e_raia.sql` | criar | tabelas e índice |
| `backend/src/main/java/br/com/idsd/kanban/internal/projeto/Etapa.java` | criar | entidade |
| `backend/src/main/java/br/com/idsd/kanban/internal/projeto/Raia.java` | criar | entidade |
| repositórios de etapa e raia | criar | interfaces |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

`etapa`:

| Campo | Tipo | Notas |
| --- | --- | --- |
| `id` | `uuid` PK | identidade estável; renomear não afeta o histórico |
| `projeto_id` | `uuid` FK NOT NULL | |
| `nome` | `text` NOT NULL | mutável |
| `ordem` | `integer` NOT NULL | define adjacência; única por projeto entre etapas não arquivadas |
| `terminal` | `boolean` NOT NULL DEFAULT false | ao menos uma por projeto |
| `arquivada_em` | `timestamptz` NULL | remoção é lógica |

`raia`: `id` (`uuid` PK), `projeto_id` (`uuid` FK NOT NULL), `nome` (`text` NOT
NULL), `ordem` (`integer` NOT NULL), `arquivada_em` (`timestamptz` NULL).

Índice obrigatório **em `etapa`, e só nela**: `(projeto_id, ordem)` **único
parcial** em `arquivada_em IS NULL`. `raia` não recebe restrição de ordem.

#### Guia técnico — pontos de atenção

- **Apagar etapa fisicamente destruiria a série de tempo.** Histórico e
  intervalos referenciam a etapa para sempre. A remoção é o preenchimento de
  `arquivada_em`.
- **Renomear preserva o identificador.** Alteração de configuração vale dali em
  diante e nunca reescreve o histórico.
- **A garantia da raia é estreita, e a versão generalizada é falsa.**
  `tarefa` **carrega** `raia_id` — a raia é o agrupamento visual do cartão
  (`data-model.md` §5, `board-e-tarefas.md`, e a congelada `RaiasIT` monta
  tarefa com raia). O que nenhuma tabela carrega é raia na **série de tempo**:
  não acrescente `raia_id` a `intervalo_tarefa` nem a `evento_tarefa`, e nenhuma
  rota agregada aceita filtro por raia. É essa ausência estreita que realiza
  RN-023, porque agregar por raia exigiria juntar a série de tempo ao estado
  corrente, que não diz em que raia a tarefa estava quando o intervalo correu.
  Corrigido em 2026-09-14 (ACH-02 / TechSpec v1.11); a redação anterior
  afirmava que **nenhuma** tabela do anel de projeção referencia `raia`.
- **A ordem da raia não é única.** A restrição criada aqui não tinha origem em
  `data-model.md` §6 nem `422` correspondente no contrato, e foi removida na
  migration de correção (ACH-06). Unicidade de ordem é invariante de `etapa`,
  onde a adjacência de RN-005 depende dela.
- A recusa de arquivar etapa que contém tarefas ativas é regra de serviço, e
  chega na task seguinte: o banco não a expressa porque a condição envolve
  contagem, não integridade referencial.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | A migration aplica em banco limpo pelo serviço de migração | execução do serviço `migracao` |
| 2 | Duas etapas ativas não compartilham a mesma ordem no projeto | inserção duplicada viola o índice |
| 3 | Etapa arquivada libera a ordem para outra etapa | preencher `arquivada_em` e inserir nova etapa com a mesma ordem |
| 4 | Renomear a etapa preserva o identificador **e a série de tempo continua apontando para ela** | após o `UPDATE` de `nome`, o `id` é o mesmo **e** a linha permanece alcançável pelas referências que a apontavam — a asserção é sobre a série sobreviver ao rename, não sobre a coluna `id` não mudar sozinha num `UPDATE` que não a menciona (ACH-05) |
| 5 | A série de tempo não carrega raia | `intervalo_tarefa` e `evento_tarefa` não têm coluna de raia, e nenhuma rota agregada aceita `raiaId`. **Não** é varredura de `%raia%` no esquema inteiro: `tarefa.raia_id` existe por especificação e nasce em TASK-02.3, de modo que a varredura antiga era vazia hoje e passaria a ser falsa lá, instruindo a próxima task a violar a spec (ACH-03) |
| 6 | O esquema tem cobertura automatizada | `EsquemaDoFluxoIT` afirma contra o catálogo o índice único **parcial**, a ausência de restrição de ordem em `raia`, o `NOT NULL`/`DEFAULT false` de `terminal` e as FKs para `projeto`; `ddl-auto=validate` não vê nada disso (ACH-08) |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
| 2026-09-11 | Red medido | `mvn -o test-compile` falha nos mesmos **cinco** arquivos de tasks posteriores da linha de base — `ReconstrucaoDaProjecaoIT`, `ImpedimentoServiceTest`, `EtapaServiceTest`, `CriacaoDeTarefaServiceTest`, `TomadaServiceTest`. Red por compilacao, como nas tasks anteriores do produto |
| 2026-09-11 | implementacao | Migration `V2026091111__etapa_e_raia.sql`, entidades `Etapa` e `Raia`, repositorios `EtapaRepositorio` e `RaiaRepositorio` |
| 2026-09-11 | decisao | **O indice de leitura do fluxo vigente nao foi criado, de proposito.** O unico parcial `(projeto_id, ordem) WHERE arquivada_em IS NULL` **e** o caminho de leitura do board: um indice separado com o mesmo prefixo nao aceleraria consulta nenhuma e cobraria em toda escrita. E o mesmo raciocinio de ACH-15 da revisao de TASK-01.5, e a dependencia ficou escrita na migration junto do gatilho que a torna falsa — reordenar a restricao para `(ordem, projeto_id)` degrada a consulta mais quente do board e obriga a criar o indice no mesmo passo |
| 2026-09-11 | decisao | Nenhum metodo de remocao nos dois repositorios, e a ausencia esta declarada no javadoc: a remocao e logica pelo `arquivada_em`, e um `delete` exposto na interface e tudo de que alguem precisaria para destruir a serie de tempo por etapa sem que teste nenhum ficasse vermelho |
| 2026-09-11 | achado | **O nome `EtapaRepositorio` diverge do `...Repository` dos tres repositorios ja existentes, e a divergencia nao e escolha**: a suite congelada (`EtapaServiceTest`) declara `EtapaRepositorio` como colaborador de `EtapaService`, e ela esta fora do alcance de quem implementa. `RaiaRepositorio` seguiu o mesmo nome para que o par nao ficasse dividido entre duas convencoes. Dono `/tests` se algum dia se quiser uniformizar; nao bloqueia nada |
| 2026-09-11 | achado | **SCN-017.1 nao pode ficar verde nesta task.** Os testes que o cobrem — `EtapaServiceTest` e `ConfiguracaoDoFluxoIT` — exercitam `EtapaService`, `FluxoRequisicao` e as rotas de etapa, que sao arquivos declarados de TASK-02.2. Esta task entrega o esquema e o mapeamento, e os cinco criterios de aceite foram medidos por execucao direta contra o banco, nao pelo cenario. Mesma assimetria de TASK-01.3, TASK-01.5 e TASK-01.8 — dono `/tasks` |
| 2026-09-11 | achado | `substituirFluxo(projetoId, ...)` e nomeado pela suite congelada como metodo de `EtapaRepositorio`, com dois argumentos, e o segundo e `FluxoRequisicao.EtapaDesejada`, tipo que nasce em TASK-02.2. Declara-lo aqui nao compilaria; o metodo entra com o consumidor, e isso e desvio da tabela de arquivos **de TASK-02.2**, nao desta |
| 2026-09-11 | verificacao | Os cinco criterios medidos por execucao. (1) `migracao` aplicou as duas migrations em **banco limpo** descartavel e a de ordem 2 no banco de desenvolvimento, incremental. (2) segunda etapa ativa com a mesma ordem recusada por `etapa_projeto_ordem_unico`. (3) preenchido `arquivada_em`, a mesma insercao passa — a ordem foi liberada. (4) `UPDATE` de `nome` com o `id` inalterado. (5) varredura de `information_schema.columns` por `%raia%` devolve **zero** colunas: nao ha `raia_id` em tabela nenhuma. Alem deles, o backend subiu **`healthy` com `ddl-auto=validate`** contra o schema recem-aplicado, que e a unica prova de que as duas entidades novas correspondem as duas tabelas |
| 2026-09-14 | correcao | Os doze achados com dono `/implement` da revisao fechados. **ACH-01** trocou a base dos dois repositorios de `JpaRepository` para `org.springframework.data.repository.Repository`, que e marcadora: a ausencia de remocao fisica deixa de ser afirmacao de javadoc e passa a ser propriedade da interface. **ACH-13** removeu o finder de `RaiaRepositorio`, que nao tinha consumidor ate TASK-06.1 — a interface ficou vazia, aplicando a `raia` a regra que `EtapaRepositorio` institui na mesma entrega. **ACH-11** tornou `arquivar` idempotente nas duas entidades: o instante de saida e dado da serie de tempo, e reatribui-lo moveria para frente o momento em que a etapa deixou o fluxo sem que nada registrasse. **ACH-12** pos guardas de `nome` em branco e `ordem` negativa nos construtores e em `reconfigurar` — o `NOT NULL` da coluna nao pega branco. **ACH-10** corrigiu as citacoes de regra no javadoc de `Etapa` (eram RN-023, que e a raia, e RN-021, que e o rename; a adjacencia e RN-005) |
| 2026-09-14 | decisao | **O SQL nao foi corrigido no lugar: entrou migration nova.** O cabecalho da propria `V2026091111` institui que migration aplicada nunca e alterada, e editar o arquivo quebraria o checksum do Flyway contra todo banco ja migrado. `V2026091412__correcoes_etapa_e_raia.sql` faz uma coisa executavel e tres declarativas: derruba `raia_projeto_ordem_unico` (**ACH-06**) e poe a verdade no catalogo por `COMMENT ON` (**ACH-02**, **ACH-07**, **ACH-10**), que e onde ela fica alcancavel por quem inspeciona o esquema em vez de ler o historico. A remocao do indice da raia nao e simetria invertida: a ordem de `etapa` e invariante porque RN-005 depende dela, e a raia e agrupamento livre sem semantica fixa (RN-023) — ordem empatada ali desempata exibicao, nunca uma transicao. Ela tambem elimina, para a rota de raias, o problema de reordenacao em lote que ACH-07 descreve |
| 2026-09-14 | decisao | **ACH-09 fechou por declaracao e nao por codigo**, com o criterio 10 acrescentado a TASK-02.3. A aplicacao conecta como o dono do schema, de modo que `REVOKE DELETE` aqui seria teatro — exatamente a classe de defeito que esta revisao inteira acusa, com o agravante de parecer resolvido. A concessao restrita a role de aplicacao e conteudo declarado da migration de ordem 4, e e la que `etapa` e `raia` entram junto de `evento_tarefa`. A razao ficou escrita na migration nova, para que a ausencia nao seja indistinguivel de esquecimento. Precedente: ACH-07/ACH-09 da revisao de TASK-01.8 |
| 2026-09-14 | verificacao | **ACH-08** — `alem/EsquemaDoFluxoIT` da ao esquema a cobertura que ele nao tinha: `ddl-auto=validate` so confere o que a entidade **declara**, e a entidade nao declara indice, valor padrao nem FK, entao remover qualquer um deixava a suite inteira verde. Cinco asserções, sobre a **definicao** no catalogo e nao sobre o comportamento de uma insercao — insercao que falha prova que alguma restricao recusou, nao qual. A excecao e o indice parcial, afirmado pelas duas metades, porque e a metade "parcial" que uma restricao total quebraria sem mudar o nome do indice. **Poder de falha provado** em copia descartavel: tirando o predicado do indice, o `DEFAULT false`, o `NOT NULL` de `terminal`, as FKs e o `DROP INDEX` da raia, as cinco ficam vermelhas |
| 2026-09-14 | verificacao | Medido em copia descartavel fora do repositorio, excluindo os cinco arquivos de tasks posteriores que nao compilam: **163 testes, 64 verdes / 99 vermelhos**, contra a linha de base de 158 (59/99). O delta e exatamente `EsquemaDoFluxoIT`, e os 99 vermelhos sao identicos — nenhuma regressao. Nenhum `.feature` nem step definition tocado. `check_escopo.py` acusa os mesmos erros de prefixo da pendencia 12 |
| 2026-09-14 | achado | **Os criterios 4 e 5 foram reescritos e nao apenas remarcados** (ACH-05, ACH-03). O 4 media "renomear preserva o `id`" por um `UPDATE` que nao menciona `id`, e passaria identico contra qualquer esquema concebivel; passou a afirmar que a serie de tempo sobrevive ao rename, que e o que RN-021 protege. O 5 media a garantia da raia por varredura `%raia%` no esquema inteiro — vazia hoje porque so existem as duas tabelas novas, e **falsa** a partir de TASK-02.3, que cria `tarefa.raia_id` porque a TechSpec manda criar: o criterio instruia a proxima task a violar a spec. Passou a afirmar a garantia estreita — a serie de tempo nao carrega raia |
| 2026-09-11 | achado de ambiente | O servico `migracao` foi invocado sem as variaveis de porta com que a stack subiu, e o Compose **recriou** o conteiner do banco tentando publicar a 5432, que outra stack da maquina ja ocupa — o banco ficou fora do ar ate ser restaurado com `PORTA_BANCO=5433`. Os dados sobreviveram por estarem em volume nomeado. E a pendencia 16 pelo outro lado: variavel de porta nao amarrada faz `compose run` de um servico pontual reconfigurar um servico que ja estava de pe |
