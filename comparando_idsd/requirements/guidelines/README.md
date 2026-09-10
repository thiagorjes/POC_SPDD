# Biblioteca de Guidelines

Normas técnicas compartilhadas entre sistemas. **As guidelines não pertencem a um
sistema** — `backend/java` vale para todo sistema Java. Cada sistema declara, no
`guidelines.yaml` da própria raiz, quais coleções o governam.

Esta biblioteca é escrita por uma única skill: `/guidelines`. Todas as outras
apenas consomem.

## Estrutura

```
guidelines/
  _shared/              transversais, agnósticos de stack
  _templates/           molde de coleção nova
  <camada>/
    _shared/            transversais da camada (opcional)
    <stack>/            a coleção concreta
```

`<camada>` é aberto — `backend`, `frontend`, `infra`, `data`, o que o workspace
tiver. `<stack>` é a linguagem ou o framework que decide o "como". Nunca crie uma
camada sem pelo menos uma stack dentro dela.

## A regra de corte

**Se trocar a stack torna a frase falsa, ela pertence à coleção da stack. Se
continua verdadeira, pertence a `_shared/`.**

"Erro 4xx não é logado como `error`" é transversal. "Usar `@Slf4j` do Lombok" é de
`backend/java`. A coleção da stack **materializa** o transversal — linka e mostra
o "como" —, nunca o reescreve. Coleção que reescreve o transversal diverge dele
com o tempo, e a versão errada é sempre a que alguém leu.

## Transversais

| Arquivo | Assunto |
|---|---|
| [`_shared/architecture-principles.md`](_shared/architecture-principles.md) | Camadas, dependências permitidas, acoplamento |
| [`_shared/api-standards.md`](_shared/api-standards.md) | Contrato HTTP, versionamento, mapa exceção → status |
| [`_shared/api-security.md`](_shared/api-security.md) | Autenticação, autorização, dados sensíveis em trânsito |
| [`_shared/logging-and-levels.md`](_shared/logging-and-levels.md) | Níveis de log e dados proibidos em log |
| [`_shared/git-workflow.md`](_shared/git-workflow.md) | Branches, commits, política de merge |
| [`_shared/vulnerable-and-proprietary-libs.md`](_shared/vulnerable-and-proprietary-libs.md) | Política de CVE, SemVer, bibliotecas proprietárias |
| [`frontend/_shared/design-principles.md`](frontend/_shared/design-principles.md) | Método de design, acessibilidade, composição |
| [`frontend/_shared/sonarqube.md`](frontend/_shared/sonarqube.md) | Regras Sonar comuns a stacks de frontend |

## Coleções

| Coleção | Estado | Escopo |
|---|---|---|
| [`backend/java`](backend/java/) | Elaborada | Java 25 + Spring Boot. Obrigatórios completos; condicionais: `spring-boot.md`, `database.md`, `integrations.md`, `openapi-swagger.md`, `sonarqube.md` |
| [`frontend/nextjs`](frontend/nextjs/) | Elaborada | Next.js + TypeScript. Obrigatórios completos; condicionais: `design-system.md` + `design-tokens.json` + `components/`, `security.md` |
| [`frontend/react`](frontend/react/) | Stub | Declarada como dívida. Ver o README da coleção |
| [`infra/docker`](infra/docker/) | Elaborada | Ambiente de execução: topologia, rede, volumes, ordem de subida, healthcheck, segredo, proveniência de imagem, ambiente de teste. Obrigatórios completos; condicionais não se aplicam à camada |

Stub é dívida declarada, não ausência: existe para impedir que duas pessoas
recriem a mesma coleção do zero em paralelo.

## Gerar guidelines para uma nova stack

1. Criar `<camada>/<stack>/`.
2. Copiar os obrigatórios de [`_templates/stack-guidelines-template.md`](_templates/stack-guidelines-template.md);
   preencher `stack.md` primeiro — é ele que decide quais condicionais existem.
   Versões fixas; "latest" não é resposta.
3. Adicionar os condicionais aplicáveis.
4. Cross-linkar cada arquivo com o `_shared/` que ele materializa.
5. Acrescentar a linha na tabela de coleções acima.
6. Declarar a coleção no `guidelines.yaml` do sistema que a usa.
7. Rodar o validador.

Toda regra carrega verificador declarado — comando de lint, teste, ou
explicitamente "revisor humano". Regra que ninguém consegue conferir olhando um
diff não é norma: é opinião.

## Validação

```
python <sistema>/.agents/skills/guidelines/scripts/check_guidelines.py \
    --raiz requirements/guidelines --sistema .
```

**Falha** o que é decidível a partir do disco: obrigatório ausente, coleção sem
cross-link para o transversal, índice fora de sincronia, coleção declarada no
`guidelines.yaml` e inexistente, DR citado e ausente de `docs/decisions/`,
`design-system.md` sem `design-tokens.json`, versão não fixada.
**Avisa** o que é cheiro na prosa: hedge, stub declarado.

## Histórico

- **2026-09-09** — `infra/docker` elaborada (ADR-011, ADR-012, ADR-013). Registry e
  CI ficam como dívidas nomeadas, com os pré-requisitos já escritos.
- **2026-09-04** — Índice criado; `infra/docker` aberta como stub. A coleção plana
  do CRUDAO que vivia na raiz foi movida para `../_legado/guidelines-crudao/`
  após extração de dois achados para `backend/java`.
