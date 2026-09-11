import { NextResponse, type NextRequest } from 'next/server'

export const CABECALHO_CAMINHO = 'x-caminho'

/**
 * Carimba na requisição o caminho que o navegador pediu.
 *
 * Existe por um motivo só: quando o cliente REST encontra sessão ausente ou
 * expirada, ele precisa recomeçar o authorization code e voltar **para a tela**
 * que a pessoa pediu. O que ele tem em mãos é o caminho da API (`/v1/sessao`),
 * e mandar a pessoa de volta para lá depois de autenticar a deixaria num
 * recurso que não é tela.
 *
 * O App Router não expõe o caminho da requisição a um Server Component, e é
 * isto que o middleware resolve. Não há decisão de acesso aqui — a guarda mora
 * no cliente REST, para que rota nova não possa esquecê-la.
 */
export default function proxy(requisicao: NextRequest) {
  const cabecalhos = new Headers(requisicao.headers)
  cabecalhos.set(CABECALHO_CAMINHO, requisicao.nextUrl.pathname + requisicao.nextUrl.search)
  return NextResponse.next({ request: { headers: cabecalhos } })
}

export const config = {
  matcher: ['/((?!_next/static|_next/image|favicon.ico).*)'],
}
