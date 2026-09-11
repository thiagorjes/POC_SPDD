import { cookies } from 'next/headers'
import { NextRequest, NextResponse } from 'next/server'

import { DESTINO, ESTADO, VERIFICADOR, gravarSessao, limparTransacao } from '@/lib/auth/cookies'
import { destinoInterno } from '@/lib/auth/destino'
import { trocarCodigo } from '@/lib/auth/oidc'
import { config } from '@/lib/config'

/**
 * Volta do provedor: é este o `redirect_uri` registrado no realm.
 *
 * Toda saída de falha vai para `/entrar` com a razão na query, e nunca para uma
 * segunda forma de entrar — ADR-006 não deixa caminho alternativo, e oferecer
 * um aqui seria inventá-lo justamente no arquivo onde ele passaria despercebido.
 *
 * Toda saída é ancorada em `config.aplicacao` e **não** em `nextUrl.origin`.
 * São duas razões: a origem da requisição segue o endereço de bind do servidor
 * — com `-H 0.0.0.0` ela vira `http://0.0.0.0:3000`, que navegador nenhum
 * alcança, e foi assim que a suíte quebrou —, e ela é derivada do cabeçalho
 * `Host`, que quem chama controla.
 */
const recusa = (razao: string) =>
  NextResponse.redirect(new URL(`/entrar?erro=${razao}`, config.aplicacao))

export async function GET(requisicao: NextRequest) {
  const parametros = requisicao.nextUrl.searchParams

  if (parametros.get('error')) return recusa('recusado')

  const codigo = parametros.get('code')
  const estadoRecebido = parametros.get('state')

  const jar = await cookies()
  const verificador = jar.get(VERIFICADOR)?.value
  const estadoEsperado = jar.get(ESTADO)?.value
  // Revalidado aqui, e não só na gravação: é este o ponto que emite o
  // redirecionamento, e o valor chega por cookie — que a pessoa controla.
  const destino = destinoInterno(jar.get(DESTINO)?.value)

  if (!codigo || !verificador || !estadoEsperado) return recusa('expirado')

  // Sem esta comparação, um código de autorização obtido em outra sessão
  // poderia ser plantado aqui por link — é o CSRF do fluxo de autorização.
  if (estadoRecebido !== estadoEsperado) return recusa('estado')

  const tokens = await trocarCodigo(codigo, verificador)
  if (!tokens) return recusa('provedor')

  await gravarSessao(tokens)
  await limparTransacao()

  return NextResponse.redirect(new URL(destino, config.aplicacao))
}
