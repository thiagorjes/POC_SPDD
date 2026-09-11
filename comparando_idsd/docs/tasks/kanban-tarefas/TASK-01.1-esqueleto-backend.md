# TASK-01.1 — Esqueleto do backend e estrutura por domínio

- **Status:** concluída
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** nenhuma
- **Cenários cobertos:** SCN-001.1
- **Origem:** ADR-001, ADR-009, quickstart §1 e §2

#### Contexto

Nada existe em disco: o repositório é greenfield. Esta task cria o projeto Java
e a árvore de pastas por domínio sobre a qual todas as demais escrevem. Ela não
entrega comportamento sozinha — habilita SCN-001.1, que só fecha ao fim do
épico. Fazer a árvore errada aqui custa caro depois, porque cada task seguinte
declara escopo de arquivo contra ela.

#### O que deve ser feito

- [x] Criar o projeto Maven do backend com Java 25 e Spring Boot na linha LTS.
- [x] Declarar as dependências: Web, Data JPA, Validation, WebSocket, OAuth2
      Resource Server, Actuator, driver PostgreSQL, Flyway (dialeto PostgreSQL),
      JUnit 5, Mockito, Testcontainers, JaCoCo.
- [x] Criar a árvore de pacotes por domínio, com um pacote por domínio e nada
      de pacote por camada técnica.
- [x] Fixar `spring.jpa.hibernate.ddl-auto=validate` em **todo** perfil,
      inclusive teste, e **não** habilitar o Flyway na aplicação.
- [x] Configurar por variável de ambiente o que **não** é segredo: URL do banco,
      usuário do banco, issuer URI do provedor de identidade. A **senha** entra
      por arquivo montado (`configtree:/run/secrets/`), nunca por variável.
      Nada de credencial em arquivo versionado, inclusive no perfil de teste.
- [x] Expor probes separados no Actuator: `GET /actuator/health/liveness` e
      `GET /actuator/health/readiness`.
- [x] Configurar o JaCoCo com gate de cobertura de 80%.
- [x] Espelhar a árvore principal em `backend/src/test/java`.

> Ação de credencial emendada em 2026-09-10 (ACH-02). A redação original mandava
> a credencial por variável de ambiente, contra `infra/docker/architecture.md`
> §7, que só foi elaborada depois de esta task ser escrita. Variável vaza em
> `docker inspect`, em log de crash e em dump de processo.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `backend/pom.xml` | criar | dependências e plugin do JaCoCo com gate de 80% |
| `backend/src/main/java/br/com/idsd/kanban/internal/projeto/` | criar | projeto, etapa, raia, participacao, papel |
| `backend/src/main/java/br/com/idsd/kanban/internal/tarefa/` | criar | tarefa, evento, impedimento, movimentos, tomada |
| `backend/src/main/java/br/com/idsd/kanban/internal/tempo/` | criar | intervalos, projeção, consultas agregadas |
| `backend/src/main/java/br/com/idsd/kanban/internal/acesso/` | criar | sessão, autoprovisionamento, resolução de permissão |
| `backend/src/main/java/br/com/idsd/kanban/shared/` | criar | problem+json, correlação, porta de publicação de evento |
| `backend/src/main/java/br/com/idsd/kanban/config/` | criar | segurança, STOMP, listener de escuta do banco |
| `backend/src/main/resources/application.yml` | criar | perfis, `ddl-auto=validate`, Actuator |
| `backend/src/main/resources/db/migration/` | criar | pasta vazia; as migrations chegam nas tasks seguintes |
| `backend/src/test/java/` | criar | espelha a árvore principal |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, e todo arquivo de verificação sob `backend/src/test/` que já
tenha sido produzido pela etapa de testes.

#### Guia técnico — padrão a seguir

Não há código de produção anterior no repositório. A referência normativa é a
coleção `requirements/guidelines/backend/java/` (`architecture.md` para
Screaming Architecture por domínio, `coding-standards.md`, `database.md`,
`testing.md`) e `requirements/guidelines/infra/docker/`.

Regras de estrutura que valem para todo o backend e que esta task institui:

- Cada domínio expõe serviço e DTO. **Entidade JPA nunca sai do serviço.**
- Controller não contém regra de negócio.
- `internal/` não é importado de fora do próprio domínio; o que atravessa
  domínio vive em `shared/`.

Propriedades obrigatórias em `application.yml`, em todo perfil:

- `spring.jpa.hibernate.ddl-auto: validate`
- `spring.flyway.enabled: false`
- `management.endpoint.health.probes.enabled: true`
- `spring.security.oauth2.resourceserver.jwt.issuer-uri` vindo de variável
- `spring.config.import: "configtree:/run/secrets/"` fora do perfil de teste,
  sem `optional:` — instância sem o segredo montado falha no arranque, e não
  adiante, na primeira conexão
- `spring.datasource.password: ${banco-senha}`, resolvido pelo configtree

#### Guia técnico — pontos de atenção

- **Não há `client secret` no backend.** Ele é Resource Server puro; o
  authorization code com PKCE vive no frontend, com client público. Configurar
  um secret aqui é sinal de que o desenho foi invertido.
- **Flyway desabilitado na aplicação não é descuido.** A migração roda em
  serviço próprio do compose. Aplicação apontada para banco desatualizado deve
  **falhar** no arranque, não se autocorrigir.
- **Nunca ponha a conexão de escuta do banco no liveness.** Instabilidade do
  banco mataria todas as instâncias ao mesmo tempo, durante a janela de backoff
  em que elas se recuperariam sozinhas. A escuta entra no readiness.
- Introspecção JavaBeans quebra em campo com prefixo `e` seguido de maiúscula
  (por exemplo `eFinal`) — achado já registrado nas guidelines do workspace.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | O módulo compila e o contexto Spring sobe com perfil de teste | `mvn -q verify` dentro do contêiner de build |
| 2 | `ddl-auto` é `validate` em todos os perfis | busca por `ddl-auto` em `src/main/resources` retorna só `validate` |
| 3 | O Flyway não roda na aplicação | `spring.flyway.enabled: false` presente e nenhuma classe de migração invocada no arranque |
| 4 | Liveness e readiness são endpoints distintos | requisição a `/actuator/health/liveness` e a `/actuator/health/readiness` respondem separadamente |
| 5 | Existem os quatro pacotes de domínio, `shared` e `config` | listagem da árvore de pacotes |
| 6 | O gate de cobertura de 80% está configurado | plugin do JaCoCo com `check` vinculado à fase de verificação |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
| 2026-09-10 | ACH-02 e ACH-04 | Senha do banco sai da variável `BANCO_SENHA` e passa a vir de `/run/secrets/banco-senha` por `configtree`, importado sem `optional:` fora do perfil de teste; credenciais literais do perfil de teste removidas — Testcontainers já as fornece por `@DynamicPropertySource`. Verificado em contêiner: com o segredo montado o contexto sobe e conecta (`Started Aplicacao`), sem ele o arranque falha com `Config data resource 'config tree [/run/secrets]' ... does not exist`. Os probes seguem `401` (TASK-01.4) |
| 2026-09-10 | tentativa 1 — Red | `mvn test-compile` no contêiner `maven:3.9-eclipse-temurin-25`: falha na **compilação** dos testes, não em asserção. Todo erro é `cannot find symbol` sobre classe de produção que ainda não existe (`EtapaService`, `TomadaService`, `ImpedimentoService`, `CriacaoDeTarefaService`, `RegistradorDeEvento`, `AplicadorDeIntervalos`, `VerificadorDeOrigem`, `ReconstrutorDeProjecao`, `Tarefa`, `Impedimento`, `Condicao`, `FluxoRequisicao`, `RegraDeNegocioViolada`, `EtapaRepositorio`, `DestaqueDeImpedimento`). É o Red previsto e o custo já declarado no plano de verificação por os cinco cenários unitários fixarem costura interna antes da implementação: a medição de Red por contagem de testes falhando só é possível quando essas classes existirem, ao fim do épico |
| 2026-09-10 | tentativa 1 — achado do Red | A suíte congelada nomeia a classe de arranque: `InstanciasEmParalelo:64` referencia `br.com.idsd.kanban.Aplicacao`. A classe foi criada com esse nome; nenhum arquivo de teste foi tocado |
| 2026-09-10 | tentativa 1 — achado de estrutura | A suíte pressupõe um quinto pacote de domínio, `internal/impedimento` (`ImpedimentoServiceTest`), que a tabela desta task não declara — ela lista quatro. Não criado aqui, por estar fora do escopo declarado. Dono: TASK-04.1 / TASK-04.2 |
| 2026-09-10 | tentativa 1 — achado de suíte | `ProvedorSimulado` referencia o body file `identidade/descoberta-200.json`, e `backend/src/test/resources/` não existe no repositório. Fixture ausente na suíte congelada; escrever ali é território do `/tests`. Só afeta SCN-001.3 (TASK-01.4) |
| 2026-09-10 | tentativa 1 — decisão | `issuer-uri` ficou em documento de perfil `"!test"`. Sob `test` o decoder vem de `jwk-set-uri`, porque `TesteDeIntegracao` não registra issuer e a descoberta OIDC é resolvida *eagerly* na subida do contexto — exigir issuer ali faria toda a suíte falhar por motivo alheio ao contrato. `InstanciasEmParalelo` sobrescreve as duas propriedades por conta própria e continua valendo |
| 2026-09-10 | tentativa 1 — verde possível | `mvn -Dmaven.test.skip=true package` verde; contexto sobe com `SPRING_PROFILES_ACTIVE=test` contra PostgreSQL 16 em contêiner (`Started Aplicacao`), com `ddl-auto=validate` e `flyway.enabled=false`. `/actuator/health/liveness` e `/actuator/health/readiness` existem e respondem separadamente, ambos `401` — não há `SegurancaConfig` ainda, e ele é arquivo declarado da TASK-01.4, que também é quem libera os probes para o healthcheck do compose |
| 2026-09-10 | tentativa 1 — validação | `check_escopo.py` acusa 14 arquivos fora do escopo, **nenhum** produzido por esta task: são modificações não commitadas preexistentes no monorepo (`.agents/`, `docs/decisions/`, `memory/`, `poc_v2/`), que o script vê porque compara a árvore inteira do repositório, cuja raiz é `SPDD_puro`. Os `.feature` e os arquivos sob `backend/src/test/` estão intactos |
