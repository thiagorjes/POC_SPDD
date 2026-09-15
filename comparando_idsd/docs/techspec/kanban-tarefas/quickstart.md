# Quickstart — kanban-tarefas

_Insumo de `/implement`. Leia antes de escrever a primeira linha._
_Fontes: TechSpec `docs/techspec/kanban-tarefas-techspec.md`, ADR-008, ADR-009, SDR-003._

---

## 1. Stack

| Camada | Escolha | Fonte |
| --- | --- | --- |
| Linguagem | Java 25 | ADR-001, ADR-009 |
| Framework | Spring Boot LTS — Web, Data JPA, Validation, WebSocket, OAuth2 Resource Server, Actuator | ADR-001 |
| Banco | PostgreSQL, único armazenamento; sem cache e sem broker | ADR-002, ADR-009 |
| Migrations | Flyway, dialeto PostgreSQL, `ddl-auto=validate` | ADR-005 |
| Tempo real | WebSocket/STOMP + `LISTEN/NOTIFY` no canal `board_events` | ADR-004 |
| Identidade | Keycloak / RH-SSO, OIDC | ADR-003, ADR-006 |
| Frontend | Next.js App Router, React Query, STOMP client | coleção `frontend/nextjs` |
| Testes | JUnit 5 + Mockito, Testcontainers, MockMvc, Playwright, JaCoCo | SDR-003 |
| Execução | Docker, tudo em contêiner — aplicação, banco, identidade e testes | ADR-008 |

**Tudo roda em contêiner.** Não existe caminho suportado de subir o banco ou o
provedor de identidade na máquina do desenvolvedor. Ver §3.

---

## 2. Estrutura de pastas

Screaming Architecture por domínio (`backend/java/architecture.md`). Quatro
domínios, derivados da TechSpec §5:

```
backend/
  src/main/java/<pkg>/
    internal/
      projeto/     projeto, etapa, raia, participacao, papel
      tarefa/      tarefa, evento, impedimento, movimentos, tomada
      tempo/       intervalos, projeção, consultas agregadas (RF-015, RF-016)
      acesso/      sessão, autoprovisionamento, resolução de permissão
    shared/        problem+json, correlação, EventoBoardPublisher (porta)
    config/        segurança, STOMP, listener LISTEN
  src/main/resources/db/migration/   V1__ .. V6__  (data-model.md §8)
  src/test/java/...                  espelha a árvore principal
frontend/
  app/             App Router — 11 telas do design brief
  components/      cartão, coluna, fila, indicadores das três séries
  lib/api/         cliente REST tipado + cliente STOMP
docker/
  compose.yaml         desenvolvimento
  compose.test.yaml    suíte
  migracao/Dockerfile  serviço de migração (ADR-011)
  keycloak/realm.json  realm de desenvolvimento
```

Cada domínio expõe serviço e DTO; **entidade JPA nunca sai do serviço**, e
controller não contém regra.

---

## 3. Setup mínimo

Pré-requisito único: Docker com Compose, nas versões que
`infra/docker/stack.md` fixa. Nada de JDK, banco ou Keycloak local.

> Desde 2026-09-09 a coleção `infra/docker` existe e governa este documento. O
> que segue **aplica** a norma dela ao sistema; onde os dois divergirem, a
> coleção manda. As decisões que esta TechSpec tomava localmente viraram
> ADR-011, ADR-012 e ADR-013.

```
docker compose -f docker/compose.yaml up --build
```

Sobe cinco serviços — quatro de longa duração e um de ciclo de vida:

| Serviço | Para quê |
| --- | --- |
| `postgres` | banco e canal `board_events` |
| `migracao` | Flyway; roda até o fim e sai (`restart: "no"`) — ADR-011 |
| `keycloak` | realm importado de `docker/keycloak/realm.json`, com usuários de teste |
| `backend` | Spring Boot, com `SPRING_PROFILES_ACTIVE=dev` |
| `frontend` | Next.js em modo desenvolvimento |

**Ordem importa e não é opcional.** O autoconfigure do OAuth2 resolve o issuer
*eagerly* na subida do contexto Spring (`backend/java/testing.md` §5): o `backend`
precisa de `depends_on` com `condition: service_healthy` sobre o `keycloak`, não
apenas `service_started`. Sem isso a aplicação sobe antes do realm existir e
falha no arranque, com mensagem que não aponta para a causa.

**Mas `service_healthy` exige um healthcheck que não vem pronto.** A imagem
oficial do Keycloak não traz healthcheck nem ferramenta HTTP no runtime, e o
endpoint de saúde precisa ser habilitado explicitamente e vive em porta de
gerenciamento separada. Sem prescrever isso, a implementação produz um healthcheck
que não funciona ou um `service_started` disfarçado — exatamente a falha que o
parágrafo acima quer evitar. O serviço `keycloak` declara:

- health habilitado por flag na subida, exposto na porta de gerenciamento;
- healthcheck sondando o endpoint de *readiness* dessa porta, não a porta da
  aplicação;
- `start_period` dimensionado para a **importação do realm** — processo no ar e
  realm importado não são a mesma condição, e é a segunda que o backend precisa.

**Migração fora do boot.** Flyway **não** roda na subida da aplicação: ele é o
serviço `migracao`, que migra e sai. O `backend` depende dele por
`condition: service_completed_successfully` e sobe com validação de schema, sem
permissão de migrar. Sem isso, as 3 instâncias que o teste de RNF-002 exige
disputariam o lock, e a que espera pode estourar o `start_period` antes de a
outra terminar. A alternativa — `start_period` folgado — foi recusada por ser
número mágico que volta a falhar quando a migração crescer. Ver
[ADR-011](../../decisions/ADR-011-migracao-de-schema-em-servico-dedicado.md) e
`infra/docker/architecture.md` §6. Fecha Q-010.

Consequência aceita: aplicação apontada para banco desatualizado **falha** em vez
de se autocorrigir. É o comportamento correto — autocorreção silenciosa é como
uma instância antiga migra um schema que ela não conhece.

**Configuração** por variável de ambiente em desenvolvimento, nunca em
`application.yml` versionado: URL e credencial do banco, issuer URI. **Não há
`client secret` no backend** — ele é Resource Server puro (TechSpec §8), e o
authorization code com PKCE vive no frontend, com client público. Em produção, a
credencial de banco vai por **Docker secret montado como arquivo**, não por
variável: variável vaza em inspeção de contêiner.

**Verificação de que subiu certo:** os probes são separados.
`GET /actuator/health/liveness` observa só o processo;
`GET /actuator/health/readiness` inclui banco e conexão de escuta `LISTEN`. O
`healthcheck` do `compose` para o `backend` usa **readiness**. Nunca ponha o
listener no liveness: instabilidade do banco mataria todas as instâncias ao mesmo
tempo, durante a janela de backoff em que elas se recuperariam sozinhas
(TechSpec §8).

**Testes:**

```
docker compose -f docker/compose.test.yaml run --rm backend-test
docker compose -f docker/compose.test.yaml run --rm e2e
```

Os testes unitários de regra rodam sem contêiner de infraestrutura — é o que
mantém curto o ciclo de quem implementa.

**Testcontainers dentro de contêiner precisa de acesso ao daemon do host.** O
serviço `backend-test` recebe o socket Docker do host montado, e os contêineres
que o Testcontainers cria são irmãos, não filhos — o que exige que eles fiquem na
mesma rede do serviço de teste. DinD foi descartado pelo custo de camada e pela
perda de cache. Isso concede ao serviço de teste o equivalente a acesso root no
host, e é aceito apenas em desenvolvimento e CI. Deixou de ser decisão desta
feature: é
[ADR-012](../../decisions/ADR-012-testcontainers-por-socket-do-host.md) e norma
de `infra/docker/testing.md` §3, que também obriga a montagem a existir só em
`compose.test.yaml`.

**A versão do PostgreSQL vem de variável única** (`POSTGRES_IMAGE`), consumida
pelo `compose` e pelo Testcontainers. "A mesma imagem do compose" não pode ser
convenção em prosa: sem fixação verificável, divergir de versão reintroduz, mais
devagar, o problema que motivou tirar o H2.

O serviço `e2e` traz os browsers na imagem e roda na mesma rede do frontend. O
teste de broadcast de RNF-002 sobe 3 réplicas do backend atrás do proxy.

### CI

Nada disso executa fora da máquina de quem desenvolve enquanto não houver:

- runner com Docker disponível e o socket acessível ao job;
- cache de dependências Maven e npm entre execuções — sem ele o ciclo em contêiner
  fica caro o bastante para as pessoas contornarem;
- publicação do relatório JaCoCo (gate de 80%) e do relatório Playwright.

Essa lista não é mais desta feature: `infra/docker/definition-of-done.md` §4 a
assume como os pré-requisitos que qualquer plataforma de CI terá de satisfazer, e
deixa a escolha da plataforma como dívida nomeada. Não bloqueia — os 19 critérios
da coleção são verificáveis na máquina de quem desenvolve. Registry e política de
tag são a outra dívida nomeada
([ADR-013](../../decisions/ADR-013-proveniencia-de-imagem-por-digest-sem-registry.md)):
não há publicação, então vale digest obrigatório, `latest` proibido e imagem local
marcada com a revisão de código.

**A ressalva que bloqueava está fechada.** A pendência 02 do `state.md` existia
porque `infra/docker` era stub enquanto SDR-003 punha Docker no caminho crítico de
toda execução, inclusive de teste — este documento estava escrevendo a norma de
containerização do workspace por omissão. A coleção foi elaborada em 2026-09-09 a
partir exatamente deste rascunho, e o que era decisão local aqui passou a ser
norma verificável lá. O sentido da dependência inverteu: quem implementa cumpre
`infra/docker/definition-of-done.md`, e este documento só diz como aplicá-lo a
este sistema.

---

## 4. Cenários principais por RF

Contratos completos em [`contracts/`](contracts/). O que segue é o caminho de
entrada de cada bloco, com um exemplo executável por bloco.

### Sessão e projetos — RF-001, RF-002, RF-017 a RF-019

`GET /v1/sessao` autoprovisiona a pessoa a partir do `sub` do token. Provedor
fora do ar devolve `503` com `Retry-After` e **nenhum caminho alternativo**
(ADR-006).

```
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/v1/sessao
```

Configuração de etapas é substituição do fluxo inteiro por `PUT`, não edição
etapa a etapa: sem isso existiria estado intermediário sem etapa terminal.

### Board e tarefas — RF-003 a RF-013

Toda escrita carrega `origem: { etapaId, condicao, versao }`:

```
curl -X POST http://localhost:8080/v1/tarefas/$ID/tomada \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"origem":{"etapaId":"'$ETAPA'","condicao":"AGUARDANDO_TOMADA","versao":3}}'
```

Três respostas possíveis e todas esperadas: `200` com o cartão; `200` sem novo
evento quando o efeito já estava aplicado (RN-031); `409` com `estadoAtual`
quando outra pessoa chegou antes.

### Fila e consultas — RF-014 a RF-016

```
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/v1/projetos/$PROJ/tempo-por-etapa?de=2026-01-01T00:00:00Z"
```

As três séries vêm lado a lado, sem campo de total. Sem histórico, a resposta é
`{"medido": false}` — não `porEtapa` com zeros.

### Tempo real — RF-020

STOMP em `ws://localhost:8080/ws`, JWT no `CONNECT`, inscrição em
`/topic/board/{projetoId}` e `/user/queue/fila`. O evento diz **que** algo mudou;
o detalhe vem por REST. Lacuna de `seq` ou reconexão → `GET .../board`.

---

## 5. Pontos de atenção

Cada item abaixo já custou uma decisão registrada. Ignorá-lo produz código que
passa em teste e viola a especificação.

1. **Três dimensões de estado independentes** (RN-002): etapa, condição de
   trabalho e marca de impedimento. Impedimento jamais é coluna do board —
   modelá-lo como etapa faz o tempo de impedimento se sobrepor ao de permanência
   e destrói a comparabilidade que é o objetivo do produto —, e também **não é
   valor de `condicao`**: a marca é derivada de `impedimento` com
   `desfecho IS NULL`, coexiste com qualquer condição não terminal e só o
   registro do desfecho a apaga (RN-032). Se você se pegar escrevendo
   `condicao = IMPEDIDA` ou zerando a marca dentro de uma movimentação, está
   reintroduzindo o defeito que INC-01 encontrou.
2. **Três séries que nunca se somam** (RN-008): permanência, espera de tomada e
   impedimento. Coexistem sobre o mesmo instante. Nenhuma tabela e nenhuma
   resposta tem campo de total.
3. **`intervalo_tarefa` não tem coluna de pessoa** (RN-014). Não a acrescente
   "para depois filtrar". A ausência é o que torna a regra estrutural em vez de
   disciplina — e nenhum teste falha quando alguém acrescenta um filtro.
4. **Mover não fecha impedimento** (RN-009, SCN-006.3). Fecha permanência e
   espera de tomada na origem, abre as duas no destino, e deixa o impedimento
   correndo. É a decisão Q-01 do demandante, contra a recomendação inicial.
5. **Escrita e projeção na mesma transação.** Log, `tarefa`, `intervalo_tarefa` e
   `impedimento` commitam juntos. Separar faz o board exibir um estado e o
   agregado outro.
6. **`NOTIFY` em `afterCommit`**, nunca dentro da transação. Evento anunciado e
   depois revertido é pior que evento atrasado.
7. **Permissão no serviço, sobre a participação real.** Nunca sobre papel ou id
   vindos do cliente, nunca só na interface (RNF-004, RN-015). Rota não mapeada
   nega por padrão.
8. **`404`, não `403`, quando revelar a existência já é vazamento** (SCN-002.3).
9. **`409` sem `estadoAtual` reprova cenário congelado.** Quem perdeu a corrida
   precisa saber o que aconteceu, não que houve erro.
10. **"Ainda não medido" ≠ zero** (RN-025). Devolver zero é mentir com número.
11. **Renomear etapa preserva o `id`** (RN-021); alteração de configuração vale
    dali em diante (RN-022). Histórico não é reescrito.
12. **Nada é acionado por decurso de prazo** (RN-026). Não existe agendador neste
    sistema; a ausência de escalonamento é decisão herdada do `/shape`.
13. **`dados jsonb` nunca recebe dado real de cliente** (IDSD 4.10.1).
14. **`seq` vem do banco, nunca de contador em memória** (SDR-004). E há um único
    publicador de `NOTIFY`: a porta em `afterCommit`, sem gatilho no esquema.
15. **Autorização de canal WebSocket não é permanente.** Mudança de participação
    invalida inscrição na mesma transação; a sessão morre no `exp` do token.
16. **Admin global é promovido por `sub`, uma única vez, com registro em `WARN`**
    (ADR-010). Nunca por e-mail.
17. **Reconstruir a projeção exige janela sem escrita no projeto**, com bloqueio
    consultivo. Reconstruir concorrentemente produz o defeito que a rotina cura.
    O bloqueio é **assimétrico**: quem escreve toma em modo compartilhado — de
    modo que escritores convivem entre si —, e só a reconstrução toma em modo
    exclusivo.
18. **A reconstrução é total na série de tempo e parcial no estado** (SDR-006).
    `intervalo_tarefa` é reproduzida por inteiro; em `tarefa` e `impedimento` a
    rotina reescreve só os campos que o log determina e **não cria nem remove
    linha**. `raia_id`, `titulo` e `descricao` não são curáveis por ela — o log
    não os carrega, e acrescentá-los desfaria RN-023.
19. **Os cenários estão congelados.** Divergência entre código e cenário se
    resolve por emenda no PRD ou por correção do código — nunca reescrevendo o
    cenário durante `/implement`.
