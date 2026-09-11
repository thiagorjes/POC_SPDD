# TASK-02.1 — Migration 2 e entidades de etapa e raia

- **Status:** pendente
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

- [ ] Criar a migration de ordem 2 com as tabelas `etapa` e `raia`.
- [ ] Criar o índice único parcial de ordem por projeto.
- [ ] Criar as entidades JPA e os repositórios.
- [ ] Implementar remoção **lógica** por `arquivada_em`; não existe remoção
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
