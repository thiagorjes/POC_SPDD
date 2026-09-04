'use client';

import { CardMenu } from './CardMenu';
import { iniciais, rotuloPrioridade, rotuloTipo } from '@/lib/format';
import type { EtapaResponse, TarefaResumoResponse } from '@/lib/types';

export type Densidade = 'compacto' | 'expandido';

interface Props {
  tarefa: TarefaResumoResponse;
  etapas: EtapaResponse[];
  densidade: Densidade;
  somenteLeitura: boolean;
  podeExcluir: boolean;
  nomeResponsavel: string | null;
  aoAbrir: () => void;
  aoMover: (etapaDestinoId: string) => void;
  aoExcluir: () => void;
  aoIniciarArraste: () => void;
  aoTerminarArraste: () => void;
}

/** Card do board — TL-03 (compacto, densidade default por A-14) e TL-03b (expandido). */
export function TaskCard({
  tarefa,
  etapas,
  densidade,
  somenteLeitura,
  podeExcluir,
  nomeResponsavel,
  aoAbrir,
  aoMover,
  aoExcluir,
  aoIniciarArraste,
  aoTerminarArraste,
}: Props) {
  const classes = [
    'task-card',
    densidade === 'expandido' ? 'task-card--expanded' : '',
    tarefa.impedida ? 'task-card__impedido' : '',
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <div
      className={classes}
      draggable={!somenteLeitura}
      onDragStart={(evento) => {
        evento.dataTransfer.setData('text/plain', tarefa.id);
        aoIniciarArraste();
      }}
      onDragEnd={aoTerminarArraste}
    >
      <CardMenu
        tarefa={tarefa}
        etapas={etapas}
        somenteLeitura={somenteLeitura}
        podeExcluir={podeExcluir}
        aoMover={aoMover}
        aoExcluir={aoExcluir}
      />
      <span className="badge badge-tipo">{rotuloTipo(tarefa.tipo)}</span>
      <button className="task-card__titulo" type="button" onClick={aoAbrir}>
        {tarefa.titulo}
      </button>
      {densidade === 'expandido' && (
        <p className="desc">
          Prioridade {rotuloPrioridade(tarefa.prioridade)} ·{' '}
          {tarefa.iniciada ? 'iniciada' : 'não iniciada'}
        </p>
      )}
      <div className="meta-row">
        <span className="avatar avatar--sm" aria-label={nomeResponsavel ?? 'Sem responsável'}>
          {iniciais(nomeResponsavel)}
        </span>
        {tarefa.impedida && <span className="badge badge-warning">Impedido</span>}
      </div>
    </div>
  );
}
