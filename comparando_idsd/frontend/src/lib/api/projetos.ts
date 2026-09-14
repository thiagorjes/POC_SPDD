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
   * Obrigatório desde TASK-02.2, que passou a emiti-lo (RN-038, SCN-002.4). Era
   * opcional enquanto o serviço não o mandava, para que a marca de "fluxo não
   * configurado" não fosse derivada de ausência de campo — o que marcaria como
   * não configurado todo projeto de uma resposta que simplesmente não o
   * carregava. Com o campo chegando sempre, manter o opcional preservaria esse
   * mesmo silêncio: um dia em que o serviço parasse de emiti-lo, nenhum projeto
   * apareceria pendente e nada acusaria a perda.
   */
  fluxoConfigurado: boolean
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
