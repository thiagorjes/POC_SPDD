# Definition of Done — Infra Docker

> Materializa
> [`../../_shared/vulnerable-and-proprietary-libs.md`](../../_shared/vulnerable-and-proprietary-libs.md)
> (política de CVE aplicada à imagem base),
> [`../../_shared/api-security.md`](../../_shared/api-security.md) (segredo e superfície
> exposta) e [`../../_shared/git-workflow.md`](../../_shared/git-workflow.md).
> Regras de escrita: [`coding-standards.md`](coding-standards.md).

Um item só está aqui se alguém consegue dizer, olhando um diff ou rodando um
comando, se foi cumprido.

## 1. Obrigatório

| # | Critério | Verificador |
|---|---|---|
| 1 | `hadolint` sem achado em todo `Dockerfile` | `docker run --rm -i hadolint/hadolint:2.12.0-alpine < <Dockerfile>` |
| 2 | `docker compose config -q` passa em todos os arquivos de compose | comando |
| 3 | Toda imagem base fixada por digest, com linha na tabela de [`stack.md`](stack.md) | `grep -L '@sha256:' */Dockerfile` sem saída |
| 4 | Estágio final não-root | `docker run --rm <imagem> id -u` ≠ `0` |
| 5 | Multi-stage onde há build; imagem final sem compilador nem código-fonte | revisor humano |
| 6 | Nenhum segredo em `ENV`, `ARG`, `.env` versionado ou camada de imagem | `trivy image --scanners secret <ref>` sem achado |
| 7 | Varredura de CVE da imagem construída sem vulnerabilidade `CRITICAL` ou `HIGH` sem tratativa registrada | `trivy image --severity HIGH,CRITICAL <ref>`; tratativa segue a política transversal de CVE |
| 8 | Todo serviço de longa duração com `healthcheck` que sonda prontidão | `docker compose ps` reporta `healthy` |
| 9 | Todo `depends_on` com `condition:` | `docker compose config` |
| 10 | Liveness e readiness separados; o `compose` usa readiness | revisor humano |
| 11 | Migração de schema em serviço dedicado, não no boot da aplicação | revisor humano ([ADR-011](../../../../docs/decisions/ADR-011-migracao-de-schema-em-servico-dedicado.md)) |
| 12 | `down -v` seguido de `up --build` sobe o ambiente do zero | comando |
| 13 | Socket do daemon montado somente em serviço de teste | `grep -l 'docker.sock' docker/compose.yaml` sem saída |
| 14 | Log só em `stdout`/`stderr` | revisor humano |
| 15 | Nenhuma porta de infraestrutura publicada em `compose.test.yaml` | `docker compose -f docker/compose.test.yaml config` |
| 16 | Mudança de imagem base em commit próprio, com a razão na mensagem | revisor humano |

## 2. Recomendado

Não bloqueia, e a ausência não precisa de justificativa.

- Imagem final baseada em distribuição mínima, quando a stack suporta.
- Rótulos OCI (`org.opencontainers.image.*`) com origem e revisão de código.
- Tamanho da imagem final acompanhado entre revisões — crescimento súbito costuma
  ser camada de build vazando para o runtime.

## 3. Proveniência de imagem — o que vale hoje

**Não há registry e não há publicação em produção.** Este workspace roda
exclusivamente em Docker local (ADR-009 do sistema IDSD). A alternativa de registry
interno on-premise foi considerada e recusada por ora: normatizar publicação em
infraestrutura que não existe produz regra que ninguém consegue verificar, que é o
oposto do que esta biblioteca serve. Ver
[ADR-013](../../../../docs/decisions/ADR-013-proveniencia-de-imagem-por-digest-sem-registry.md).

O que a ausência de registry **não** dispensa:

| # | Critério | Verificador |
|---|---|---|
| 17 | `latest` proibido em qualquer referência, inclusive local | `grep -rn ':latest' docker/` sem saída |
| 18 | Imagem construída localmente é marcada com a revisão de código que a originou | `docker images` exibe a revisão na coluna TAG, não `latest` |
| 19 | Deploy a partir de imagem sem proveniência declarada é proibido | revisor humano — hoje não há deploy; a regra existe para o dia em que houver |

**Dívida nomeada:** registry, política de tag de release e retenção. Reabrir quando
houver ambiente compartilhado ou publicação fora da máquina de quem desenvolve.

## 4. CI — dívida nomeada

Não há CI neste workspace, e a coleção não inventa plataforma. O que qualquer
pipeline terá de satisfazer, quando existir, já está decidido:

- runner com daemon Docker acessível ao job — sem isso a suíte não roda
  ([ADR-012](../../../../docs/decisions/ADR-012-testcontainers-por-socket-do-host.md));
- cache de dependência entre execuções (ver [`testing.md`](testing.md) §4);
- execução dos itens 1, 2, 6, 7 e 12 desta lista como etapa que reprova o build;
- publicação dos relatórios de cobertura e de teste de ponta a ponta que a coleção
  da stack exigir.

Enquanto isso, os 19 critérios acima são verificados na máquina de quem desenvolve
e conferidos em revisão. É por isso que nenhum deles depende de pipeline.

## 5. Checklist de code review

1. Imagem base tem digest e está na tabela do `stack.md`?
2. Estágio final é não-root e não carrega ferramenta de build?
3. Algum segredo entrou por variável, `ARG` ou arquivo versionado?
4. Todo `depends_on` tem `condition:`, e o healthcheck sonda prontidão de verdade?
5. Alguém colapsou liveness e readiness, ou moveu migração para o boot?
6. O socket do daemon aparece fora do `compose.test.yaml`?
7. `down -v && up --build` foi executado nesta mudança?
8. Supressão de lint tem justificativa e autorizador nomeados?
9. A mudança de imagem base veio em commit próprio?
