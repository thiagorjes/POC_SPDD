import { cookies, headers } from 'next/headers'

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
 *
 * Mas ele **falha fechado**, e é isso que ACH-12 corrigiu. A versão anterior
 * lia `NEXT_PUBLIC_APP_URL`, isto é, uma variável de configuração pública: por
 * trás de terminação TLS, onde a aplicação recebe `http` e a variável não foi
 * ajustada, o token de acesso viajava sem a marca e nada quebrava — o defeito
 * era silencioso e permanente. A decisão passou a vir da requisição que está
 * sendo servida, e o esquema inseguro só é aceito quando o host é local. Host
 * desconhecido em `http` é tratado como produção mal configurada, e o cookie
 * sai seguro: pior é a sessão não se formar do que o token trafegar em claro.
 */
const ACESSO = 'idsd_acesso'
export const IDENTIDADE = 'idsd_identidade'
export const VERIFICADOR = 'idsd_verificador'
export const ESTADO = 'idsd_estado'
export const DESTINO = 'idsd_destino'

const LOCAIS = ['localhost', '127.0.0.1', '[::1]']

async function seguro() {
  const cabecalhos = await headers()
  // `x-forwarded-proto` primeiro: por trás de terminação TLS é o único que
  // conta, e é justamente o caso em que a leitura anterior errava.
  const esquema = cabecalhos.get('x-forwarded-proto')?.split(',')[0]?.trim()
  if (esquema) return esquema === 'https'

  // Só a porta sai: partir em `:` quebraria `[::1]`.
  const hospedeiro = (cabecalhos.get('host') ?? '').replace(/:\d+$/, '')
  return !LOCAIS.includes(hospedeiro)
}

async function padrao() {
  return {
    httpOnly: true,
    sameSite: 'lax' as const,
    secure: await seguro(),
    path: '/',
  }
}

export async function gravarSessao(tokens: Tokens) {
  const jar = await cookies()
  const opcoes = await padrao()
  jar.set(ACESSO, tokens.access_token, opcoes)
  if (tokens.id_token) jar.set(IDENTIDADE, tokens.id_token, opcoes)
}

export async function gravarTransacao(verificador: string, estado: string, destino: string) {
  const jar = await cookies()
  // Vida curta: são credenciais de uma ida ao provedor, não da sessão.
  const efemero = { ...(await padrao()), maxAge: 600 }
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
