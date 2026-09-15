# TASK-02.9 — Role de aplicação distinta do dono do schema

- **Status:** concluída — 2026-09-15
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 1
- **Depende de:** TASK-02.3
- **Cenários cobertos:** nenhum — task de infraestrutura. Torna verificável
  `ImutabilidadeDoLogIT`, que é verificação além dos cenários
- **Origem:** ACH-01 da revisão de TASK-02.3, pendências 21 e 22, RNF-008,
  SDR-001, ADR-011

#### Contexto

As migrations concedem ao grupo `aplicacao_kanban` apenas `SELECT, INSERT` em
`evento_tarefa` e negam `DELETE` em `etapa` e `raia`. As concessões estão
corretas e foram medidas com uma role fabricada. **Nenhuma conexão do produto
passa por elas:** a aplicação conecta com `BANCO_USUARIO`, que é o
`POSTGRES_USER` da imagem — superusuário e dono do schema —, e contra ele toda
revogação é inerte. RNF-008 fica garantido por uma perna só, e
`contracts/sessao-e-projetos.md:118` promete a outra.

Esta task cria a role de login que faltava e aponta a aplicação para ela.

**O nó que a task precisa desatar junto.** A suíte congelada se contradiz hoje:
`ImutabilidadeDoLogIT` exige que `TRUNCATE evento_tarefa` falhe para a
credencial da aplicação, e `TesteDeIntegracao.esvaziarBanco` trunca todas as
tabelas com essa mesma credencial antes de cada teste. Nenhuma configuração de
privilégio satisfaz as duas **enquanto houver uma credencial só**. A saída não é
relaxar o privilégio: é reconhecer que preparar o ambiente e exercer o produto
são papéis diferentes e sempre foram. O arnês passa a limpar com a credencial do
dono; a aplicação, e só ela, conecta pela role restrita.

Decisão do demandante em 2026-09-15, entre esta saída e aceitar a garantia de
uma perna só emendando o contrato.

#### O que deve ser feito

- [x] Migration nova: criar a role de login `kanban_app`, membro de
      `aplicacao_kanban`, sem `SUPERUSER` e sem posse de objeto. Senha nunca no
      arquivo — atribuída fora da migration.
- [x] `docker/compose.yaml`: segredo próprio da role de aplicação; o serviço
      `backend` passa a conectar com ela. `postgres` e `migracao` continuam com
      a credencial do dono, porque migrar é ato de dono (ADR-011).
- [x] Arnês de teste: a aplicação sob teste conecta pela role restrita, e a
      limpeza entre testes passa a usar conexão própria com a credencial do
      dono, em vez do `DataSource` da aplicação.
- [x] `ImutabilidadeDoLogIT.conectarComoAplicacao` passa a conectar de fato pela
      role da aplicação. Hoje usa o usuário do contêiner, que é o dono — a
      verificação afirmava medir a credencial da aplicação e media outra coisa.
- [x] Emendar o comentário de catálogo de `evento_tarefa`, que registra a
      ressalva de ACH-02: a partir daqui a garantia dupla vale de fato.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `.../db/migration/V<...>__role_de_aplicacao.sql` | criar | role de login, filiação ao grupo, comentário de catálogo revisto |
| `docker/compose.yaml` | alterar | segredo e credencial da aplicação |
| `docker/secrets/app-senha.dev` | criar | segredo de desenvolvimento, como `banco-senha.dev` |
| `docker/compose.test.yaml` | alterar | se replicar a credencial do banco |
| `backend/src/test/java/br/com/idsd/kanban/suporte/TesteDeIntegracao.java` | alterar | **suíte congelada** — mudança de arnês, declarada; ver pendência 22 |
| `backend/src/test/java/br/com/idsd/kanban/alem/ImutabilidadeDoLogIT.java` | alterar | **suíte congelada** — corrige a credencial que a verificação usa |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
migrations já aplicadas, e todo arquivo de verificação de cenário congelado —
as duas exceções acima são de arnês e de verificação além dos cenários, e estão
declaradas.

#### Critérios de aceite

1. A aplicação conecta como role que **não** é superusuário e **não** é dona do
   schema, verificável por `SELECT current_user, usesuper` e por
   `pg_class.relowner`.
2. `ImutabilidadeDoLogIT` passa nas duas verificações, com a credencial que a
   aplicação de fato usa.
3. A suíte de integração continua limpando o banco entre testes, e nenhuma
   classe fica vermelha no `@BeforeEach`.
4. `INSERT` em `evento_tarefa` continua permitido pela role da aplicação — a
   restrição é do log e não do banco.
5. Sem regressão: a contagem de verdes não cai.

#### Histórico

| Data | Tentativa | Resultado |
| --- | --- | --- |
| 2026-09-15 | 1 | **Quatro dos cinco critérios medidos e satisfeitos; o segundo satisfeito pela metade, por dependência e não por defeito.** Critério 1: a aplicação conecta como `kanban_app`, `usesuper = f`, e o dono de `evento_tarefa` é `kanban` — role restrita de fato. Critério 4: `INSERT` no log passa. A recusa é do log e não do banco: `UPDATE`, `DELETE` e `TRUNCATE` de `evento_tarefa` saem em *permission denied*, e também `DELETE` de `etapa` e `raia`; `DELETE` em `intervalo_tarefa` passa, e `CREATE TABLE` no schema `public` é negado. Critério 3: **nenhuma classe fica vermelha no `@BeforeEach`** — zero ocorrência de `permission denied` na suíte inteira, o que fecha a pendência 22 no arranjo de duas credenciais. Critério 5: **189 testes, 96 verdes / 93 vermelhos**, idêntico à base — zero regressão. Critério 2: das duas verificações de `ImutabilidadeDoLogIT`, `aProjecaoContinuaGravavel` fica **verde com a credencial restrita**, e `roleDaAplicacaoNaoAlteraNemApagaEvento` continua vermelha em `404` de `POST /v1/projetos/{id}/tarefas` — rota de TASK-02.5, a mesma dependência que segura as outras classes. O que a asserção dela exige foi medido direto contra o esquema, em `postgres:16-alpine` limpo com as migrations aplicadas em ordem e o placeholder da senha resolvido, e as três recusas saem. A diferença em relação à medição de TASK-02.3 é a que importa: lá a role foi **fabricada** para a medição, e aqui ela é a que a aplicação usa |
