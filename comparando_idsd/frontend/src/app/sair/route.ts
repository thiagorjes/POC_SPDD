import { cookies } from 'next/headers'
import { NextResponse } from 'next/server'

import { IDENTIDADE, limparSessao } from '@/lib/auth/cookies'
import { urlDeEncerramento } from '@/lib/auth/oidc'

/**
 * Sair encerra as **duas** sessões.
 *
 * Apagar só os cookies daqui deixaria a sessão de SSO do provedor de pé, e a
 * próxima visita a qualquer rota interna entraria de novo sem digitar nada —
 * uma tela dizendo "você saiu" enquanto a pessoa continua dentro. Por isso a
 * saída passa pelo `end_session_endpoint`, levando o `id_token_hint`.
 */
export async function GET() {
  const idToken = (await cookies()).get(IDENTIDADE)?.value
  await limparSessao()
  return NextResponse.redirect(urlDeEncerramento(idToken))
}
