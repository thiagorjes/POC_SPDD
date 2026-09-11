# TASK-02.1 — Migration 2 e entidades de etapa e raia

- **Status:** concluída
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** nenhuma
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
      física de etapa nem de raia.

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

Índice obrigatório: `(projeto_id, ordem)` **único parcial** em
`arquivada_em IS NULL`.

#### Guia técnico — pontos de atenção

- **Apagar etapa fisicamente destruiria a série de tempo.** Histórico e
  intervalos referenciam a etapa para sempre. A remoção é o preenchimento de
  `arquivada_em`.
- **Renomear preserva o identificador.** Alteração de configuração vale dali em
  diante e nunca reescreve o histórico.
- **Nenhuma tabela do anel de projeção referencia `raia`.** A raia não restringe
  transição e não entra em agregação, e é essa ausência no esquema que garante a
  regra. Não acrescente `raia_id` a `intervalo_tarefa`.
- A recusa de arquivar etapa que contém tarefas ativas é regra de serviço, e
  chega na task seguinte: o banco não a expressa porque a condição envolve
  contagem, não integridade referencial.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | A migration aplica em banco limpo pelo serviço de migração | execução do serviço `migracao` |
| 2 | Duas etapas ativas não compartilham a mesma ordem no projeto | inserção duplicada viola o índice |
| 3 | Etapa arquivada libera a ordem para outra etapa | preencher `arquivada_em` e inserir nova etapa com a mesma ordem |
| 4 | Renomear a etapa preserva o identificador | atualização de `nome` sem alteração de `id` |
| 5 | Não existe coluna de raia em nenhuma tabela fora do anel de configuração | inspeção do esquema |

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
| 2026-09-11 | achado de ambiente | O servico `migracao` foi invocado sem as variaveis de porta com que a stack subiu, e o Compose **recriou** o conteiner do banco tentando publicar a 5432, que outra stack da maquina ja ocupa — o banco ficou fora do ar ate ser restaurado com `PORTA_BANCO=5433`. Os dados sobreviveram por estarem em volume nomeado. E a pendencia 16 pelo outro lado: variavel de porta nao amarrada faz `compose run` de um servico pontual reconfigurar um servico que ja estava de pe |
