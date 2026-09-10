# Revisão técnica — TASK-01.2 (revisão parcial)

_Data: 2026-09-10 | Revisor: agente | Épico: EPIC-01 | PR: —_
_Commits revisados: 3481f3d..3f1da26_

> O revisor não escreve código. Achado é devolvido a quem implementa; correção
> feita pelo revisor faz o revisor revisar a si mesmo.

Revisão **parcial**: o EPIC-01 tem oito tasks e duas estão concluídas. O
GATE-REVISAO-TECNICA e o GATE-NFR são de fechamento de épico e permanecem
reprovados. Os achados da revisão de TASK-01.1 continuam válidos; ACH-01, ACH-02
e ACH-03 daquele relatório estão fechados e foram reconferidos em disco.


## Escopo revisado

| Item | Valor |
| --- | --- |
| Tasks | TASK-01.1, TASK-01.2 |
| Cenários entregues | SCN-001.1 |
| Arquivos | 18 de produção/infra, 2 de task, 1 de estado |
| Suíte | não compila; 0 testes executáveis (Red medido em TASK-01.1) |

- **Cenários congelados alterados no período:** nenhum
- **Step definitions alterados no período:** nenhum

Fase 0 verificada no git: `git diff 8346946..HEAD` sobre `*.feature`,
`backend/src/test`, `frontend/tests` e `e2e` retorna vazio. A linha de base que
ACH-01 da revisão anterior exigia agora existe, e é o commit `8346946`.

> Qualquer das duas linhas diferente de "nenhum" sem emenda registrada reprova
> o GATE-VERIFICACAO-INDEPENDENTE, e a revisão para aqui.


## Conformidade com a especificação

| Cenário | Passa | Implementado conforme a task | Observação |
| --- | --- | --- | --- |
| SCN-001.1 | não | parcial | O cenário é verificado por suíte que ainda não compila (classes de produção das tasks seguintes). A infraestrutura que ele exige existe e sobe; o cenário só é executável ao fim do épico |

Aderência às treze ações da task: doze cumpridas e verificáveis em disco. A
décima — "colocar os contêineres irmãos criados pela suíte na mesma rede do
serviço de teste" — não é alcançável por configuração com a suíte congelada, e o
desvio está declarado no histórico com a intenção satisfeita por
`host.docker.internal` e `TESTCONTAINERS_HOST_OVERRIDE`. Aceito: satisfazer a
letra exigiria tocar em `TesteDeIntegracao`, que o `/implement` não pode.

Critérios de aceite: 2, 3, 4, 5, 6 e 7 cumpridos e reconferidos aqui
(`compose config -q` limpo nos dois arquivos; zero `:latest`; zero `FROM`
externo sem `@sha256:`; `docker.sock` só em `compose.test.yaml`). O critério 1
está bloqueado em TASK-01.4 e o 8 é parcial por falta de `frontend/package.json`
(TASK-01.7) — ambos declarados no histórico da task, e nenhum contornável de
dentro dela.

- **Escopo além do especificado:** cinco arquivos fora da tabela —
  `docker/migracao/entrypoint.sh`, `docker/keycloak/health.sh`,
  `docker/keycloak/README.md`, `docker/proxy/nginx.conf` e `docker/secrets/` —
  mais `docker/.gitattributes` e a emenda de
  `backend/src/main/resources/application.yml`. Todos justificados no histórico
  e nenhum acrescenta comportamento de produto. Não é achado.


## Envelopes de RNF

| RNF | Envelope | Medido | Como | Situação |
| --- | --- | --- | --- | --- |
| todos | ver PRD §RNF | — | — | não medido |

Nenhum RNF é mensurável antes do fechamento do EPIC-01: não há endpoint de
produto no ar (RNF-001, RNF-009, RNF-010), não há tela (RNF-005, RNF-006) e a
suíte não compila. O arnês de RNF-002 foi **construído** nesta task
(`backend-broadcast` + `proxy-broadcast`, perfil `broadcast`) e não foi
exercitado — ver ACH-04.

> RNF não medido não passa por omissão. Se a instrumentação não existe, o
> GATE-NFR reprova — a ausência de medição é o achado.


## Achados

| ID | Severidade | Tipo | Local | Descrição | Destino |
| --- | --- | --- | --- | --- | --- |
| ACH-01 | bloqueante | segurança | `docker/migracao/Dockerfile:3`, `backend/Dockerfile:18` | O item 7 do `definition-of-done.md` de `infra/docker` é obrigatório e **nunca foi executado**: o histórico da task registra apenas `trivy --scanners secret`, que é o item 6. Medido nesta revisão com `trivy 0.58.0 --severity HIGH,CRITICAL --scanners vuln`: `idsd/migracao:dev` acusa **5 CRITICAL e 56 HIGH** em pacotes de sistema da base `flyway/flyway:11-alpine` — openssl `CVE-2026-31789`, gnutls `CVE-2026-33845`, libexpat `CVE-2026-25210`, libpng `CVE-2025-64720`, musl `CVE-2026-40200`, zlib `CVE-2026-22184` —, todos com versão corrigida disponível; `idsd/backend:dev` acusa 8 HIGH no binário `usr/bin/pebble` da base `eclipse-temurin:25-jre`. Nenhuma tratativa registrada em lugar nenhum. A imagem que aplica migration é justamente a única do ambiente com permissão de alterar o schema | /implement |
| ACH-02 | bloqueante | segurança | `docker/keycloak/realm.json:11-30,79-156` | Os quatro papéis de negócio entram como **realm roles** do provedor e são atribuídos globalmente a cada usuário. ADR-003 decide o oposto — Keycloak autentica, permissão é modelada na aplicação — e BDR-001 fixa o papel por par usuário↔projeto. O realm passa a afirmar, no token, que `ana` é `project_admin` em toda parte, e é esse token que TASK-01.4 e TASK-01.5 vão ler para montar autorização: derivar papel da claim é o caminho mais curto a partir deste arquivo, e ele concede acesso transversal a projeto de que a pessoa não participa. Cobrir os quatro papéis, que é o que a ação pede, não obriga a modelá-los no provedor | /implement |
| ACH-03 | bloqueante | segurança | `backend/Dockerfile:32` (dependências do artefato) | A varredura de CVE da imagem do backend acusa **27 achados HIGH/CRITICAL em dependências Java** empacotadas no jar — entre elas `jackson-databind` `CVE-2026-54512` (execução arbitrária de código, corrigida em 2.18.8/2.21.4) e `jackson-core` `GHSA-r7wm-3cxj-wff9`. Vêm da árvore do Spring Boot 3.5.8 fixada em TASK-01.1 e atravessam o produto inteiro; a mesma varredura na imagem de migração acusa 2 CRITICAL e 48 HIGH em Java. Item 7 do DoD, sem tratativa registrada | /implement |
| ACH-04 | relevante | código | `docker/compose.test.yaml:91-105` | O arnês de RNF-002 entra na rede `idsd-net` **externa** e aponta `BANCO_URL` para `postgres:5432`, que é o banco de desenvolvimento. O cabeçalho do próprio arquivo declara a invariante contrária — "a suíte não pode alcançar um banco de desenvolvimento por acidente" — e aqui ela é quebrada por construção, não por acidente: medir broadcast escreve evento e projeção na base de trabalho de quem desenvolve, e o dado da medição fica misturado ao dado de uso. Como os serviços de que ele depende vivem em outro projeto Compose, o arnês também não tem `depends_on` possível: subi-lo com o ambiente parcialmente no ar falha sem dizer por quê | /implement |
| ACH-05 | relevante | código | `frontend/Dockerfile:10-14` | No estágio `desenvolvimento`, `npm ci` e `COPY . .` rodam como root e o `USER node` vem depois: `/app`, `/app/node_modules` e o contexto copiado ficam pertencendo a root, e o processo de desenvolvimento precisa escrever `/app/.next` em tempo de execução. O arranque tende a falhar com `EACCES`. Não foi exercitado porque `frontend/package.json` nasce em TASK-01.7, e por isso o defeito só aparecerá lá, com a causa a dois arquivos de distância | /implement |
| ACH-06 | relevante | spec | `requirements/guidelines/infra/docker/stack.md:47-53` | O item 3 do DoD exige que **toda** imagem base fixada por digest tenha linha na tabela de `stack.md`. Três das que entraram nesta task não têm: `flyway/flyway:11-alpine`, `mcr.microsoft.com/playwright:v1.56.0-noble` e `nginx:1.29-alpine`. A task proíbe explicitamente tocar na coleção, então o conflito não é da implementação — é a norma sendo elaborada depois das tasks, terceira ocorrência da mesma classe (ACH-02 e ACH-05 da revisão anterior). Dono real é `/guidelines`; a tabela registra `/techspec` porque o validador só aceita donos da cadeia de especificação | /techspec |
| ACH-07 | relevante | spec | `docker/compose.yaml:81` | `stack.md` registra a versão do provedor de identidade como "versão fixa por sistema, fonte ADR-003", e o ADR-003 é silencioso quanto a versão. A 26.4 foi escolhida dentro da task, por digest. Já registrado no histórico da própria task; repetido aqui porque o dono é outro | /techspec |
| ACH-08 | menor | código | `docker/proxy/nginx.conf:20-21` | `Connection: upgrade` é enviado incondicionalmente, em vez de derivado de `$http_upgrade` por `map`. Requisição HTTP comum atravessando o proxy — inclusive o probe de readiness — vai com cabeçalho de upgrade sem `Upgrade` correspondente. Não impede o WebSocket que o arnês mede; sujeita o resto | /implement |
| ACH-09 | menor | código | `backend/Dockerfile:32` | O `COPY` do artefato traz o nome do jar com a versão literal (`kanban-0.1.0-SNAPSHOT.jar`). Alterar a versão no `pom.xml` quebra a construção da imagem com erro de arquivo inexistente, que não aponta para a causa | /implement |
| ACH-10 | menor | código | `backend/Dockerfile:26` | `curl` fixado em `8.18.0-1ubuntu2.5`. Fixar é o que a norma quer, mas o índice do Ubuntu remove a versão anterior quando publica a correção, e nesse dia o build passa a falhar com "Version not found" — sem que nada no repositório tenha mudado | /implement |
| ACH-11 | relevante | segurança | `docker/keycloak/realm.json:4,51-61` | `sslRequired: "none"`, cliente público `idsd-e2e` com concessão direta de senha habilitada e senhas literais versionadas. Coerente com realm de desenvolvimento, e é para isso que ele existe; o que falta é o arquivo dizer isso de si mesmo — não há marca que impeça alguém de importá-lo em outro ambiente. O `README.md` ao lado explica escolhas de importação, não vigência. Não rebaixado a menor: rebaixar achado de segurança exige aprovador humano nomeado, e esta revisão parcial não tem um | /implement |
| ACH-12 | menor | código | `docker/.env.example:20` | `REVISAO` tem `dev` por padrão e foi assim que as imagens locais foram marcadas (`idsd/backend:dev`, `idsd/migracao:dev`). O item 18 do DoD pede a revisão de código na coluna TAG. Não é `latest`, e a proibição do item 17 está cumprida; a proveniência que o ADR-013 quer, porém, não está na imagem construída | /implement |

**Severidade:**

- **bloqueante** — impede o merge. Correção obrigatória antes do gate.
- **relevante** — merge permitido com registro; entra no backlog com prazo.
- **menor** — registrado, sem prazo.


## Análise de segurança

| Verificação | Resultado | Evidência |
| --- | --- | --- |
| Entrada validada nas fronteiras | n/a | Não há endpoint de produto nesta task |
| Autorização verificada por operação | achado | ACH-02 — o realm modela papel de negócio como papel global do provedor, contra ADR-003 e BDR-001 |
| Segredo fora do código e do log | ok | `trivy --scanners secret` exit 0 nas duas imagens; senha por `secrets:` em todo serviço que a usa, `POSTGRES_PASSWORD_FILE` no banco, argumento efêmero de processo no Flyway, `configtree` no backend. O único valor literal é o descartável de desenvolvimento, versionado por decisão declarada |
| Dado sensível fora de log e mensagem de erro | ok | `entrypoint.sh` falha nomeando o caminho do segredo, nunca o conteúdo |
| Dependência nova sem vulnerabilidade conhecida | achado | ACH-01 e ACH-03 — item 7 do DoD nunca executado; 5 CRITICAL e 56 HIGH na base da migração, 27 HIGH/CRITICAL nas dependências Java do backend |

Verificado e conforme, sem achado: socket do daemon apenas em
`compose.test.yaml` (ADR-012); estágio final não-root nas três imagens
(`kanban` 10001, `migracao` 10001, `node`); multi-stage sem toolchain na imagem
final do backend e no `runtime` do frontend; nenhuma porta de infraestrutura
publicada em `compose.test.yaml`; healthcheck do provedor sondando a porta de
gerenciamento, que nunca é publicada.


## Guardrails extraídos

| Guardrail | Origem | Onde passa a valer |
| --- | --- | --- |
| Fechar task que constrói imagem exige as **duas** varreduras do `trivy` — `--scanners secret` (item 6) e `--severity HIGH,CRITICAL` (item 7). Registrar a saída das duas no histórico; uma sozinha não é o critério | ACH-01, ACH-03 | `guidelines/infra/docker/definition-of-done.md` §5 |
| Realm de desenvolvimento não modela papel de negócio como papel do provedor quando a decisão de RBAC põe a permissão na aplicação. O provedor autentica; papel por escopo vive no banco | ACH-02 | `guidelines/_shared/api-security.md` |
| Serviço de medição não compartilha banco com ambiente de trabalho, mesmo quando reusa a rede | ACH-04 | `guidelines/infra/docker/testing.md` |
| `USER` não-root vem **antes** do `COPY` do código, ou o `COPY` usa `--chown`: trocar de usuário depois deixa o diretório de trabalho inescrevível para quem vai rodar | ACH-05 | `guidelines/infra/docker/coding-standards.md` |
| Toda imagem base nova entra na tabela de `stack.md` **na mesma mudança** que a introduz; quando a task proíbe tocar na coleção, o pedido de linha é achado de revisão, não silêncio | ACH-06 | `guidelines/infra/docker/definition-of-done.md` item 3 |


## Veredicto

- **GATE-REVISAO-TECNICA:** reprovado
- **GATE-NFR:** reprovado
- **Bloqueantes em aberto:** 0 — os três foram fechados pelo `/implement` em
  2026-09-10, depois desta revisão; ver a nota abaixo
- **Revisor humano:** — (revisão parcial; não fecha épico)

Os dois gates são de fechamento de épico e seguem reprovados pela ausência de
qualquer RNF medido, que nenhuma das duas tasks concluídas torna mensurável.
Quando a revisão foi escrita, também os três bloqueantes os reprovavam.

### Fechamento dos bloqueantes (2026-09-10, `/implement`)

| Achado | Como fechou | Prova |
| --- | --- | --- |
| ACH-01 | Base da migração para `flyway/flyway:13.6.0-alpine` (a linha 11 parou de receber build a montante e não tem digest mais novo), `apk upgrade` na construção e remoção nominal dos três drivers JDBC não usados; base de runtime do backend para `eclipse-temurin:25-jre-noble`, porque a tag genérica traz Ubuntu 26.04 com `/usr/bin/pebble` vulnerável e sem digest corrigido | `trivy --severity HIGH,CRITICAL --scanners vuln`: **0 e 0** nas duas imagens |
| ACH-02 | `realm.json` perdeu o bloco `roles` e todo `realmRoles`; o papel pretendido de cada conta virou tabela no `README.md` do realm, com a concessão declarada como sendo no banco, junto da participação | Token real por direct grant para `ana`: `sub` fixo e `preferred_username`, **sem** `realm_access` nem `resource_access`. A suíte congelada não lê nenhuma das duas claims |
| ACH-03 | Spring Boot 3.5.8 → 3.5.16, com sobreposição de duas versões gerenciadas que a 3.5.16 ainda não alcançou (Tomcat 10.1.59, driver PostgreSQL 42.7.13), cada uma com a razão e a condição de remoção escritas no `pom.xml` | Mesma varredura: **0 HIGH/CRITICAL em Java** nas duas imagens; ambiente sobe com `Apache Tomcat/10.1.59`, o que confirma que a sobreposição vale em runtime |

Reconferido depois da correção: `trivy --scanners secret` em exit 0 nas duas
imagens, hadolint limpo nos três Dockerfiles, `docker compose config -q` limpo
nos dois arquivos, e a subida completa na ordem esperada — banco saudável,
migração `Exited (0)`, provedor saudável, backend de pé em 5,3 s. Os probes
seguem `401` sem token e `UP` com token, como na TASK-01.1: liberá-los é
TASK-01.4.

> Bloqueante em aberto reprova, sem exceção e sem waiver: a policy proíbe waiver
> para verificação independente e para dados.


## Fora deste artefato — regras negativas

- **Correção de código** — pertence ao `/implement`. O revisor descreve o
  achado e o devolve; não conserta.
- **Requisito novo** — achado de spec vira devolução ao `/prd`, não requisito
  escrito aqui.
- **Decisão de arquitetura** — pertence ao `/techspec`.
- **Cenário Gherkin** — congelado; alteração exige emenda no PRD.
- **Reescrita do plano de tasks** — pertence ao `/tasks`.
