# TASK-01.3 — Migration 1 e entidades de usuário, projeto e participação

- **Status:** concluída
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

- [x] Criar a migration de ordem 1 com as tabelas `usuario`, `projeto`,
      `participacao` e `participacao_papel`, e seus índices.
- [x] Criar as entidades JPA e os repositórios correspondentes.
- [x] Definir o catálogo de papéis como **enumeração em código**, nunca tabela.
- [x] Implementar o resolvedor de permissões a partir dos papéis da
      participação real.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/src/main/resources/db/migration/V<ANO><MES><DIA><HORA>__configuracao_base.sql` | criar | tabelas e índices desta task |
| `backend/src/main/java/br/com/idsd/kanban/internal/acesso/Usuario.java` | criar | entidade |
| `backend/src/main/java/br/com/idsd/kanban/internal/projeto/Projeto.java` | criar | entidade |
| `backend/src/main/java/br/com/idsd/kanban/internal/projeto/Participacao.java` | criar | entidade |
| `backend/src/main/java/br/com/idsd/kanban/internal/projeto/Papel.java` | criar | enumeração fechada |
| `backend/src/main/java/br/com/idsd/kanban/internal/projeto/Permissao.java` | criar | enumeração derivada dos papéis |
| `backend/src/main/java/br/com/idsd/kanban/internal/projeto/ResolvedorDePermissao.java` | criar | serviço |
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
| 2026-09-10 | Red medido | `mvn test-compile` reprova por `cannot find symbol` sobre classes de produção inexistentes — a suíte **não compila**, como em TASK-01.1. Nenhum teste dos cenários desta task chega a executar, e o Red segue sendo de compilação e não de asserção |
| 2026-09-10 | tentativa 1 | Nove arquivos criados, todos dentro da tabela. A migration é `V2026091009__configuracao_base.sql`. Três decisões que a task deixava à implementação: **(a)** o código persistido do papel (`project_admin`, `dev`…) é atributo da enumeração, com conversor JPA aninhado em `Papel` — aninhado de propósito, para que renomear o código sem ver a coluna que depende dele fique difícil, e porque um arquivo próprio estaria fora da tabela; código desconhecido na leitura **levanta exceção**, já que catálogo fechado não serve para nada se linha inválida no banco virar papel silenciosamente nulo. **(b)** `participacao` referencia `usuario` e `projeto` por `@ManyToOne` preguiçoso, e os papéis são `@ElementCollection` sobre `Set`, porque a PK `(participacao_id, papel)` já torna papel repetido um estado impossível. **(c)** Nenhuma constraint `CHECK` sobre `participacao_papel.papel`: ela duplicaria o catálogo no banco, que é exatamente a segunda fonte que a decisão de enumeração existe para não ter — o critério 5 pede ausência de tabela de papel, e uma lista de valores presa na migration teria o mesmo defeito com outro nome |
| 2026-09-10 | verificação | Critérios 1 a 6 medidos por execução. **1:** `up migracao` em banco limpo aplica a migration e sai (`Successfully applied 1 migration`, PostgreSQL 16.15, Flyway 13.6.0). **2:** o backend sobe com `ddl-auto=validate` contra o schema aplicado (`Started Aplicacao in 8.618 seconds`) — que é a prova real do mapeamento das cinco entidades, e a razão de a verificação não ter parado na compilação. **3:** segunda `participacao` para o mesmo par é recusada por `participacao_usuario_projeto_unico`. **4:** `project_admin` e `dev` convivem na mesma participação, duas linhas. **5:** `\dt` lista quatro tabelas do domínio e nenhuma de catálogo de papel. **6:** 8 testes verdes em `ResolvedorDePermissaoTest`. Suíte inteira reconferida: os símbolos ausentes continuam sendo só de tasks posteriores (tarefa, etapa, impedimento, evento) — nenhuma regressão |
| 2026-09-10 | achado — critério 6 sem teste na suíte congelada | O critério pede "teste unitário por papel e por acúmulo", e a suíte escrita pelo `/tests` **não tem nenhum**: SCN-002.1 é o único cenário da task, é de integração, depende de `GET /v1/projetos` (TASK-01.5) e não exercita `project_admin` nem acúmulo. `ResolvedorDePermissaoTest` foi escrito aqui para cumprir o critério, **fora da tabela de arquivos** e declaradamente fora da suíte congelada: não cobre cenário Gherkin, não toca `.feature` nem step definition, e não altera nada do que já existia. Como a suíte não compila, ele foi executado isoladamente por `javac` + console do JUnit sobre o classpath do projeto — 8/8 verdes. Dono do buraco: `/tests` |
| 2026-09-10 | achado — SCN-002.1 inalcançável nesta task | O único cenário coberto exige `GET /v1/projetos`, que é da **TASK-01.5**, e a rota de sessão que autoprovisiona, que é da TASK-01.4. Mesma assimetria que ACH-07 da revisão de TASK-01.1 registrou: a task está completa e o cenário que ela declara cobrir só fica verde duas tasks adiante. Dono `/tasks` |
| 2026-09-10 | bloqueante da revisão fechado | ACH-01 do `TASK-01.3-review.md`: o resolvedor passou a reconhecer os **dois** sujeitos que precisa resolver. Lê `usuario.adminGlobal` e devolve um `Acesso` com as permissões, a marca `porAdministracaoGlobal` que SCN-021.2 exige e o sinalizador `participa`. A marca vem de quem decidiu o acesso e não é derivada de novo na borda, porque derivá-la duas vezes é a segunda fonte da mesma decisão; e `participa` existe para que o chamador distinga "não participa" de "participa sem papel" sem que este serviço escolha entre `403` e `404` — essa decisão é de spec e está em aberto com o `/techspec`. Fecharam na mesma passagem ACH-02 (`permissoesDe` deixou de ser público: calcular permissão a partir de papel do chamador é o que RNF-004 proíbe) e ACH-10 (retornos imutáveis). O que **não** entrou: criar projeto (RN-036) ficou declaradamente fora de `Permissao`, porque não é permissão de projeto — nenhum papel a possui, já que nenhum papel existe antes do projeto. Verificado por execução: 14/14 verdes, com 6 testes novos sobre o caminho que lê do banco, e o contexto sobe com `ddl-auto=validate` contra o schema aplicado. A suíte continua reprovando nos mesmos cinco arquivos de tasks posteriores — nenhuma regressão |
| 2026-09-10 | `check_escopo.py` | Oito erros, **nenhum desta task**: são os arquivos do fechamento dos bloqueantes de TASK-01.2, já commitados, que o script vê como alteração fora do escopo por comparar com o commit anterior. Os onze arquivos desta task não são acusados |
