# Segurança — Frontend Next.js (BFF)

> Complementa [`_shared/api-security.md`](../../_shared/api-security.md). A app é um BFF:
> os route handlers em `src/app/api/*` são a superfície de exposição.

## 1. Fronteira cliente / servidor

- Segredos (`SECRET_KEY`, `API_USER_KEY`, chaves privadas, `jose`, cripto) **só** em Server
  Component / route handler / `proxy.ts`. Nunca em `'use client'`.
- Único env que chega ao browser: `NEXT_PUBLIC_*`. Revisar cada novo `NEXT_PUBLIC_*` — não
  expor nada que não possa ser público.
- Leitura e validação de env centralizada em `src/config/envs.ts`; falhar no boot se faltar
  variável obrigatória.
- `.env*` fora do git (`.env.example` é o contrato). Segredos reais vêm de
  `configuration/secret` (OpenShift), nunca do `configmap` nem da imagem.

## 2. Route handlers (BFF)

- Toda chamada ao `API_GATEWAY_URL` passa pelo cliente em `src/lib/axios` — nunca `fetch`
  cru espalhado, nunca o browser falando direto com o gateway.
- Handler valida input com Zod antes de repassar; valida/repassa credencial de sessão;
  molda a resposta (sem vazar campos internos do upstream).
- Erro do upstream é normalizado (`src/errors/parser.ts`) para status semântico + corpo
  modelado (RFC 7807 — ver [`_shared/api-standards.md`](../../_shared/api-standards.md)).
  Nunca repassar stack, mensagem crua ou status opaco do gateway.
- Autenticação ao gateway conforme `_shared/api-security.md` (token da sessão; sem repassar
  cegamente token de terceiro).
- `DEBUG_API_REQUESTS_HABILITADO` só liga log verboso fora de produção; esse log nunca
  contém corpo com dado sensível.

## 3. Sessão

- Dados de sessão criptografados com `SECRET_KEY` (`src/utils/auth`, `src/utils/crypto`).
- JWT lido/validado com `jose` no servidor: assinatura, `exp`, emissor esperado.
- Cookies de sessão: `httpOnly`, `secure`, `sameSite` restritivo.
- Flags de bypass (`BYPASS_*`) são **apenas** para dev/lab; garantir que resolvem `false` em
  produção e revisar qualquer uso novo.

## 4. Controle de acesso a funcionalidades

- `src/lib/feature-access` decide exibição/uso de feature (pilotos, perfis). A checagem de
  UI **não** é controle de segurança — o route handler e o gateway continuam responsáveis
  pela autorização real.
- `NEXT_PUBLIC_BYPASS_CONTROLE_ACESSO_HABILITADO` só em ambiente controlado.

## 5. Cabeçalhos e CSP

Mantidos em `next.config.mjs` — ao mexer, preservar:

- `Content-Security-Policy: frame-ancestors 'none'` + `X-Frame-Options: SAMEORIGIN` (anti-clickjacking)
- `Strict-Transport-Security` com `preload`
- `X-Content-Type-Options: nosniff`, `Referrer-Policy: same-origin`, `X-XSS-Protection: 0`
- CSP report-only endurecida: `default-src 'self'`, `object-src 'none'`, `form-action 'self'`;
  toda nova origem de script/connect entra na allowlist explicitamente e justificada.
- `poweredByHeader: false`; `productionBrowserSourceMaps: false` (anti reverse engineering).

## 6. Entrada e saída de conteúdo

- HTML dinâmico sanitizado com `dompurify` antes de render; nunca `dangerouslySetInnerHTML`
  sem sanitizar.
- Máscaras (`imask`) não substituem validação — validar sempre com Zod.
- Links externos: `rel="noopener noreferrer"` (ver [`../_shared/sonarqube.md`](../_shared/sonarqube.md)).
- Nunca montar URL do gateway por concatenação de input do usuário — usar `src/routes/gateway-routes.ts`.

## 7. Dependências

`npm audit` / scanner corporativo no pipeline; CVE Critical/High bloqueia
(ver [`_shared/vulnerable-and-proprietary-libs.md`](../../_shared/vulnerable-and-proprietary-libs.md)).
`package-lock.json` commitado.

## Checklist

- [ ] Nenhum segredo fora de Server/handler; todo novo `NEXT_PUBLIC_*` revisado
- [ ] Env só via `config/envs.ts`; secret via `configuration/secret`, não imagem/configmap
- [ ] Gateway só pelos route handlers via `src/lib/axios`; input validado com Zod
- [ ] Erro do upstream normalizado; sem vazar stack/campo interno
- [ ] Sessão criptografada; JWT validado com `jose`; cookie `httpOnly`/`secure`/`sameSite`
- [ ] Flags `BYPASS_*` = `false` em produção
- [ ] Headers/CSP do `next.config.mjs` preservados; nova origem na allowlist justificada
- [ ] HTML dinâmico via `dompurify`; sem `dangerouslySetInnerHTML` cru
- [ ] `npm audit` sem CVE Critical/High pendente; lockfile commitado
