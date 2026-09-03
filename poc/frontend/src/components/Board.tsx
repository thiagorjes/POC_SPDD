'use client';

import { useState } from 'react';
import { CardMenu } from './CardMenu';
import { Vazio } from './Estados';
import type { BoardSnapshot, TarefaResumo } from '@/lib/types';

type Densidade = 'compacto' | 'expandido';

/**
 * Colunas por etapa, agrupadas por raia. Densidade compacta e o default (A-14 / TL-03).
 *
 * <p>Durante o arraste apenas as colunas em {@code destinosPermitidos} do card ficam realcadas; as
 * demais sao esmaecidas e recusam o drop, devolvendo o card com um toast (DDR-002).
 */
export function Board({
  snapshot,
  podeMover,
  aoMover,
  aoAbrir,
  aoNovaTarefa,
  aoDropInvalido,
}: {
  snapshot: BoardSnapshot;
  podeMover: boolean;
  aoMover: (tarefa: TarefaResumo, etapaDestinoId: string, raiaDestinoId?: string) => void;
  aoAbrir: (tarefaId: string) => void;
  aoNovaTarefa: (etapaId: string) => void;
  aoDropInvalido: () => void;
}) {
  const [densidade, setDensidade] = useState<Densidade>('compacto');
  const [arrastando, setArrastando] = useState<TarefaResumo | null>(null);
  const somenteLeitura = snapshot.somenteLeitura || !podeMover;

  return (
    <>
      <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
        <button
          aria-pressed={densidade === 'compacto'}
          onClick={() => setDensidade('compacto')}
        >
          Compacto
        </button>
        <button
          aria-pressed={densidade === 'expandido'}
          onClick={() => setDensidade('expandido')}
        >
          Expandido
        </button>
      </div>

      <div className="board">
        {snapshot.etapas.map((etapa) => {
          const alvoValido = arrastando?.destinosPermitidos.includes(etapa.id) ?? false;
          const classe = arrastando
            ? `coluna ${alvoValido ? 'alvo-valido' : 'alvo-invalido'}`
            : 'coluna';
          return (
            <section
              key={etapa.id}
              className={classe}
              aria-label={`Etapa ${etapa.nome}`}
              onDragOver={(evento) => {
                if (alvoValido) {
                  evento.preventDefault();
                }
              }}
              onDrop={(evento) => {
                evento.preventDefault();
                const tarefa = arrastando;
                setArrastando(null);
                if (!tarefa) {
                  return;
                }
                if (!tarefa.destinosPermitidos.includes(etapa.id)) {
                  aoDropInvalido();
                  return;
                }
                aoMover(tarefa, etapa.id);
              }}
            >
              <header style={{ display: 'flex', justifyContent: 'space-between' }}>
                <strong>{etapa.nome}</strong>
                <button
                  disabled={snapshot.somenteLeitura}
                  aria-label={`Nova tarefa em ${etapa.nome}`}
                  onClick={() => aoNovaTarefa(etapa.id)}
                >
                  +
                </button>
              </header>

              {snapshot.raias.map((raia) => {
                const cards = snapshot.tarefas.filter(
                  (t) => t.etapaId === etapa.id && t.raiaId === raia.id,
                );
                if (cards.length === 0) {
                  return null;
                }
                return (
                  <div key={raia.id}>
                    <p className="raia-titulo">{raia.nome}</p>
                    {cards.map((tarefa) => (
                      <article
                        key={tarefa.id}
                        className={`card ${densidade} ${tarefa.impedida ? 'impedida' : ''}`}
                        draggable={!somenteLeitura}
                        onDragStart={() => setArrastando(tarefa)}
                        onDragEnd={() => setArrastando(null)}
                      >
                        <button
                          onClick={() => aoAbrir(tarefa.id)}
                          style={{ border: 0, background: 'none', padding: 0, textAlign: 'left' }}
                        >
                          <strong>{tarefa.titulo}</strong>
                        </button>
                        <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap', marginTop: 4 }}>
                          <span className="badge">{tarefa.tipo}</span>
                          {densidade === 'expandido' && (
                            <span className="badge">{tarefa.prioridade}</span>
                          )}
                          {tarefa.impedida && (
                            <span className="badge impedida" title="Tarefa impedida">
                              Impedida
                            </span>
                          )}
                          {tarefa.responsavelNome && (
                            <span className="badge">{tarefa.responsavelNome}</span>
                          )}
                        </div>
                        {!somenteLeitura && (
                          <CardMenu
                            tarefa={tarefa}
                            etapas={snapshot.etapas}
                            desabilitado={somenteLeitura}
                            aoMover={(destino) => aoMover(tarefa, destino)}
                          />
                        )}
                      </article>
                    ))}
                  </div>
                );
              })}
            </section>
          );
        })}
      </div>

      {snapshot.tarefas.length === 0 && <Vazio titulo="Nenhuma tarefa neste board ainda." />}
    </>
  );
}
