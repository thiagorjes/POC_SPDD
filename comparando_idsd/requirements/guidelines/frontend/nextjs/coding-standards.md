# Padrões de Código — Frontend Next.js

> Fonte da verdade executável: `eslint.config.mjs`, `.prettierrc.js`, `tsconfig.json` do
> sistema. Este documento resume o contrato e o que o lint não pega.
> Commits: [`_shared/git-workflow.md`](../../_shared/git-workflow.md). Logs: [`_shared/logging-and-levels.md`](../../_shared/logging-and-levels.md).

## TypeScript

- `strict` sempre. Proibido `@typescript-eslint/no-explicit-any` (`any` é erro) — usar
  `unknown` + narrowing.
- **`type`, nunca `interface`** (`consistent-type-definitions: ['error', 'type']`).
- Import de tipo com `import { type X }` inline (`consistent-type-imports`).
- `catch (e: unknown)` — `useUnknownInCatchVariables`. Fazer narrow antes de usar.
- `switch` sobre union deve ser exaustivo (`switch-exhaustiveness-check`, `noFallthroughCasesInSwitch`).
- `@ts-ignore` só com descrição; preferir `@ts-expect-error`.
- Sem variável/parâmetro não usado (prefixo `_` para intencional).

## React

- `react/react-in-jsx-scope` off (React 19). `prop-types` off — tipar props com `type`.
- Sem componente aninhado instável (`no-unstable-nested-components`), sem valor de contexto
  construído inline (`jsx-no-constructed-context-values` — memoizar).
- `key` real em lista; `no-array-index-key` (warn) — evitar índice como key.
- `self-closing-comp`, `jsx-boolean-value`, `jsx-curly-brace-presence` (sem `{'texto'}`).
- Hooks: `rules-of-hooks` (error), `exhaustive-deps` (warn — resolver, não silenciar).
- `'use client'` só quando necessário (ver [`architecture.md`](architecture.md) §4).

## Imports

- Ordem automática (`import-helpers/order-imports`): `react` → `next` → libs → `@/…` → `~` →
  relativos; linha em branco entre grupos; alfabético.
- Sem import duplicado, sem self-import, sem segmento de path inútil. Usar alias `@/`.

## Nomenclatura

| Artefato | Convenção | Exemplo |
|---|---|---|
| Arquivo / pasta | kebab-case (pt_BR em termo de negócio) | `formatar-valor-brl.ts`, `minhas-chaves/` |
| Componente React | PascalCase | `ChavesPixCard` |
| Hook | `use-` + kebab no arquivo, camelCase no símbolo | `use-feature-access.ts` → `useFeatureAccess` |
| Schema Zod | `*.schema.ts` | `chave-pix.schema.ts` |
| Route handler | `route.ts` na pasta do recurso | `app/api/pix/route.ts` |
| Constante | UPPER_SNAKE_CASE | `MAX_TENTATIVAS` |
| Tipo | PascalCase | `type TipoChavePix` |

## Estilo

- Prettier: aspas simples, `;`, `trailingComma: all`, 2 espaços, LF. Sem formatação manual.
- `eqeqeq` smart, `no-var`, `prefer-const`, `object-shorthand`, `no-unneeded-ternary`,
  `prefer-for-of`.
- `no-param-reassign` (warn) — não mutar parâmetro.
- Promises: sem `no-floating-promises`; `await` só em thenable; `only-throw-error` (lançar `Error`).

## Estilização / CSS

- Tailwind utilitário; compor classes condicionais com `cn()` (`clsx` + `tailwind-merge`),
  variantes com `class-variance-authority`. Sem CSS inline arbitrário.
- Sem `!important` (ver [`../_shared/sonarqube.md`](../_shared/sonarqube.md)).
- Tokens e escala de design vêm do design system ([`design-system.md`](design-system.md) +
  [`design-tokens.json`](design-tokens.json)) — não inventar cor/spacing fora dos tokens.
- Acessibilidade: usar primitivos Radix; validar com Storybook `addon-a11y`.
  Ver [`../_shared/design-principles.md`](../_shared/design-principles.md).

## Logging

- `no-console` — permitido **apenas** `console.warn` e `console.error`. Sem `console.log`.
- Log de servidor (route handler) segue níveis de [`_shared/logging-and-levels.md`](../../_shared/logging-and-levels.md);
  nunca logar token, chave PEM, PIN block, CPF/CNPJ sem máscara, payload de login.

## Tratamento de erro

- Erros de domínio como classes em `src/errors/` (`app-error`, `bad-request-error`,
  `unauthorized-error`, `forbidden-error`, `not-found-error`); `parser.ts` normaliza erro do
  gateway. Route handler devolve status semântico + corpo modelado (ver
  [`_shared/api-standards.md`](../../_shared/api-standards.md)), sem stack do upstream.
- UI usa `react-error-boundary` para falha de render; diálogos de erro via providers dedicados.

## Comentários

Só o **porquê** não óbvio (o `eslint.config.mjs` e o `next.config.mjs` do sistema são bom
exemplo). `removeComments` no build.

## Proibido silenciar análise

Sem `// eslint-disable` sem justificativa, sem `@ts-ignore` sem descrição, sem `any` para
"fazer passar". Corrigir estruturalmente.

## Checklist

- [ ] Sem `any`; `type` em vez de `interface`; import de tipo inline
- [ ] `switch` exaustivo sobre union; `catch (e: unknown)` com narrow
- [ ] `'use client'` só nas folhas; contexto memoizado
- [ ] Imports na ordem do `import-helpers`; alias `@/`
- [ ] Arquivos kebab-case; componentes PascalCase; hooks `use-*`
- [ ] `cn()`/`cva` para classes; sem `!important`; cores/spacing via tokens
- [ ] Só `console.warn`/`console.error`; nada sensível em log
- [ ] Erros via `src/errors/*`; handler devolve status semântico modelado
- [ ] Nenhuma supressão de lint/TS sem justificativa
