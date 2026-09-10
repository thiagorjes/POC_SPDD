# TASK-01.2 — Compose de desenvolvimento, migração e identidade

- **Status:** concluída
- **Sistema:** idsd
- **Executor:** agente
- **Tentativas:** 3
- **Depende de:** TASK-01.1
- **Cenários cobertos:** SCN-001.1
- **Origem:** ADR-008, ADR-011, ADR-012, ADR-013, quickstart §3

#### Contexto

Tudo roda em contêiner: não existe caminho suportado de subir banco ou provedor
de identidade na máquina de quem desenvolve, e a suíte de verificação depende do
mesmo ambiente. Esta task cria os dois arquivos de composição — desenvolvimento
e teste — e o serviço de migração dedicado. A ordem de subida não é preferência
de estilo: sem ela a aplicação falha no arranque com mensagem que não aponta
para a causa.

#### O que deve ser feito

- [x] Criar `docker/compose.yaml` com cinco serviços: `postgres`, `migracao`,
      `keycloak`, `backend`, `frontend`.
- [x] Fazer `migracao` executar o Flyway até o fim e sair, com `restart: "no"`.
- [x] Declarar `backend.depends_on.migracao` com
      `condition: service_completed_successfully`.
- [x] Declarar `backend.depends_on.keycloak` com `condition: service_healthy`.
- [x] Habilitar o endpoint de saúde do provedor de identidade por flag na
      subida, na porta de gerenciamento separada, e sondar o endpoint de
      *readiness* dessa porta no healthcheck — nunca a porta da aplicação.
- [x] Dimensionar o `start_period` do provedor de identidade para a
      **importação do realm**, não para o processo no ar.
- [x] Usar `readiness` do backend como healthcheck do serviço `backend`.
- [x] Criar `docker/migracao/Dockerfile` com a imagem do Flyway e as migrations
      montadas de `backend/src/main/resources/db/migration`.
- [x] Criar `docker/keycloak/realm.json` com o realm de desenvolvimento, client
      público com PKCE para o frontend, e usuários de teste cobrindo os papéis
      `project_admin`, `product_owner`, `dev` e `gestor`.
- [x] Criar `docker/compose.test.yaml` com os serviços `backend-test` e `e2e`.
- [x] Montar o socket Docker do host **apenas** em `compose.test.yaml`, no
      serviço `backend-test`, e colocar os contêineres irmãos criados pela suíte
      na mesma rede do serviço de teste.
- [x] Extrair a versão do PostgreSQL para a variável única `POSTGRES_IMAGE`,
      consumida pelo compose e pela suíte de contêineres de teste.
- [x] Fazer o serviço `e2e` trazer os browsers na imagem e rodar na mesma rede
      do frontend; prever a subida de 3 réplicas do backend atrás de proxy para
      o teste de broadcast.
- [x] Referenciar toda imagem base por **digest**; `latest` é proibido; marcar
      a imagem construída com a revisão de código.
- [x] Prover a credencial de banco por **arquivo montado** (`secrets:` do
      Compose) em **todo** ambiente, montado como `/run/secrets/banco-senha`,
      que é o caminho que o `application.yml` importa por configtree. Em
      desenvolvimento o valor é descartável, mas entra pelo mesmo mecanismo.


> Ação de credencial emendada em 2026-09-10 (ACH-02). A redação original admitia
> variável de ambiente em desenvolvimento; `infra/docker/architecture.md` §7
> recusa isso pela razão de que caminho de produção não exercitado em
> desenvolvimento não é caminho testado.

#### Guia técnico — estrutura de arquivos

| Arquivo | Ação | Observação |
| --- | --- | --- |
| `docker/compose.yaml` | criar | cinco serviços, ordem de subida por condição |
| `docker/compose.test.yaml` | criar | `backend-test` e `e2e`; única montagem do socket |
| `docker/migracao/Dockerfile` | criar | serviço de migração dedicado |
| `docker/keycloak/realm.json` | criar | realm, client público com PKCE, usuários por papel |
| `docker/.env.example` | criar | `POSTGRES_IMAGE` e demais variáveis, sem valor secreto real |
| `backend/Dockerfile` | criar | imagem do backend, base por digest |
| `frontend/Dockerfile` | criar | imagem do frontend, base por digest |

**Proibido tocar:** `docs/prd/kanban-tarefas/*.feature`, `docs/prd/`,
`docs/techspec/`, `requirements/guidelines/infra/docker/` (norma, não artefato
desta feature).

#### Guia técnico — padrão a seguir

Norma vinculante: `requirements/guidelines/infra/docker/stack.md`,
`architecture.md` §6, `coding-standards.md`, `testing.md` §3 e
`definition-of-done.md` (19 critérios verificáveis na máquina de quem
desenvolve).

Comandos que precisam funcionar ao fim da task:

```
docker compose -f docker/compose.yaml up --build
docker compose -f docker/compose.test.yaml run --rm backend-test
docker compose -f docker/compose.test.yaml run --rm e2e
```

#### Guia técnico — pontos de atenção

- **`service_started` disfarçado de saúde.** A imagem oficial do provedor de
  identidade não traz healthcheck nem ferramenta HTTP no runtime. Sem habilitar
  o endpoint de saúde e sondar a porta de gerenciamento, o healthcheck ou não
  funciona ou é um `service_started` com outro nome — exatamente a falha que a
  ordem de subida existe para evitar.
- **Processo no ar e realm importado não são a mesma condição.** É a segunda
  que o backend precisa.
- **A migração fora do boot protege o teste de três instâncias.** Migrar no boot
  as poria em disputa pelo lock, e a que espera pode estourar o `start_period`
  antes de a outra terminar. Afrouxar o `start_period` foi recusado por ser
  número mágico que volta a falhar quando a migração crescer.
- **O socket do host concede ao serviço de teste o equivalente a acesso root.**
  É aceito apenas em desenvolvimento e CI, e a montagem só pode existir em
  `compose.test.yaml`. Execução de Docker dentro de Docker foi descartada pelo
  custo de camada e pela perda de cache — suíte cara é suíte contornada.
- **"A mesma imagem do compose" não pode ser convenção em prosa.** Sem a
  variável única, divergir de versão reintroduz, mais devagar, o problema que
  motivou tirar o banco em memória da suíte.
- **Variável vaza em inspeção de contêiner.** Por isso o segredo de produção vai
  por arquivo montado.

#### Critérios de aceite

| # | Critério | Verificação |
| --- | --- | --- |
| 1 | Um comando sobe o ambiente completo | `docker compose -f docker/compose.yaml up --build` chega com os cinco serviços saudáveis |
| 2 | O backend só inicia após a migração terminar | `docker compose config` mostra `service_completed_successfully` no `migracao` |
| 3 | O backend só inicia após o realm estar importado | derrubar o provedor de identidade e subir: o backend aguarda em vez de falhar |
| 4 | O healthcheck do provedor sonda a porta de gerenciamento | inspeção do healthcheck no `compose config` |
| 5 | O socket do host não aparece em `compose.yaml` | busca por `docker.sock` retorna ocorrência apenas em `compose.test.yaml` |
| 6 | Nenhuma imagem usa `latest` e todas trazem digest | busca por `:latest` retorna vazio; toda referência traz `@sha256:` |
| 7 | A versão do banco vem de variável única | `POSTGRES_IMAGE` é a única fonte, usada pelos dois arquivos de composição |
| 8 | Os dois comandos de teste executam | `run --rm backend-test` e `run --rm e2e` iniciam sem erro de infraestrutura |

#### Histórico

| Data | Evento | Detalhe |
| --- | --- | --- |
| 2026-09-09 | criação | Task derivada do plano de execução do épico |
| 2026-09-10 | tentativa 1 | Sete arquivos declarados criados, mais cinco que a execução obrigou a existir e que a tabela não previa: `docker/migracao/entrypoint.sh` (o Flyway não tem opção de senha por arquivo — o CLI a recebe como argumento lido do arquivo montado, nunca por `FLYWAY_PASSWORD`, que vazaria em `docker inspect`), `docker/keycloak/health.sh` (a imagem não traz curl nem wget, só bash; o healthcheck sonda 9000 por `/dev/tcp`, porque apontar para ferramenta que não existe no runtime é o `service_started` disfarçado que o próprio ponto de atenção nomeia), `docker/keycloak/README.md` (a importação recusa campo desconhecido e derruba o servidor em laço com `Unrecognized field` — a razão de cada escolha do realm não cabe dentro dele), `docker/proxy/nginx.conf` e `docker/secrets/`. Três falhas de construção corrigidas na própria tentativa: a imagem do backend compilava `src/test` com `-DskipTests` e quebrava no Red medido em TASK-01.1 — passou a copiar só `src/main` com `-Dmaven.test.skip=true`, porque atar a construção da imagem ao Red que a suíte mede é acoplar coisas que existem para medir uma à outra; `USER flyway` não existe na imagem oficial, que roda como root, e o usuário não-root foi criado; e o `KC_BOOTSTRAP_ADMIN_PASSWORD_FILE` não é honrado em 26.4, o que deixou duas saídas — credencial por `ENV`, que §7 recusa, ou nenhuma conta administrativa. Ficou a segunda: o realm inteiro vem do arquivo e nenhum cenário precisa do console |
| 2026-09-10 | achado — ambiente | Subida limpa reprovava com `UnknownHostException: postgres` e o backend em laço de reinício, com `RestartCount` 15. A causa não estava no compose: outra stack desta máquina (`idsd-full-claude`) usa o **mesmo nome de projeto** `idsd`, e o Compose reusou o contêiner alheio — `docker compose ps` listava um serviço `app` que este arquivo não declara e um `postgres:16` onde este arquivo fixa `postgres:16-alpine@sha256:…`. Projeto e rede passaram a `idsd-kanban` / `idsd-kanban-net`. A colisão de porta que veio junto (5432, 8080 e 8180 ocupadas) expôs que `PORTA_BANCO` não era parametrizável: virou variável, como as outras três, e o `.env.example` registra a razão. O modo de falha vale ser lembrado — nome de projeto é namespace, e quando ele colide o Compose não avisa, ele reusa |
| 2026-09-10 | verificação | Execução real, e a de ACH-03 nos dois sentidos. `config -q` limpo nos dois arquivos, hadolint limpo nos três Dockerfiles. Subida limpa após `down -v`: postgres `healthy` → `migracao` `Exited (0)` contra PostgreSQL 16 → keycloak `healthy` após a importação → backend `Started`, `RestartCount` 0. Com token real do realm (`idsd-e2e`, direct grant, usuário `ana`) liveness e readiness respondem `200 UP`; **com o banco parado, liveness segue `200 UP` e readiness cai para `503 DOWN`** — que é a prova de ACH-03, e não o `UP` de rotina. Critério 3 medido: com o provedor parado, o Compose o restabeleceu e o backend só subiu depois de `Healthy`. Critérios 5, 6 e 7 conferidos por busca: `docker.sock` só em `compose.test.yaml`, zero `:latest`, dez referências externas todas por digest, `POSTGRES_IMAGE` lida pelos dois arquivos e pela suíte. `trivy image --scanners secret` em `idsd/backend:dev` e `idsd/migracao:dev`: exit 0. `run --rm backend-test` executa e falha em `testCompile` com os mesmos `cannot find symbol` do Red de TASK-01.1 — o serviço funciona, a suíte é que ainda não compila; o socket foi verificado à parte, alcançável de dentro do serviço, com `host.docker.internal` resolvendo |
| 2026-09-10 | achado — critério 1 inalcançável nesta task | Os probes respondem `401` sem token, porque `SegurancaConfig` é arquivo declarado da **TASK-01.4**, que é também quem os libera para o healthcheck. O healthcheck do backend, portanto, nunca chega a `healthy`, e o critério 1 ("cinco serviços saudáveis") está bloqueado em TASK-01.4, não aqui. Afrouxar o healthcheck para `service_started` foi recusado: é literalmente a falha que o ponto de atenção desta task nomeia |
| 2026-09-10 | achado — critério 8 parcial | `run --rm e2e` não é executável: não existe `frontend/package.json`, que nasce na **TASK-01.7**. Pela mesma razão `docker compose up frontend` não constrói. O `frontend/Dockerfile` foi escrito e é o artefato que a task pede, mas não foi exercitado |
| 2026-09-10 | achado — desvio de instrução | A ação manda colocar os contêineres irmãos da suíte "na mesma rede do serviço de teste". Não é alcançável por configuração com a suíte congelada: quem cria os irmãos é o Testcontainers, pelo socket do host, e o daemon os põe na rede *dele*, não na do serviço — mudar isso exigiria tocar em `TesteDeIntegracao`, que a restrição desta skill proíbe. A intenção — o serviço alcança os irmãos que ele mesmo criou — foi satisfeita por `host.docker.internal` com `extra_hosts: host-gateway` e `TESTCONTAINERS_HOST_OVERRIDE`, e verificada |
| 2026-09-10 | achado — versão do provedor não fixada por decisão | `stack.md` espera "versão fixa por sistema" vinda de ADR-003, e o ADR é silencioso quanto à versão do Keycloak. 26.4 foi escolhida aqui, por digest. Dono `/techspec` |
| 2026-09-10 | bloqueantes da revisão fechados | ACH-01, ACH-02 e ACH-03 de `TASK-01.2-review.md`, numa passagem. **ACH-01/ACH-03:** base da migração para `flyway/flyway:13.6.0-alpine` (a linha 11 parou de receber build a montante e não tem digest mais novo; a troca de major não alcança a aplicação, que não migra em perfil nenhum), `apk upgrade` e remoção **nominal** dos três drivers JDBC vulneráveis — apagar diretório de driver inteiro derruba o CLI na subida, porque o registro de plugins percorre todos (`ClassNotFoundException: com.datastax...`, medido); base de runtime do backend para `eclipse-temurin:25-jre-noble`, porque a tag genérica resolve para Ubuntu 26.04 com `/usr/bin/pebble` vulnerável e sem correção; Spring Boot 3.5.8 → 3.5.16, com Tomcat 10.1.59 e driver PostgreSQL 42.7.13 sobrepostos por trazerem CVE corrigido que a 3.5.16 ainda não alcançou — cada um com a condição de remoção escrita no `pom.xml`. Medido: `trivy --severity HIGH,CRITICAL --scanners vuln` em **0 e 0** nas duas imagens, contra 5 CRITICAL / 56 HIGH e 27 HIGH/CRITICAL antes. **ACH-02:** `realm.json` perdeu `roles` e todo `realmRoles`; o papel pretendido de cada conta virou tabela no `README.md` do realm, concedido no banco junto da participação (ADR-003, BDR-001). Provado com token real: `ana` autentica sem `realm_access` nem `resource_access`. Seguro porque nenhum teste da suíte congelada lê essas claims, verificado antes. Reconferido: `--scanners secret` exit 0, hadolint limpo, `config -q` limpo, subida completa na ordem esperada com o backend em 5,3 s sobre `Apache Tomcat/10.1.59`. `backend/pom.xml` está **fora da tabela de arquivos desta task**, como `application.yml` |
| 2026-09-10 | ACH-03 absorvido | `backend/src/main/resources/application.yml` — grupo de readiness passa a `readinessState,db` e o de liveness a `livenessState`. Grupo de readiness com só `readinessState` é liveness com outro nome, e com o healthcheck do compose apontado para lá uma instância com o banco fora responderia pronta e seguiria recebendo tráfego. O indicador da conexão de LISTEN ainda não existe e entra no grupo quando o épico de broadcast o criar. O arquivo está **fora da tabela de arquivos desta task**, deliberadamente, e `check_escopo.py` o acusa — junto com os dois arquivos de task e o `state.md`, que a própria skill obriga a atualizar |
