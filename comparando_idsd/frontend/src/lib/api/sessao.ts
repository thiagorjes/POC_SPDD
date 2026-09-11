import { chamar } from '@/lib/api/cliente'

/**
 * `GET /v1/sessao`.
 *
 * Não há cadastro local a preencher: a conta nasce na primeira entrada, a
 * partir da identidade federada (ADR-003, ADR-010). O nome exibido é o que o
 * provedor afirma, e a aplicação não o edita.
 */
export type Sessao = {
  id: string
  nome: string
  email: string | null
  adminGlobal: boolean
}

export const obterSessao = () => chamar<Sessao>('/v1/sessao')
