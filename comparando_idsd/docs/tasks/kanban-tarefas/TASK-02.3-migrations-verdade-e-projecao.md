# TASK-02.3 — Migrations 3 a 6 do anel de verdade e da projeção

- **Status:** pendente
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

- [ ] Criar a migration de ordem 3: `tarefa`, restrição de verificação sobre
      `condicao`, índices do board e da fila.
- [ ] Criar a migration de ordem 4: `evento_tarefa` e a concessão restrita de
      `SELECT, INSERT` à role de aplicação.
- [ ] Criar a migration de ordem 5: `intervalo_tarefa`, `impedimento` e os
      índices únicos parciais.
- [ ] Criar a migration de ordem 6: unicidade de `(projeto_id, seq)` e a coluna
      gerada `duracao` com seu índice.
- [ ] Criar as entidades JPA e os repositórios correspondentes.
- [ ] Não expor em repositório nenhum método de atualização ou remoção sobre
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

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
