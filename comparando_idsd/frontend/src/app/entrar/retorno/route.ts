import { cookies } from 'next/headers'
import { NextRequest, NextResponse } from 'next/server'

import { DESTINO, ESTADO, VERIFICADOR, gravarSessao, limparTransacao } from '@/lib/auth/cookies'
import { trocarCodigo } from '@/lib/auth/oidc'

/**
 * Volta do provedor: é este o `redirect_uri` registrado no realm.
 *
 * Toda saída de falha vai para `/entrar` com a razão na query, e nunca para uma
 * segunda forma de entrar — ADR-006 não deixa caminho alternativo, e oferecer
 * um aqui seria inventá-lo justamente no arquivo onde ele passaria despercebido.
 */
const recusa = (requisicao: NextRequest, razao: string) =>
  NextResponse.redirect(new URL(`/entrar?erro=${razao}`, requisicao.nextUrl.origin))

export async function GET(requisicao: NextRequest) {
  const parametros = requisicao.nextUrl.searchParams

  if (parametros.get('error')) return recusa(requisicao, 'recusado')

  const codigo = parametros.get('code')
  const estadoRecebido = parametros.get('state')

  const jar = await cookies()
  const verificador = jar.get(VERIFICADOR)?.value
  const estadoEsperado = jar.get(ESTADO)?.value
  const destino = jar.get(DESTINO)?.value ?? '/projetos'

  if (!codigo || !verificador || !estadoEsperado) return recusa(requisicao, 'expirado')

  // Sem esta comparação, um código de autorização obtido em outra sessão
  // poderia ser plantado aqui por link — é o CSRF do fluxo de autorização.
  if (estadoRecebido !== estadoEsperado) return recusa(requisicao, 'estado')

  const tokens = await trocarCodigo(codigo, verificador)
  if (!tokens) return recusa(requisicao, 'provedor')

  await gravarSessao(tokens)
  await limparTransacao()

  return NextResponse.redirect(new URL(destino, requisicao.nextUrl.origin))
}
