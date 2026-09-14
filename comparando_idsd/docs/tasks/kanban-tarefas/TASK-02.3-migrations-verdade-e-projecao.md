# TASK-02.3 — Migrations 3 a 6 do anel de verdade e da projeção

- **Status:** implementada sem medição — ver histórico
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-02.1
- **Cenários cobertos:** SCN-004.1
- **Origem:** SDR-001, SDR-004, RN-008, RN-014, modelo de dados §4 a §6 e §8

#### Contexto

O modelo tem três anéis com regras de escrita diferentes, e confundi-los é o
erro que a imutabilidade do histórico existe para impedir. Esta task cria o anel
de verdade — log somente de inserção — e o anel de projeção, derivado dele e
reconstruível. Várias regras de negócio deixam de ser disciplina de quem escreve
o serviço e passam a ser propriedade do esquema; é isso que torna esta migration
a peça mais sensível do sistema.

#### O que deve ser feito

- [x] Criar a migration de ordem 3: `tarefa`, restrição de verificação sobre
      `condicao`, índices do board e da fila.
- [x] Criar a migration de ordem 4: `evento_tarefa` e a concessão restrita de
      `SELECT, INSERT` à role de aplicação.
- [x] Criar a migration de ordem 5: `intervalo_tarefa`, `impedimento` e os
      índices únicos parciais.
- [x] Criar a migration de ordem 6: unicidade de `(projeto_id, seq)` e a coluna
      gerada `duracao` com seu índice.
- [x] Criar as entidades JPA e os repositórios correspondentes.
- [x] Não expor em repositório nenhum método de atualização ou remoção sobre
      `evento_tarefa`.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `.../db/migration/V<...>__tarefa.sql` | criar | ordem 3 |
| `.../db/migration/V<...>__evento_tarefa.sql` | criar | ordem 4, com a concessão restrita |
| `.../db/migration/V<...>__intervalo_e_impedimento.sql` | criar | ordem 5 |
| `.../db/migration/V<...>__sequencia_e_duracao.sql` | criar | ordem 6 |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/Tarefa.java` | criar | entidade, com controle de versão |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/EventoTarefa.java` | criar | entidade somente de inserção |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/Impedimento.java` | criar | entidade |
| `backend/src/main/java/br/com/idsd/kanban/internal/tempo/IntervaloTarefa.java` | criar | entidade |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas, e todo arquivo de verificação já
produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

`tarefa` — estado corrente:

| Campo | Tipo | Notas |
| --- | --- | --- |
| `id` | `uuid` PK | |
| `projeto_id` | `uuid` FK NOT NULL | |
| `titulo` | `text` NOT NULL | obrigatório |
| `descricao` | `text` | |
| `etapa_id` | `uuid` FK NOT NULL | dimensão 1 |
| `raia_id` | `uuid` FK NULL | |
| `condicao` | `text` NOT NULL | dimensão 2 |
| `responsavel_id` | `uuid` FK NULL | nulo quando aguardando tomada |
| `assumida_em` | `timestamptz` NULL | |
| `episodio_atual` | `integer` NOT NULL DEFAULT 1 | |
| `versao` | `bigint` NOT NULL | bloqueio otimista |
| `criada_em` | `timestamptz` NOT NULL | |

Domínio de `condicao`, com restrição de verificação no banco além da enumeração
em código: `AGUARDANDO_TOMADA`, `EM_CURSO`, `CONCLUIDA`,
`ENCERRADA_SEM_CONCLUSAO`. **`IMPEDIDA` não pertence a este domínio.**

**Não existe coluna de impedimento em `tarefa`.** A dimensão 3 é derivada da
existência de linha em `impedimento` com `desfecho IS NULL`.

`evento_tarefa` — log somente de inserção:

| Campo | Tipo | Notas |
| --- | --- | --- |
| `id` | `bigserial` PK | ordem total de gravação |
| `tarefa_id` | `uuid` NOT NULL | |
| `projeto_id` | `uuid` NOT NULL | desnormalizado; todo filtro parte do projeto |
| `tipo` | `text` NOT NULL | catálogo fechado |
| `ocorrido_em` | `timestamptz` NOT NULL | |
| `ator_id` | `uuid` NULL | nulo em evento decorrente de configuração |
| `episodio` | `integer` NOT NULL | 1 na criação |
| `etapa_origem_id` | `uuid` NULL | |
| `etapa_destino_id` | `uuid` NULL | |
| `condicao_origem` | `text` NULL | |
| `condicao_destino` | `text` NULL | |
| `seq` | `bigint` NOT NULL | sequência por projeto; único por `(projeto_id, seq)` |
| `dados` | `jsonb` NULL | motivo, desfecho, título na criação |

`intervalo_tarefa` — as três séries:

| Campo | Tipo | Notas |
| --- | --- | --- |
| `id` | `bigserial` PK | |
| `tarefa_id` | `uuid` NOT NULL | |
| `projeto_id` | `uuid` NOT NULL | |
| `etapa_id` | `uuid` NOT NULL | etapa vigente na **abertura** do intervalo |
| `tipo` | `text` NOT NULL | `PERMANENCIA`, `ESPERA_TOMADA`, `IMPEDIMENTO` |
| `episodio` | `integer` NOT NULL | |
| `inicio` | `timestamptz` NOT NULL | |
| `fim` | `timestamptz` NULL | nulo enquanto em curso |
| `duracao` | `interval` GENERATED ALWAYS AS (`fim - inicio`) STORED | indexável |

`impedimento`: `id` (`uuid` PK), `tarefa_id` (`uuid` NOT NULL), `projeto_id`
(`uuid` NOT NULL), `intervalo_id` (`bigint` FK NOT NULL, aponta para o intervalo
de tipo `IMPEDIMENTO`), `motivo` (`text` NOT NULL), `anotacoes` (`jsonb` NOT NULL
DEFAULT `[]`), `aberto_por` (`uuid` FK NOT NULL), `desfecho` (`text` NULL),
`resolvido_por` (`uuid` FK NULL).

Índices obrigatórios:

| Índice | Tabela |
| --- | --- |
| `(projeto_id, etapa_id, condicao)` | `tarefa` |
| `(condicao, projeto_id)` parcial em `AGUARDANDO_TOMADA` | `tarefa` |
| `(responsavel_id)` | `tarefa` |
| `(projeto_id, etapa_id, tipo, inicio)` | `intervalo_tarefa` |
| `(projeto_id, etapa_id, tipo, duracao)` parcial em `fim IS NOT NULL` | `intervalo_tarefa` |
| `(tarefa_id, tipo)` **único** parcial em `fim IS NULL` | `intervalo_tarefa` |
| `(tarefa_id, episodio)` | `intervalo_tarefa` |
| `(tarefa_id)` **único** parcial em `desfecho IS NULL` | `impedimento` |
| `(projeto_id)` parcial em `desfecho IS NULL`, **não único** | `impedimento` |
| `(projeto_id, seq)` **único** | `evento_tarefa` |
| `(tarefa_id, id)` | `evento_tarefa` |

#### Guia técnico — pontos de atenção

- **Não há coluna de pessoa em `intervalo_tarefa`, e não haverá.** Não a
  acrescente "para depois filtrar": a ausência é o que torna estrutural a
  proibição de agregar tempo por pessoa, e nenhum teste falha quando alguém
  acrescenta um filtro.
- **Não há coluna de total.** As três séries nunca se somam, e uma coluna de
  soma seria o convite a fazê-lo.
- **Não crie gatilho de notificação no banco.** A publicação é feita pela
  aplicação após o commit; dois publicadores para o mesmo evento fariam cada
  mudança gerar dois envios, e o cliente contabilizaria sequência repetida como
  estado inconsistente.
- **A role de aplicação recebe apenas `SELECT, INSERT` em `evento_tarefa`.** A
  garantia é dupla: nenhum método de repositório expõe atualização ou remoção, e
  o banco recusa.
- **`dados` nunca recebe dado real de cliente.**
- **Um mesmo instante pode ter até três intervalos abertos da mesma tarefa.**
  Não existe restrição de não sobreposição entre tipos diferentes; existe, sim,
  no máximo um intervalo aberto **por tipo** por tarefa.
- **O instantâneo de etapa no intervalo de impedimento é necessário.** Sem ele,
  o bloco de impedimento por etapa das consultas agregadas cairia num único
  grupo nulo. Ele não fere a regra que proíbe a movimentação encerrar ou
  reiniciar a contagem.
- **A coluna de contador de sequência já existe em `projeto`** desde a primeira
  migration; a de ordem 6 acrescenta a unicidade e a coluna gerada.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | As quatro migrations aplicam em banco limpo, na ordem | execução do serviço `migracao` |
| 2 | O backend valida o schema sem divergência | contexto sobe com `ddl-auto=validate` |
| 3 | `condicao` recusa valor fora do domínio, inclusive `IMPEDIDA` | inserção direta viola a restrição |
| 4 | A role de aplicação não consegue atualizar nem apagar evento | tentativa por cada caminho exposto é recusada |
| 5 | Duas linhas de impedimento aberto para a mesma tarefa são impossíveis | inserção duplicada viola o índice único parcial |
| 6 | Dois intervalos abertos do mesmo tipo na mesma tarefa são impossíveis | idem |
| 7 | Três intervalos abertos de tipos diferentes na mesma tarefa são possíveis | inserção dos três tipos com `fim` nulo |
| 8 | Não existe coluna de pessoa nem de total em `intervalo_tarefa` | inspeção do esquema |
| 9 | Não existe gatilho de notificação no banco | inspeção dos gatilhos do esquema |
| 10 | A role de aplicação também não consegue apagar `etapa` nem `raia` | o `REVOKE DELETE` da migration de concessão restrita (`data-model.md` §8, ordem 4) cobre `etapa` e `raia` junto de `evento_tarefa`, e a tentativa por cada caminho exposto é recusada. Vem de ACH-09 da revisão de TASK-02.1: a remoção das duas é **lógica** por `arquivada_em`, e apagar a linha destruiria a série de tempo por etapa que RF-016 existe para produzir. Não foi feito lá porque a aplicação conecta como o dono do schema, e `REVOKE` contra o dono não surte efeito — é aqui, junto da role de aplicação distinta, que a garantia deixa de ser de um lado só |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
| 2026-09-14 | tentativa 1 — implementação | As quatro migrations (`V2026091413` a `V2026091416`), as quatro entidades e quatro repositórios. Nenhuma medição: ver a linha de ambiente abaixo |
| 2026-09-14 | divergência — `impedimento.resolvido_em` | A coluna **não** consta da tabela de campos desta task nem de `data-model.md` §5, e entrou porque a suíte congelada a exige: `ImpedimentoServiceTest` chama `setResolvidoEm`/`getResolvidoEm` para verificar que a resolução repetida de SCN-010.3 não reescreve o instante já registrado. Sem ela a suíte não compila; com ela, `ddl-auto=validate` exige a coluna no esquema. Dono da correção da spec: `/techspec` |
| 2026-09-14 | costura fixada pela suíte | `Tarefa` e `Impedimento` nascem **JavaBean** — construtor público sem argumentos e acessores de escrita —, e não pelo padrão de `Etapa`/`Raia`, que constroem por construtor e só mudam por método de domínio. `TomadaServiceTest` e `ImpedimentoServiceTest` montam as duas com `new` e atribuem campo a campo. O plano de verificação declara a costura de `TomadaService` e `ImpedimentoService` como restrição de TASK-03.5 e TASK-04.2; a forma das **entidades** que esses testes manipulam não está naquela tabela, e é restrição desta task. `Condicao` também é fixada ali (`Condicao.EM_CURSO`, `Condicao.AGUARDANDO_TOMADA`) |
| 2026-09-14 | **ACH — critérios 4 e 10 não são satisfeitos, e não podem ser daqui** | A concessão restrita está escrita na migration de ordem 4 — grupo `aplicacao_kanban` sem login, `GRANT SELECT, INSERT` no log, `REVOKE UPDATE, DELETE, TRUNCATE` dele e `REVOKE DELETE, TRUNCATE` de `etapa` e `raia` —, mas **ela é inerte hoje**. A aplicação conecta com `BANCO_USUARIO`, que no compose e no Testcontainers é o `POSTGRES_USER` da imagem, isto é, o superusuário de bootstrap e dono do schema: superusuário ignora privilégio, e dono reconcede a si mesmo. É exatamente o que ACH-09 da revisão de TASK-02.1 antecipou ao recusar fazer o `REVOKE` lá — o que a task supunha resolvido "junto da role de aplicação distinta" é que **essa role continua não existindo**. Criá-la, montar seu segredo e apontar `BANCO_USUARIO` para ela é mudança de `docker/compose.yaml` e do arranjo de segredos, fora do escopo de arquivo declarado. Dono: `/tasks` (infra) |
| 2026-09-14 | **ACH — a suíte congelada contradiz a si mesma neste ponto** | `ImutabilidadeDoLogIT` exige que `TRUNCATE evento_tarefa` **falhe** para a credencial da aplicação; `TesteDeIntegracao.esvaziarBanco` — suporte comum, também congelado — faz `truncate table <todas as tabelas públicas>` com **a mesma credencial** antes de cada teste. Não há configuração de privilégio que satisfaça as duas: negar o truncate deixa toda a suíte de integração vermelha na primeira linha do `@BeforeEach`. Enquanto a aplicação for superusuário, as duas convivem porque nenhuma das duas é exercida de verdade — e é isso que torna a contradição invisível. Resolvê-la exige role de aplicação distinta **e** uma decisão sobre por qual credencial o suporte limpa o banco. Dono: `/tests`, com o anterior |
| 2026-09-14 | decisão — `duracao` fora do mapeamento JPA | A coluna gerada existe no banco e **não** é mapeada em `IntervaloTarefa`. Não há consumidor antes de RF-016, que a lê por consulta agregada; e mapear `interval` exigiria escolher uma conversão para tipo Java que `ddl-auto=validate` aceitasse, decisão sem medição e sem leitor. Coluna presente no banco e ausente do mapeamento não reprova a validação |
| 2026-09-14 | decisão — `tipo` como texto em `evento_tarefa` e `intervalo_tarefa` | Sem enumeração em código e sem restrição de verificação na coluna, pela mesma razão: o catálogo de eventos cresce a cada épico, nenhum caminho grava evento ou intervalo ainda, e antecipar o tipo fechado criaria valores sem escritor — mais uma migration sobre a tabela mais sensível do sistema a cada tipo novo. O enum nasce com a primeira escrita, em EPIC-03. `condicao` é o caso oposto e ganhou as duas travas: o domínio é fechado por RN-003 e a suíte congelada já o lê do catálogo do banco |
| 2026-09-14 | **ambiente — nada foi executado** | Não há `mvn`, `mvnw`, `docker` nem JDK nesta máquina, e o git recusa o repositório por *dubious ownership* (`D:/DEV/Projects/SPDD_puro` pertence a outro usuário do Windows), de modo que `check_escopo.py` não roda e não há commit. **Nenhum dos dez critérios de aceite foi medido** — em particular 1 (as migrations aplicam), 2 (`ddl-auto=validate` sem divergência) e 3, 5, 6, 7 (as restrições recusam o que devem). Os pontos de maior risco à validação são os dois `jsonb` mapeados por `@JdbcTypeCode(SqlTypes.JSON)` e o `bigserial` como `GenerationType.IDENTITY`. Medir é pré-condição do `/code-review` |
