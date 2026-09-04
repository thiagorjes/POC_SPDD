'use client';

import { useState } from 'react';
import { TaskCard, type Densidade } from './TaskCard';
import type { BoardSnapshotResponse, TarefaResumoResponse } from '@/lib/types';

interface Props {
  snapshot: BoardSnapshotResponse;
  densidade: Densidade;
  podeCriar: boolean;
  podeExcluir: boolean;
  nomePorUsuario: Record<string, string>;
  aoAbrirTarefa: (tarefaId: string) => void;
  aoMover: (tarefa: TarefaResumoResponse, etapaDestinoId: string, raiaDestinoId: string) => void;
  aoExcluirTarefa: (tarefa: TarefaResumoResponse) => void;
  aoNovoCard: (raiaId: string) => void;
  aoDropInvalido: () => void;
}

/**
 * Board: uma coluna por etapa do workflow ativo, agrupadas por raia (RF-011).
 *
 * Durante o arraste apenas as colunas presentes em `destinosPermitidos` do card sao destacadas; as
 * demais sao esmaecidas e recusam o drop, devolvendo o card e emitindo toast (RF-002/RN-003). Raia
 * e agrupamento visual: nunca restringe destino.
 */
export function Board({
  snapshot,
  densidade,
  podeCriar,
  podeExcluir,
  nomePorUsuario,
  aoAbrirTarefa,
  aoMover,
  aoExcluirTarefa,
  aoNovoCard,
  aoDropInvalido,
}: Props) {
  const [arrastando, setArrastando] = useState<TarefaResumoResponse | null>(null);

  const etapas = [...snapshot.etapas].sort((a, b) => a.ordem - b.ordem);
  const raias = [...snapshot.raias].sort((a, b) => a.ordem - b.ordem);

  function classeColuna(etapaId: string): string {
    if (!arrastando) {
      return 'column';
    }
    if (arrastando.etapaId === etapaId) {
      return 'column';
    }
    return arrastando.destinosPermitidos.includes(etapaId)
      ? 'column column--drop-valid'
      : 'column column--drop-invalid';
  }

  function soltar(etapaId: string, raiaId: string) {
    const tarefa = arrastando;
    setArrastando(null);
    if (!tarefa) {
      return;
    }
    if (tarefa.etapaId === etapaId && tarefa.raiaId === raiaId) {
      return;
    }
    if (tarefa.etapaId !== etapaId && !tarefa.destinosPermitidos.includes(etapaId)) {
      aoDropInvalido();
      return;
    }
    aoMover(tarefa, etapaId, raiaId);
  }

  return (
    <>
      {raias.map((raia) => {
        const tarefasDaRaia = snapshot.tarefas.filter((tarefa) => tarefa.raiaId === raia.id);
        return (
          <section key={raia.id} className="swimlane" aria-label={`Raia ${raia.nome}`}>
            <div className="swimlane__title">Raia: {raia.nome}</div>
            <div className="board">
              {etapas.map((etapa) => {
                const tarefas = tarefasDaRaia.filter((tarefa) => tarefa.etapaId === etapa.id);
                const destinoValido =
                  arrastando !== null &&
                  arrastando.etapaId !== etapa.id &&
                  arrastando.destinosPermitidos.includes(etapa.id);
                return (
                  <div
                    key={etapa.id}
                    className={classeColuna(etapa.id)}
                    aria-label={`Coluna ${etapa.nome}`}
                    onDragOver={(evento) => {
                      if (destinoValido) {
                        evento.preventDefault();
                      }
                    }}
                    onDrop={(evento) => {
                      evento.preventDefault();
                      soltar(etapa.id, raia.id);
                    }}
                  >
                    <div className="column__header">
                      <span>{etapa.nome}</span>
                      {arrastando && arrastando.etapaId !== etapa.id ? (
                        <span
                          className={
                            destinoValido
                              ? 'badge-transicao badge-transicao--ok'
                              : 'badge-transicao badge-transicao--bloqueada'
                          }
                        >
                          {destinoValido ? 'Permitido' : 'Sem transição'}
                        </span>
                      ) : (
                        <span className="badge badge-neutral">{tarefas.length}</span>
                      )}
                    </div>

                    {tarefas.length === 0 && !arrastando && (
                      <div className="empty-state empty-state--inline">Sem tarefas nesta etapa</div>
                    )}

                    {tarefas.map((tarefa) => (
                      <TaskCard
                        key={tarefa.id}
                        tarefa={tarefa}
                        etapas={etapas}
                        densidade={densidade}
                        somenteLeitura={snapshot.somenteLeitura}
                        podeExcluir={podeExcluir}
                        nomeResponsavel={
                          tarefa.responsavelId ? (nomePorUsuario[tarefa.responsavelId] ?? null) : null
                        }
                        aoAbrir={() => aoAbrirTarefa(tarefa.id)}
                        aoMover={(destino) => aoMover(tarefa, destino, tarefa.raiaId)}
                        aoExcluir={() => aoExcluirTarefa(tarefa)}
                        aoIniciarArraste={() => setArrastando(tarefa)}
                        aoTerminarArraste={() => setArrastando(null)}
                      />
                    ))}

                    {podeCriar && !snapshot.somenteLeitura && etapa.ordem === etapas[0]?.ordem && (
                      <button className="btn btn-text" type="button" onClick={() => aoNovoCard(raia.id)}>
                        + Novo card
                      </button>
                    )}
                  </div>
                );
              })}
            </div>
          </section>
        );
      })}
    </>
  );
}
