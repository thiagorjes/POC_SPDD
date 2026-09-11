import { config } from '@/lib/config'

/**
 * Destino interno pós-autenticação.
 *
 * Existe um ponto só porque a validação estava em três — o início do
 * authorization code, a volta dele e o cliente REST —, e três cópias de uma
 * regra de segurança é o mesmo que nenhuma: a revisão de TASK-01.7 (ACH-01)
 * mediu que duas delas divergiam e que a terceira não recusava o que dizia
 * recusar.
 *
 * A recusa é **por resolução e não por lista negra**. Filtrar prefixo não
 * funciona porque a normalização WHATWG converte barra invertida e tabulação em
 * separador de autoridade dentro de esquema especial: `/\evil.com` começa com
 * uma barra só, passa por qualquer teste de prefixo, e `new URL` o resolve para
 * `http://evil.com/`. Resolver contra a própria origem e comparar origem é o
 * único teste que não depende de enumerar as formas de escrever a mesma coisa —
 * o que a normalização fizer, ela já fez antes da comparação.
 */
const PADRAO = '/projetos'

export function destinoInterno(pedido: string | null | undefined): string {
  if (!pedido) return PADRAO
  try {
    const origem = new URL(config.aplicacao)
    const alvo = new URL(pedido, origem)
    if (alvo.origin !== origem.origin) return PADRAO
    return `${alvo.pathname}${alvo.search}${alvo.hash}`
  } catch {
    // Entrada que nem sequer resolve não tem para onde apontar.
    return PADRAO
  }
}
