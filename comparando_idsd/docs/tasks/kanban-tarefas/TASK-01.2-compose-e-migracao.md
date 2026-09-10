# TASK-01.2 — Compose de desenvolvimento, migração e identidade

- **Status:** pendente
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

- [ ] Criar `docker/compose.yaml` com cinco serviços: `postgres`, `migracao`,
      `keycloak`, `backend`, `frontend`.
- [ ] Fazer `migracao` executar o Flyway até o fim e sair, com `restart: "no"`.
- [ ] Declarar `backend.depends_on.migracao` com
      `condition: service_completed_successfully`.
- [ ] Declarar `backend.depends_on.keycloak` com `condition: service_healthy`.
- [ ] Habilitar o endpoint de saúde do provedor de identidade por flag na
      subida, na porta de gerenciamento separada, e sondar o endpoint de
      *readiness* dessa porta no healthcheck — nunca a porta da aplicação.
- [ ] Dimensionar o `start_period` do provedor de identidade para a
      **importação do realm**, não para o processo no ar.
- [ ] Usar `readiness` do backend como healthcheck do serviço `backend`.
- [ ] Criar `docker/migracao/Dockerfile` com a imagem do Flyway e as migrations
      montadas de `backend/src/main/resources/db/migration`.
- [ ] Criar `docker/keycloak/realm.json` com o realm de desenvolvimento, client
      público com PKCE para o frontend, e usuários de teste cobrindo os papéis
      `project_admin`, `product_owner`, `dev` e `gestor`.
- [ ] Criar `docker/compose.test.yaml` com os serviços `backend-test` e `e2e`.
- [ ] Montar o socket Docker do host **apenas** em `compose.test.yaml`, no
      serviço `backend-test`, e colocar os contêineres irmãos criados pela suíte
      na mesma rede do serviço de teste.
- [ ] Extrair a versão do PostgreSQL para a variável única `POSTGRES_IMAGE`,
      consumida pelo compose e pela suíte de contêineres de teste.
- [ ] Fazer o serviço `e2e` trazer os browsers na imagem e rodar na mesma rede
      do frontend; prever a subida de 3 réplicas do backend atrás de proxy para
      o teste de broadcast.
- [ ] Referenciar toda imagem base por **digest**; `latest` é proibido; marcar
      a imagem construída com a revisão de código.
- [ ] Prever credencial de banco por **arquivo montado** no perfil de produção,
      e por variável apenas em desenvolvimento.

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
