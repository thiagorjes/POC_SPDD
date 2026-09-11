import { chamar } from '@/lib/api/cliente'

/**
 * `GET /v1/projetos` e `POST /v1/projetos`.
 *
 * `permissoes` serve **apenas** para não apresentar ação que a pessoa não pode
 * executar (RN-015, RNF-004). Esconder o botão não autoriza nada: a recusa real
 * é do serviço, e é ela que os cenários congelados verificam.
 */
type Permissao = string

export type ProjetoResumo = {
  id: string
  nome: string
  papeis: string[]
  permissoes: Permissao[]
  acessoPorAdministracaoGlobal: boolean
  /**
   * Opcional de propósito. O contrato o promete e o serviço ainda não o emite —
   * ele nasce com a tabela `etapa`, em TASK-02.2. A marca de "fluxo não
   * configurado" só é renderizada quando o campo **chega** como falso: derivar
   * a ausência de etapas no cliente marcaria como não configurado todo projeto
   * de uma resposta que simplesmente não carrega o campo.
   */
  fluxoConfigurado?: boolean
}

export type PaginaDeProjetos = {
  conteudo: ProjetoResumo[]
  totalElements: number
  totalPages: number
}

export type ProjetoCriado = {
  id: string
  nome: string
  descricao: string | null
  etapas: unknown[]
}

export const listarProjetos = () => chamar<PaginaDeProjetos>('/v1/projetos')

export const criarProjeto = (corpo: {
  nome: string
  descricao?: string
  primeiroAdministradorId: string
}) => chamar<ProjetoCriado>('/v1/projetos', { metodo: 'POST', corpo })
