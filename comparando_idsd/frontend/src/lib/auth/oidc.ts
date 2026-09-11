import { createHash, randomBytes } from 'node:crypto'

import { config, ROTA_RETORNO } from '@/lib/config'

/**
 * Authorization code com PKCE em client público (ADR-003).
 *
 * Não há segredo de cliente aqui, e não é omissão: `idsd-web` é declarado
 * `publicClient` no realm justamente porque qualquer segredo embarcado num
 * frontend é um segredo publicado. O que substitui o segredo é o PKCE — o
 * verificador nunca sai do servidor do Next, e o código de autorização
 * interceptado não vale nada sem ele.
 *
 * A troca do código por token acontece no servidor, e o token vive em cookie
 * `httpOnly`. Guardá-lo em `localStorage` o exporia a qualquer script da
 * página, que é a forma mais barata de transformar um XSS em roubo de sessão.
 */

const base64url = (buffer: Buffer) => buffer.toString('base64url')

export function novoVerificador() {
  return base64url(randomBytes(32))
}

export function desafioDe(verificador: string) {
  return base64url(createHash('sha256').update(verificador).digest())
}

export const redirectUri = () => `${config.aplicacao}${ROTA_RETORNO}`

const endpoint = (base: string, nome: string) =>
  `${base}/realms/${config.realm}/protocol/openid-connect/${nome}`

export function urlDeAutorizacao(opcoes: {
  desafio: string
  estado: string
}) {
  const parametros = new URLSearchParams({
    client_id: config.clientId,
    response_type: 'code',
    scope: 'openid profile email',
    redirect_uri: redirectUri(),
    state: opcoes.estado,
    code_challenge: opcoes.desafio,
    code_challenge_method: 'S256',
  })
  return `${endpoint(config.provedorPublico, 'auth')}?${parametros}`
}

export function urlDeEncerramento(idToken: string | undefined) {
  const parametros = new URLSearchParams({
    post_logout_redirect_uri: `${config.aplicacao}/entrar`,
    client_id: config.clientId,
  })
  // Sem `id_token_hint` o provedor pede confirmação em vez de encerrar, e a
  // sessão de SSO sobrevive ao "sair" — quem voltasse entraria de novo sem
  // digitar nada, que é exatamente o oposto do que sair significa.
  if (idToken) parametros.set('id_token_hint', idToken)
  return `${endpoint(config.provedorPublico, 'logout')}?${parametros}`
}

export type Tokens = {
  access_token: string
  id_token?: string
}

async function pedirToken(corpo: Record<string, string>): Promise<Tokens | null> {
  const resposta = await fetch(endpoint(config.provedorInterno, 'token'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ client_id: config.clientId, ...corpo }),
    cache: 'no-store',
  })
  if (!resposta.ok) return null
  return (await resposta.json()) as Tokens
}

/**
 * Não há renovação por refresh token, e é decisão e não esquecimento: quando o
 * token de acesso expira, o serviço responde `401` e a aplicação recomeça o
 * authorization code. A sessão de SSO do provedor continua de pé, então a ida
 * e a volta são silenciosas — ninguém digita nada de novo. Guardar refresh
 * token no navegador acrescentaria uma credencial de vida longa para poupar um
 * redirecionamento que a pessoa não vê.
 */
export const trocarCodigo = (codigo: string, verificador: string) =>
  pedirToken({
    grant_type: 'authorization_code',
    code: codigo,
    redirect_uri: redirectUri(),
    code_verifier: verificador,
  })
