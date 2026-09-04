# Guidelines — Infra Docker

> **Status: não elaborada.** Transversais aplicáveis já existem em
> [`../../_shared/`](../../_shared/): `vulnerable-and-proprietary-libs.md` (política de
> CVE, que vale para imagem base tanto quanto para dependência de aplicação) e
> `git-workflow.md`.

Enquanto esta coleção não existir, a norma de containerização vigente é a que
cada stack declara no seu `definition-of-done.md` — ver
[`../../backend/java/definition-of-done.md`](../../backend/java/definition-of-done.md) e
[`../../frontend/nextjs/definition-of-done.md`](../../frontend/nextjs/definition-of-done.md).
Isso cobre o caso de imagem por serviço, e **não** cobre o que é transversal ao
ambiente: rede, volumes, healthcheck, ordem de subida, gestão de segredo,
política de tag e registry.

Para criar esta coleção, siga [`../../_templates/stack-guidelines-template.md`](../../_templates/stack-guidelines-template.md)
e o processo em [`../../README.md`](../../README.md) (seção "Gerar guidelines para uma nova stack").

Arquivos esperados: `stack.md`, `architecture.md`, `coding-standards.md`,
`testing.md`, `definition-of-done.md`.

Assuntos a decidir na entrevista, já levantados e ainda em aberto:

- Imagem base e política de fixação de tag — digest ou tag semântica; hoje o
  backend fixa `maven:3.9-eclipse-temurin-25` / `eclipse-temurin:25-jre`.
- Multi-stage build obrigatório e usuário não-root no runtime.
- Healthcheck e `depends_on: condition: service_healthy` — o pré-requisito de
  provedor OIDC no ar descrito em
  [`../../backend/java/testing.md`](../../backend/java/testing.md) §5 depende disso.
- Gestão de segredo: proibição de `ENV` com credencial, origem dos valores.
- Registry, política de tag de release e varredura de vulnerabilidade da imagem,
  materializando [`../../_shared/vulnerable-and-proprietary-libs.md`](../../_shared/vulnerable-and-proprietary-libs.md).
- Alvo futuro OpenShift/Kubernetes (ADR-008) e o que isso restringe hoje.
