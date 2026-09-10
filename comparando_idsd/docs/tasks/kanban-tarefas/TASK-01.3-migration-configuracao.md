# TASK-01.3 — Migration 1 e entidades de usuário, projeto e participação

- **Status:** pendente
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-01.2
- **Cenários cobertos:** SCN-002.1
- **Origem:** RF-002, RN-015, BDR-001, BDR-002, modelo de dados §3 e §8

#### Contexto

O anel de configuração é a base de autorização de todo o sistema: participação
por projeto com papéis acumuláveis, e o catálogo de papéis fechado em código
para que ninguém componha permissão nova em runtime. Esta task cria a primeira
migration e as entidades correspondentes.

#### O que deve ser feito

- [ ] Criar a migration de ordem 1 com as tabelas `usuario`, `projeto`,
      `participacao` e `participacao_papel`, e seus índices.
- [ ] Criar as entidades JPA e os repositórios correspondentes.
- [ ] Definir o catálogo de papéis como **enumeração em código**, nunca tabela.
- [ ] Implementar o resolvedor de permissões a partir dos papéis da
      participação real.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/resources/db/migration/V<ANO><MES><DIA><HORA>__configuracao_base.sql` | criar | tabelas e índices desta task |
| `backend/src/main/java/<pkg>/internal/acesso/Usuario.java` | criar | entidade |
| `backend/src/main/java/<pkg>/internal/projeto/Projeto.java` | criar | entidade |
| `backend/src/main/java/<pkg>/internal/projeto/Participacao.java` | criar | entidade |
| `backend/src/main/java/<pkg>/internal/projeto/Papel.java` | criar | enumeração fechada |
| `backend/src/main/java/<pkg>/internal/projeto/Permissao.java` | criar | enumeração derivada dos papéis |
| `backend/src/main/java/<pkg>/internal/projeto/ResolvedorDePermissao.java` | criar | serviço |
| repositórios dos quatro agregados | criar | interfaces de repositório |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, migrations já aplicadas.

#### Guia técnico — padrão a seguir

Nomenclatura de migration: `V<ANO><MES><DIA><HORA>__descricao_clara.sql`.
Migration aplicada **nunca** é alterada — corrige-se com uma nova.

Colunas de `usuario`:

| Campo | Tipo | Notas |
| --- | --- | --- |
| `id` | `uuid` PK | |
| `subject_id` | `text` UNIQUE NOT NULL | identificador do token do provedor; chave de vínculo, nunca o e-mail |
| `nome` | `text` NOT NULL | espelhado do token a cada entrada |
| `email` | `text` NOT NULL | idem |
| `admin_global` | `boolean` NOT NULL DEFAULT false | fora do vínculo por projeto |
| `criado_em` | `timestamptz` NOT NULL | autoprovisionamento na primeira entrada |

Não há senha e não há cadastro local.

Colunas de `projeto`: `id` (`uuid` PK), `nome` (`text` NOT NULL), `descricao`
(`text`), `criado_em` (`timestamptz` NOT NULL), `seq_atual` (`bigint` NOT NULL
DEFAULT 0 — o contador de sequência de eventos do projeto, que uma task
posterior passa a usar).

`participacao`: `id` (`uuid` PK), `usuario_id` (`uuid` FK NOT NULL),
`projeto_id` (`uuid` FK NOT NULL), `criada_em` (`timestamptz` NOT NULL),
UNIQUE `(usuario_id, projeto_id)`.

`participacao_papel`: `participacao_id` (`uuid` FK NOT NULL), `papel` (`text`
NOT NULL), PK `(participacao_id, papel)` — papéis acumuláveis no mesmo projeto.

Índice obrigatório: `(usuario_id, projeto_id)` único em `participacao`.

Catálogo fechado de papéis e permissões:

| Papel | Ler board | Escrever tarefa | Desbloquear | Encerrar sem conclusão | Reabrir | Configurar projeto |
| --- | --- | --- | --- | --- | --- | --- |
| `project_admin` | sim | sim | sim | sim | não | sim |
| `product_owner` | sim | sim | sim | sim | sim | não |
| `dev` | sim | sim | não | não | não | não |
| `gestor` | sim | não | não | não | não | não |
| `user` | não | não | não | não | não | não |

- `gestor` é somente-leitura: não recebe ação de escrita na interface e é
  recusado se solicitar por outro caminho.
- Encerrar sem conclusão exige permissão de configuração. É a única linha em que
  `product_owner` a tem sem administrar o projeto.
- Reabrir é privativo de `product_owner`.
- `admin_global` não aparece na tabela porque não é vínculo de projeto.

#### Guia técnico — pontos de atenção

- **O contador de sequência vive em `projeto`, não em sequência do banco.**
  Sequência do PostgreSQL não é transacional: transação revertida deixaria
  buraco permanente e todo cliente resincronizaria para sempre. A coluna nasce
  aqui mesmo sem uso, para que a migration que a introduz não seja alterada
  depois.
- **O vínculo é pelo identificador de sujeito do token, jamais pelo e-mail.**
  O e-mail é mutável no provedor e, em realm com autocadastro, atribuível por
  quem se registra.
- **Papel como tabela abriria composição de permissão em runtime.** A
  enumeração é o que impede isso.
- **Permissão é resolvida no serviço, sobre a participação real** — nunca sobre
  papel ou identificador vindos do cliente.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | A migration aplica em banco limpo pelo serviço de migração | `docker compose -f docker/compose.yaml up migracao` termina com sucesso |
| 2 | O backend sobe com validação de schema contra a migration aplicada | contexto Spring sobe com `ddl-auto=validate` |
| 3 | Um par usuário/projeto não admite participação duplicada | inserção repetida viola a restrição única |
| 4 | Papéis são acumuláveis no mesmo projeto | duas linhas em `participacao_papel` para a mesma participação |
| 5 | O catálogo de papéis é enumeração, não tabela | não existe tabela de papel na migration |
| 6 | O resolvedor devolve as permissões da tabela acima para cada combinação de papéis | teste unitário por papel e por acúmulo |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
