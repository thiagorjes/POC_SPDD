import { randomBytes } from 'node:crypto'
import { NextRequest, NextResponse } from 'next/server'

import { gravarTransacao } from '@/lib/auth/cookies'
import { desafioDe, novoVerificador, urlDeAutorizacao } from '@/lib/auth/oidc'

/**
 * Início do authorization code.
 *
 * É rota e não componente de tela por uma razão de mecanismo: componente de
 * servidor não pode gravar cookie, e o verificador do PKCE precisa ser gravado
 * **antes** de a pessoa sair para o provedor. Como é rota, o caminho inteiro —
 * `/projetos` → aqui → provedor — é uma sequência de redirecionamentos do
 * servidor, e quem chega por link não vê tela intermediária nenhuma.
 */
export async function GET(requisicao: NextRequest) {
  const pedido = requisicao.nextUrl.searchParams.get('destino') ?? '/projetos'
  // Só destino interno: aceitar URL absoluta aqui transformaria a entrada num
  // redirecionador aberto, que é como se leva alguém autenticado para fora.
  const destino = pedido.startsWith('/') && !pedido.startsWith('//') ? pedido : '/projetos'

  const verificador = novoVerificador()
  const estado = randomBytes(16).toString('base64url')

  await gravarTransacao(verificador, estado, destino)

  return NextResponse.redirect(
    urlDeAutorizacao({ desafio: desafioDe(verificador), estado }),
  )
}
