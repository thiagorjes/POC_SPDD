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
const nextConfig: NextConfig = {
  output: 'standalone',
}

export default nextConfig
