# Definition of Done — Frontend Next.js

Uma task está **concluída** quando todos os critérios obrigatórios forem atendidos.

## Obrigatórios

### Qualidade estática (gate real do projeto)
- [ ] `npm run lint` sem erro (warnings de `exhaustive-deps` / import order resolvidos, não silenciados)
- [ ] `npm run typecheck` (`tsc --noEmit`) sem erro
- [ ] `npm run prettier` sem diferença
- [ ] `npm run knip` sem código/dependência morta introduzida
- [ ] Sem `any`, sem `@ts-ignore`/`eslint-disable` sem justificativa, sem `console.log`
- [ ] SonarQube: sem issue Blocker/Critical nova (ver [`../_shared/sonarqube.md`](../_shared/sonarqube.md))

### Testes
- [ ] `npm test` verde
- [ ] Componente/hook/util novo com teste em `__tests__/` colocado
- [ ] Schema Zod novo com teste de aceitação e de rejeição
- [ ] Route handler novo com teste de 2xx, erro de validação e erro de upstream

### Contrato e dados
- [ ] Acesso a backend só via route handler `app/api/*` + `@tanstack/react-query` — nenhum componente falando com o gateway
- [ ] Route handler devolve status semântico + corpo modelado (ver [`_shared/api-standards.md`](../../_shared/api-standards.md)), sem vazar upstream
- [ ] Path/URL via `src/routes/`, sem string solta
- [ ] Estado de servidor no react-query, não duplicado no Redux

### Segurança
- [ ] Nenhum segredo em `'use client'`; novo `NEXT_PUBLIC_*` revisado e justificado
- [ ] Env lido só em `config/envs.ts`
- [ ] Headers/CSP do `next.config.mjs` intactos; nova origem de script/connect na allowlist justificada
- [ ] Nada sensível em log (token, PEM, PIN block, CPF/CNPJ, payload de login)
- [ ] `npm audit` sem CVE Critical/High pendente (ver [`_shared/vulnerable-and-proprietary-libs.md`](../../_shared/vulnerable-and-proprietary-libs.md))

### Acessibilidade e design
- [ ] Primitivos Radix para interação; `addon-a11y` sem violação nova no Storybook
- [ ] Cores/spacing/tipografia dos tokens do design system ([`design-system.md`](design-system.md) / [`design-tokens.json`](design-tokens.json)), sem valores avulsos
- [ ] Story criada/atualizada para componente novo ou alterado

### UX de erro e carregamento
- [ ] Estados de loading, vazio e erro tratados (skeleton/`react-error-boundary`/diálogo de erro)

## Recomendados

- [ ] Cobertura dos fluxos críticos (login, PIX, pagamento, transferência)
- [ ] `README`/docs do sistema atualizados se houve nova rota, env ou mudança de execução
- [ ] Server Component por padrão; `'use client'` só nas folhas

## Checklist de code review

- [ ] `'use client'` no ponto certo (folha, não layout)
- [ ] Context value memoizado; sem componente aninhado instável
- [ ] `key` real em listas; sem index como key
- [ ] Erros via `src/errors/*`; `parser.ts` para erro do gateway
- [ ] Formulário com `react-hook-form` + `zodResolver`; schema no lugar certo (`_schemas/` ou `src/schemas/`)
- [ ] Imports ordenados; alias `@/`; sem import morto
- [ ] Sem lógica de negócio pesada em `page.tsx` — extraída para hook/handler/util
