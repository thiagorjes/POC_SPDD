import { chamar } from '@/lib/api/cliente'

/**
 * `GET` e `PUT /v1/projetos/{projetoId}/etapas` (RF-017).
 *
 * A forma é a mesma dos dois lados: o fluxo é sempre o conjunto inteiro, nunca
 * um delta. `id` ausente cria a etapa; presente, preserva a etapa vigente —
 * é o que separa renomear de trocar por outra, porque a série de tempo por
 * etapa segue o identificador (RN-021, RN-022).
 */
export type EtapaDesejada = {
  id?: string
  nome: string
  ordem: number
  terminal: boolean
}

export type FluxoAtual = {
  etapas: (EtapaDesejada & { id: string })[]
}

export const obterFluxo = (projetoId: string) =>
  chamar<FluxoAtual>(`/v1/projetos/${projetoId}/etapas`)

export const substituirFluxo = (projetoId: string, etapas: EtapaDesejada[]) =>
  chamar<FluxoAtual>(`/v1/projetos/${projetoId}/etapas`, {
    metodo: 'PUT',
    corpo: { etapas },
  })
