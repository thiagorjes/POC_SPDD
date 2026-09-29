'use client'

import { useActionState, useEffect, useRef, useState } from 'react'

import { Alerta } from '@/componentes/alerta'
import { Botao } from '@/componentes/botao'
import { substituirFluxoAcao, type EstadoDoFluxo } from '@/app/projetos/[id]/config/fluxo/acoes'
import type { EtapaDesejada } from '@/lib/api/etapas'

/** Uma etapa em edição, com uma chave estável para a lista — nunca a `ordem`. */
type Rascunho = EtapaDesejada & { chave: string }

let contador = 0
function paraRascunho(etapa: EtapaDesejada): Rascunho {
  return { ...etapa, chave: etapa.id ?? `nova-${contador++}` }
}

/**
 * TL-08 — editor do fluxo de etapas (RF-017, TASK-02.7).
 *
 * A ordem é a posição na lista, e nunca um campo digitável: o protótipo de
 * referência já trata a ordem como derivada (o número ao lado da linha é
 * `aria-hidden`), e reordenar por número digitado abriria a mesma disputa de
 * posição que o backend recusa com `409`/passo intermediário — problema que a
 * tela não precisa reproduzir quando mover por botão já evita.
 *
 * **A remoção da lista é o arquivamento** (guia técnico da task): não há botão
 * "arquivar" à parte, e retirar a etapa e salvar é o que a arquiva — RF-017
 * trata a etapa omitida do corpo como arquivada.
 */
export function EditorDeFluxo({
  projetoId,
  etapasIniciais,
}: {
  projetoId: string
  etapasIniciais: EtapaDesejada[]
}) {
  const acao = substituirFluxoAcao.bind(null, projetoId)
  const [estado, disparar, pendente] = useActionState<EstadoDoFluxo, EtapaDesejada[]>(acao, {
    etapas: etapasIniciais,
  })
  const [itens, setItens] = useState<Rascunho[]>(() => etapasIniciais.map(paraRascunho))

  // Só sincroniza o rascunho com o que o servidor confirmou salvo — nunca com
  // uma recusa, cujo `estado.etapas` é eco do que já está na tela e recarregar
  // dali apagaria a mesma coisa que preservar deveria manter intacta.
  const ultimoSalvo = useRef<EstadoDoFluxo | undefined>(undefined)
  useEffect(() => {
    if (estado.salvo && estado !== ultimoSalvo.current) {
      ultimoSalvo.current = estado
      setItens(estado.etapas.map(paraRascunho))
    }
  }, [estado])

  function bloqueioDe(chave: string) {
    return estado.bloqueadas?.find((item) => item.etapaId === chave)
  }

  function renomear(chave: string, nome: string) {
    setItens((atuais) => atuais.map((item) => (item.chave === chave ? { ...item, nome } : item)))
  }

  function alternarTerminal(chave: string) {
    setItens((atuais) =>
      atuais.map((item) => (item.chave === chave ? { ...item, terminal: !item.terminal } : item)),
    )
  }

  function remover(chave: string) {
    setItens((atuais) => atuais.filter((item) => item.chave !== chave))
  }

  function mover(indice: number, sentido: -1 | 1) {
    setItens((atuais) => {
      const alvo = indice + sentido
      if (alvo < 0 || alvo >= atuais.length) return atuais
      const copia = [...atuais]
      const trocado = copia[indice]
      copia[indice] = copia[alvo]
      copia[alvo] = trocado
      return copia
    })
  }

  function adicionar() {
    setItens((atuais) => [
      ...atuais,
      { chave: `nova-${contador++}`, nome: '', ordem: atuais.length + 1, terminal: false },
    ])
  }

  function salvar() {
    const etapas: EtapaDesejada[] = itens.map((item, indice) => ({
      id: item.id,
      nome: item.nome,
      ordem: indice + 1,
      terminal: item.terminal,
    }))
    disparar(etapas)
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between gap-4">
        <h1 className="text-xl font-semibold">Configuração do fluxo</h1>
        <Botao type="button" onClick={salvar} disabled={pendente} aria-busy={pendente}>
          {pendente ? 'Salvando…' : 'Salvar fluxo'}
        </Botao>
      </div>

      {estado.salvo ? (
        <Alerta tom="sucesso" titulo="Fluxo salvo">
          Vale para as transições a partir de agora. O tempo por etapa já acumulado permanece
          interpretável — nada foi reescrito.
        </Alerta>
      ) : null}

      {estado.recusa ? (
        <Alerta tom="recusa" titulo={estado.recusa}>
          {estado.bloqueadas?.length ? (
            <ul className="list-disc pl-5">
              {estado.bloqueadas.map((item) => (
                <li key={item.etapaId}>
                  “{item.nome}” tem {item.tarefasAtivas}{' '}
                  {item.tarefasAtivas === 1 ? 'tarefa ativa' : 'tarefas ativas'}. Mova-as para
                  outra etapa e tente de novo.
                </li>
              ))}
            </ul>
          ) : null}
        </Alerta>
      ) : null}

      <section aria-labelledby="et-titulo" className="flex flex-col gap-4">
        <div className="flex items-center justify-between gap-4">
          <h2 id="et-titulo" className="text-base font-semibold">
            Etapas e ordem
          </h2>
          <Botao type="button" tipo="secundario" onClick={adicionar}>
            Adicionar etapa
          </Botao>
        </div>

        <ul className="flex flex-col gap-3">
          {itens.map((item, indice) => {
            const nomeCampo = `fx-nome-${item.chave}`
            const bloqueio = bloqueioDe(item.id ?? '')
            const nomeExibido = item.nome || `etapa ${indice + 1}`
            return (
              <li key={item.chave} className="flex flex-wrap items-center gap-3 rounded-lg border border-borda p-3">
                <span aria-hidden="true" className="w-6 text-center text-sm text-texto-secundario">
                  {indice + 1}
                </span>

                <div className="flex-1">
                  <label htmlFor={nomeCampo} className="sr-only">
                    Nome da etapa {indice + 1}
                  </label>
                  <input
                    id={nomeCampo}
                    className="w-full rounded-md border border-borda bg-superficie px-3 py-2 text-sm"
                    type="text"
                    value={item.nome}
                    onChange={(evento) => renomear(item.chave, evento.target.value)}
                    aria-invalid={Boolean(bloqueio)}
                  />
                </div>

                <label className="flex items-center gap-2 text-xs">
                  <input
                    type="checkbox"
                    checked={item.terminal}
                    onChange={() => alternarTerminal(item.chave)}
                    aria-label={
                      item.terminal ? `${nomeExibido} é terminal` : `Marcar ${nomeExibido} como terminal`
                    }
                  />
                  Terminal
                </label>

                <div className="flex gap-1">
                  <button
                    type="button"
                    className="rounded-md border border-borda p-2 disabled:opacity-40"
                    aria-label={`Mover a etapa ${nomeExibido} para cima`}
                    disabled={indice === 0}
                    onClick={() => mover(indice, -1)}
                  >
                    ↑
                  </button>
                  <button
                    type="button"
                    className="rounded-md border border-borda p-2 disabled:opacity-40"
                    aria-label={`Mover a etapa ${nomeExibido} para baixo`}
                    disabled={indice === itens.length - 1}
                    onClick={() => mover(indice, 1)}
                  >
                    ↓
                  </button>
                  <button
                    type="button"
                    className="rounded-md border border-destrutivo p-2 text-destrutivo"
                    aria-label={`Remover a etapa ${nomeExibido}`}
                    onClick={() => remover(item.chave)}
                  >
                    ✕
                  </button>
                </div>
              </li>
            )
          })}
        </ul>
      </section>
    </div>
  )
}
