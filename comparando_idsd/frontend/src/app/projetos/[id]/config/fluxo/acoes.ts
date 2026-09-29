'use server'

import { revalidatePath } from 'next/cache'

import { FalhaDaApi } from '@/lib/api/cliente'
import { substituirFluxo, type EtapaDesejada } from '@/lib/api/etapas'

/** Item de `errors` da recusa "etapa ainda tem tarefas" (RF-017). */
export type EtapaBloqueada = {
  etapaId: string
  nome: string
  tarefasAtivas: number
}

export type EstadoDoFluxo = {
  etapas: EtapaDesejada[]
  recusa?: string
  bloqueadas?: EtapaBloqueada[]
  salvo?: boolean
}

function eEtapaBloqueada(item: unknown): item is EtapaBloqueada {
  const registro = item as Record<string, unknown>
  return typeof registro?.etapaId === 'string' && typeof registro?.tarefasAtivas === 'number'
}

/**
 * Ação de servidor da configuração do fluxo.
 *
 * Recusa preserva o rascunho: em qualquer `422`, o corpo devolvido é
 * exatamente o que a pessoa enviou, nunca uma releitura do servidor — recarregar
 * depois da recusa é a falha que o guia técnico da task nomeia como a mais
 * provável desta tela. `errors` chega em formatos diferentes conforme a regra
 * violada (`EtapaService`): "sem etapa terminal" não nomeia campo nenhum,
 * "etapa com tarefa ativa" nomeia `etapaId`/`nome`/`tarefasAtivas` — daí a
 * checagem de forma em vez de comparar por um `slug` que o contrato não expõe
 * ao cliente.
 */
export async function substituirFluxoAcao(
  projetoId: string,
  _anterior: EstadoDoFluxo,
  etapas: EtapaDesejada[],
): Promise<EstadoDoFluxo> {
  try {
    const fluxo = await substituirFluxo(projetoId, etapas)
    revalidatePath(`/projetos/${projetoId}/config/fluxo`)
    return { etapas: fluxo.etapas, salvo: true }
  } catch (falha) {
    if (!(falha instanceof FalhaDaApi)) throw falha

    const brutos = (falha.problema.errors ?? []) as unknown[]
    const bloqueadas = brutos.filter(eEtapaBloqueada)

    return {
      etapas,
      recusa:
        falha.problema.detail ?? falha.problema.title ?? 'Não foi possível salvar o fluxo.',
      bloqueadas: bloqueadas.length > 0 ? bloqueadas : undefined,
    }
  }
}
