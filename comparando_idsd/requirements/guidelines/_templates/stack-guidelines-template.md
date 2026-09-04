# Template — Guidelines de uma nova stack

> Copie a estrutura abaixo para `backend/<stack>/` ou `frontend/<stack>/` ao introduzir uma
> nova linguagem/framework. Cada arquivo **materializa** o `_shared/` correspondente com
> ferramentas e sintaxe concretas — não repita o princípio, referencie-o e mostre o "como".

## Arquivos obrigatórios

| Arquivo | Materializa | Deve conter |
|---|---|---|
| `stack.md` | — | Linguagem + versão, framework principal + versão, libs e finalidade, libs proprietárias (tabela), banco/persistência, build, testes, infra. Versões fixas. |
| `architecture.md` | `_shared/architecture-principles.md` | Estrutura de pastas concreta, mapa de camadas × dependências permitidas, padrões táticos da stack (tipo imutável, DI, config tipada, handler global de erro), protocolo de uso pela IA, checklist. |
| `coding-standards.md` | `_shared/logging-and-levels.md`, `_shared/git-workflow.md` | Nomenclatura (tabela), DI, tipos imutáveis, tratamento de erro, logging (lib concreta), complexidade/lint, comentários, supressão de alerta proibida, checklist. |
| `testing.md` | — | Estratégia por camada (tabela ferramenta × foco), mock de integração externa, exemplos mínimos, asserts exaustivos de contrato, meta de cobertura. |
| `definition-of-done.md` | `_shared/api-standards.md`, `_shared/api-security.md`, `_shared/vulnerable-and-proprietary-libs.md` | Critérios obrigatórios (qualidade/lint, SCA, doc de API, contrato HTTP, segurança) + recomendados + checklist de code review. Só o que é verificável. |

## Arquivos condicionais

| Arquivo | Quando |
|---|---|
| `<framework>.md` (ex.: `spring-boot.md`) | O framework tem convenções próprias fortes (config, segurança, resiliência, observabilidade). |
| `database.md` | A stack acessa banco relacional (migrations, N+1, projeções, transação, índices). |
| `integrations.md` | A stack consome APIs externas (cliente HTTP, timeout, retry, isolamento de models). |
| `openapi-swagger.md` | A stack expõe API documentada por OpenAPI. |
| `sonarqube.md` | Há regras Sonar específicas da linguagem no perfil `Banestes_way` (frontend: usar `frontend/_shared/sonarqube.md`). |
| `design-system.md` + `design-tokens.json` + `components/` | Stack de **frontend** com UI: materializa `frontend/_shared/design-principles.md` com valores de token, mecânica de composição e API dos primitivos. Inventário de telas/componentes de rota **não** entra — é artefato do sistema. |

## Regras de redação

- **Não duplicar** o transversal. Onde o `_shared/` já decide algo (status HTTP, níveis de log,
  política de CVE, SemVer, transcode RH-SSO), **linkar** e mostrar só a implementação.
- Todo arquivo termina com um **checklist** acionável.
- Exemplos de código mínimos e idiomáticos da stack.
- Versões sempre fixas; sem "latest".
- Idioma: pt_BR.

## Passo a passo

1. Criar `backend/<stack>/` ou `frontend/<stack>/`.
2. Copiar os obrigatórios; preencher `stack.md` primeiro (decide o resto).
3. Adicionar os condicionais aplicáveis.
4. Cross-linkar cada arquivo com o `_shared/` que ele materializa.
5. Atualizar a tabela de stacks no [`../README.md`](../README.md).
6. Rodar `/guidelines` para revisão final de consistência.
