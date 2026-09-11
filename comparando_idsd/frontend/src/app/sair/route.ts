import { cookies, headers } from 'next/headers'
import { NextResponse } from 'next/server'

import { IDENTIDADE, limparSessao } from '@/lib/auth/cookies'
import { urlDeEncerramento } from '@/lib/auth/oidc'
import { config } from '@/lib/config'

/**
 * Sair encerra as **duas** sessões.
 *
 * Apagar só os cookies daqui deixaria a sessão de SSO do provedor de pé, e a
 * próxima visita a qualquer rota interna entraria de novo sem digitar nada —
 * uma tela dizendo "você saiu" enquanto a pessoa continua dentro. Por isso a
 * saída passa pelo `end_session_endpoint`, levando o `id_token_hint`.
 *
 * É `POST` e não `GET`, e a origem é verificada — ACH-14 da revisão. Enquanto
 * era navegação, qualquer site derrubava a sessão por link ou imagem: os
 * cookies são `sameSite: 'lax'`, que os envia em navegação de topo, e o efeito
 * não é local, porque leva junto a sessão do provedor. Não é roubo de dado, mas
 * é negação de serviço acionável de fora, e a correção é barata.
 *
 * A verificação é do cabeçalho `Origin` contra `config.aplicacao`, e não contra
 * `Host`: `Host` é enviado por quem chama e a comparação seria consigo mesma.
 * Formulário de terceiro manda `Origin` da página que o hospeda, que é
 * exatamente o que se quer distinguir; requisição sem `Origin` é recusada, o
 * que fecha o caso do agente que o omite.
 */
export async function POST() {
  const origem = (await headers()).get('origin')
  if (origem !== new URL(config.aplicacao).origin) {
    return new NextResponse('origem nao autorizada', { status: 403 })
  }

  const idToken = (await cookies()).get(IDENTIDADE)?.value
  await limparSessao()
  return NextResponse.redirect(urlDeEncerramento(idToken), { status: 303 })
}
