import type { NextConfig } from 'next'

/**
 * O navegador nunca fala com o serviço REST diretamente.
 *
 * Isso não é obtido por reescrita de caminho: **nenhum** código de tela chama a
 * API. Toda leitura sai de Server Component e toda escrita de Server Action, e
 * `src/lib/api/cliente.ts` declara `import 'server-only'`, o que torna a
 * violação erro de compilação em vez de convenção que alguém lembra.
 *
 * Uma reescrita `/v1/*` chegou a existir aqui e foi removida: com tudo no
 * servidor ela não é usada por ninguém, e o que ela deixaria de pé é um proxy
 * aberto — o navegador alcançaria o serviço pela origem da aplicação sem passar
 * pelo cookie de sessão que o cliente REST anexa.
 *
 * `API_URL_INTERNO` continua sendo lido no servidor, em `src/lib/config.ts`, e
 * por isso não leva o prefixo `NEXT_PUBLIC_`.
 */
/**
 * Cabeçalhos de segurança de `frontend/nextjs` §5.
 *
 * A CSP vai em **report-only** porque é o que a coleção prescreve: endurecida
 * na redação e sem poder de quebrar a tela enquanto ninguém observa relatório.
 * `default-src 'self'` cobre o provedor de identidade sem allowlist — a ida até
 * ele é redirecionamento de navegação de topo, que CSP não governa, e a troca
 * de código por token acontece no servidor do Next, não no navegador.
 *
 * Desvio declarado: `report-uri`/`report-to` ficam ausentes porque não há
 * coletor de relatório neste sistema, e apontar para um que não existe é o
 * mesmo silêncio com aparência de instrumentação. Reabrir quando houver
 * observabilidade de frontend.
 */
const CSP = [
  "default-src 'self'",
  "object-src 'none'",
  "form-action 'self'",
  "frame-ancestors 'none'",
  "base-uri 'self'",
].join('; ')

const cabecalhos = [
  // Fora da CSP report-only também, porque anti-clickjacking não pode depender
  // de um cabeçalho que por definição não bloqueia nada.
  { key: 'Content-Security-Policy', value: "frame-ancestors 'none'" },
  { key: 'Content-Security-Policy-Report-Only', value: CSP },
  { key: 'X-Frame-Options', value: 'SAMEORIGIN' },
  { key: 'X-Content-Type-Options', value: 'nosniff' },
  { key: 'Referrer-Policy', value: 'same-origin' },
  { key: 'X-XSS-Protection', value: '0' },
]

/**
 * HSTS só fora de desenvolvimento, e a razão é medida e não preferência: o
 * Chromium trata `localhost` como origem confiável, portanto **aceita** o
 * cabeçalho mesmo sobre HTTP e passa a forçar `https://localhost:3000`, que
 * não existe — emiti-lo em desenvolvimento derruba a aplicação inteira no
 * navegador, e o estado sobrevive ao teste porque fica gravado no perfil.
 */
const hsts = {
  key: 'Strict-Transport-Security',
  value: 'max-age=63072000; includeSubDomains; preload',
}

const nextConfig: NextConfig = {
  output: 'standalone',
  poweredByHeader: false,
  productionBrowserSourceMaps: false,
  async headers() {
    const lista = process.env.NODE_ENV === 'production' ? [...cabecalhos, hsts] : cabecalhos
    return [{ source: '/:path*', headers: lista }]
  },
}

export default nextConfig
