import { cookies } from 'next/headers'

import type { Tokens } from '@/lib/auth/oidc'

/**
 * Os cookies da sessão são todos `httpOnly`: nenhum script da página os lê, e
 * por isso um XSS não vira roubo de token. `sameSite: 'lax'` deixa o retorno do
 * provedor funcionar — é navegação de topo — e barra envio em requisição de
 * terceiro.
 *
 * `secure` acompanha o esquema em vez de ser fixo: em `http://localhost` o
 * navegador descarta cookie marcado como seguro, e a sessão não se formaria no
 * ambiente em que o produto é desenvolvido e testado.
 */
export const ACESSO = 'idsd_acesso'
export const IDENTIDADE = 'idsd_identidade'
export const VERIFICADOR = 'idsd_verificador'
export const ESTADO = 'idsd_estado'
export const DESTINO = 'idsd_destino'

const seguro = () => (process.env.NEXT_PUBLIC_APP_URL ?? '').startsWith('https://')

const padrao = () => ({
  httpOnly: true,
  sameSite: 'lax' as const,
  secure: seguro(),
  path: '/',
})

export async function gravarSessao(tokens: Tokens) {
  const jar = await cookies()
  jar.set(ACESSO, tokens.access_token, padrao())
  if (tokens.id_token) jar.set(IDENTIDADE, tokens.id_token, padrao())
}

export async function gravarTransacao(verificador: string, estado: string, destino: string) {
  const jar = await cookies()
  // Vida curta: são credenciais de uma ida ao provedor, não da sessão.
  const efemero = { ...padrao(), maxAge: 600 }
  jar.set(VERIFICADOR, verificador, efemero)
  jar.set(ESTADO, estado, efemero)
  jar.set(DESTINO, destino, efemero)
}

export async function limparTransacao() {
  const jar = await cookies()
  for (const nome of [VERIFICADOR, ESTADO, DESTINO]) jar.delete(nome)
}

export async function limparSessao() {
  const jar = await cookies()
  for (const nome of [ACESSO, IDENTIDADE, VERIFICADOR, ESTADO, DESTINO]) {
    jar.delete(nome)
  }
}

export async function tokenDeAcesso() {
  return (await cookies()).get(ACESSO)?.value
}
