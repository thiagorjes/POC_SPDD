import { redirect } from 'next/navigation'

/**
 * A raiz não é destino: ela leva à lista de projetos, que é a primeira coisa
 * que alguém autenticado tem para fazer. O redirecionamento é do servidor para
 * que quem chega sem sessão atravesse a raiz, a lista e o provedor numa
 * sequência só, sem tela intermediária piscando.
 */
export default function Raiz() {
  redirect('/projetos')
}
