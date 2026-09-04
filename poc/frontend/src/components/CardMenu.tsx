'use client';

import type { Etapa, TarefaResumo } from '@/lib/types';

/**
 * Avancar, retroceder e desfinalizar sao o mesmo PATCH /mover (DDR-002) — o menu apenas escolhe
 * o destino dentro de {@code destinosPermitidos}, que ja veio filtrado por grafo e permissao.
 */
export function CardMenu({
  tarefa,
  etapas,
  desabilitado,
  aoMover,
}: {
  tarefa: TarefaResumo;
  etapas: Etapa[];
  desabilitado: boolean;
  aoMover: (etapaDestinoId: string) => void;
}) {
  const destinos = etapas.filter((e) => tarefa.destinosPermitidos.includes(e.id));
  if (destinos.length === 0) {
    return null;
  }
  return (
    <div className="form-field" style={{ marginBottom: 0 }}>
      <select
        aria-label={`Mover a tarefa ${tarefa.titulo}`}
        disabled={desabilitado}
        value=""
        onChange={(evento) => {
          if (evento.target.value) {
            aoMover(evento.target.value);
          }
        }}
      >
        <option value="">Mover para…</option>
        {destinos.map((etapa) => (
          <option key={etapa.id} value={etapa.id}>
            {etapa.nome}
            {etapa.etapaFinal ? ' (finalizar)' : ''}
          </option>
        ))}
      </select>
    </div>
  );
}
