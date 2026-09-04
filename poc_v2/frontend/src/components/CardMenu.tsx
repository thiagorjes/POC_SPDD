'use client';

import { useState } from 'react';
import type { EtapaResponse, TarefaResumoResponse } from '@/lib/types';

interface Props {
  tarefa: TarefaResumoResponse;
  etapas: EtapaResponse[];
  somenteLeitura: boolean;
  podeExcluir: boolean;
  aoMover: (etapaDestinoId: string) => void;
  aoExcluir: () => void;
}

/**
 * Segunda forma de mover o card (DDR-002): avancar, retroceder e desfinalizar chamam **o mesmo**
 * `PATCH /api/tarefas/{id}/mover` usado pelo drag-and-drop — a validacao e unica.
 *
 * As opcoes vem de `destinosPermitidos`, ja filtrado pelo backend por transicao configurada e por
 * permissao (incluindo `tarefa:finalizar` na etapa final).
 */
export function CardMenu({
  tarefa,
  etapas,
  somenteLeitura,
  podeExcluir,
  aoMover,
  aoExcluir,
}: Props) {
  const [aberto, setAberto] = useState(false);
  const etapaAtual = etapas.find((etapa) => etapa.id === tarefa.etapaId);
  const destinos = tarefa.destinosPermitidos
    .map((id) => etapas.find((etapa) => etapa.id === id))
    .filter((etapa): etapa is EtapaResponse => etapa !== undefined);

  function rotulo(destino: EtapaResponse): string {
    if (etapaAtual?.etapaFinal) {
      return `Desfinalizar para "${destino.nome}"`;
    }
    if (etapaAtual && destino.ordem < etapaAtual.ordem) {
      return `Retroceder para "${destino.nome}"`;
    }
    return `Avançar para "${destino.nome}"`;
  }

  return (
    <div className="card-menu">
      <button
        className="card-menu__toggle"
        type="button"
        aria-label={`Ações do card ${tarefa.titulo}`}
        aria-expanded={aberto}
        onClick={() => setAberto((valor) => !valor)}
      >
        ⋯
      </button>
      {aberto && (
        <div className="card-menu__list" role="menu">
          {destinos.length === 0 && (
            <button type="button" role="menuitem" disabled>
              Sem destinos disponíveis
            </button>
          )}
          {destinos.map((destino) => (
            <button
              key={destino.id}
              type="button"
              role="menuitem"
              disabled={somenteLeitura}
              onClick={() => {
                setAberto(false);
                aoMover(destino.id);
              }}
            >
              {rotulo(destino)}
            </button>
          ))}
          <button
            type="button"
            role="menuitem"
            disabled={somenteLeitura || !podeExcluir}
            onClick={() => {
              setAberto(false);
              aoExcluir();
            }}
          >
            Excluir card
          </button>
        </div>
      )}
    </div>
  );
}
