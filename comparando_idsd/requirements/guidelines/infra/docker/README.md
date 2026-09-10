# Guidelines — Infra Docker

> **Status: elaborada** em 2026-09-09. Substitui o stub aberto em 2026-09-04.

Coleção da camada `infra`. Governa o **ambiente** em que os sistemas rodam —
topologia de contêineres, rede, volumes, ordem de subida, healthcheck, segredo,
proveniência de imagem e ambiente de teste. O conteúdo de cada imagem (qual JDK,
qual gerenciador de pacotes) continua na coleção da stack correspondente.

| Arquivo | Assunto |
|---|---|
| [`stack.md`](stack.md) | Versões de Engine/Compose, ferramental de verificação, imagens base em uso, alvo de execução, estrutura de `docker/` |
| [`architecture.md`](architecture.md) | Topologia, redes, volumes, ordem de subida, healthcheck, migração, segredo, log |
| [`coding-standards.md`](coding-standards.md) | Nomenclatura, regras de `Dockerfile` e de `compose`, comentário, versionamento |
| [`testing.md`](testing.md) | `compose.test.yaml`, suíte que sobe contêiner, cache, verificação da própria infraestrutura |
| [`definition-of-done.md`](definition-of-done.md) | 19 critérios verificáveis, recomendados, proveniência de imagem e dívida de CI |

Transversais materializados:
[`../../_shared/vulnerable-and-proprietary-libs.md`](../../_shared/vulnerable-and-proprietary-libs.md),
[`../../_shared/api-security.md`](../../_shared/api-security.md),
[`../../_shared/architecture-principles.md`](../../_shared/architecture-principles.md),
[`../../_shared/logging-and-levels.md`](../../_shared/logging-and-levels.md) e
[`../../_shared/git-workflow.md`](../../_shared/git-workflow.md).

Condicionais do [template](../../_templates/stack-guidelines-template.md) —
`<framework>.md`, `database.md`, `integrations.md`, `openapi-swagger.md`,
`sonarqube.md`, `design-system.md` — **não se aplicam**: são condicionais de camada
de aplicação, e esta coleção não expõe API, não acessa banco e não tem UI.

## Decisões que a originaram

- [ADR-011](../../../../docs/decisions/ADR-011-migracao-de-schema-em-servico-dedicado.md) — migração em serviço dedicado.
- [ADR-012](../../../../docs/decisions/ADR-012-testcontainers-por-socket-do-host.md) — socket do host para a suíte; DinD descartado.
- [ADR-013](../../../../docs/decisions/ADR-013-proveniencia-de-imagem-por-digest-sem-registry.md) — digest obrigatório, sem registry.

## Dívidas nomeadas

| Dívida | Reabrir quando |
|---|---|
| Registry, política de tag de release e retenção | Houver publicação fora da máquina de quem desenvolve |
| CI: runner, cache, publicação de relatório | Houver plataforma de CI decidida |

Dívida nomeada não é ausência: os pré-requisitos de ambas já estão escritos em
[`definition-of-done.md`](definition-of-done.md) §3 e §4, para que a decisão futura
não recomece do zero.
