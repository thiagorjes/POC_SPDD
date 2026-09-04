'use client';

import { useState } from 'react';
import { CardMenu } from './CardMenu';
import { Avatar } from './ui/Avatar';
import { Badge } from './ui/Badge';
import { Botao } from './ui/Botao';
import { EstadoVazio } from './ui/EstadoVazio';
import type { BoardSnapshot, Etapa, Raia, TarefaResumo } from '@/lib/types';

type Densidade = 'compacto' | 'expandido';

const ROTULO_TIPO: Record<string, string> = {
  FEATURE: 'Feature',
  BUG: 'Bug',
  TAREFA: 'Tarefa',
  MELHORIA: 'Melhoria',
};

const ROTULO_PRIORIDADE: Record<string, string> = {
  BAIXA: 'Baixa',
  MEDIA: 'Média',
  ALTA: 'Alta',
  CRITICA: 'Crítica',
};

/**
 * Raia e o container externo e cada uma contem o conjunto completo de colunas (TL-03). A hierarquia
 * inversa que existia antes deixava o card sempre na mesma raia visual, sem diferenciacao.
 *
 * <p>Durante o arraste apenas as colunas em {@code destinosPermitidos} do card ficam realcadas; as
 * demais sao esmaecidas e recusam o drop (DDR-002). A raia e agrupamento visual e nunca restringe
 * o destino.
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

  const raias = [...snapshot.raias].sort((a, b) => a.ordem - b.ordem);

  return (
    <>
      <div className="linha" style={{ marginBottom: 'var(--espaco-scale-md)' }}>
        <Botao
          variante="outline"
          aria-pressed={densidade === 'compacto'}
          onClick={() => setDensidade('compacto')}
        >
          Compacto
        </Botao>
        <Botao
          variante="outline"
          aria-pressed={densidade === 'expandido'}
          onClick={() => setDensidade('expandido')}
        >
          Expandido
        </Botao>
      </div>

      {raias.map((raia) => (
        <section className="swimlane" key={raia.id} aria-label={`Raia ${raia.nome}`}>
          <div className="swimlane__title">Raia: {raia.nome}</div>
          <div className="board">
            {snapshot.etapas.map((etapa) => (
              <Coluna
                key={etapa.id}
                etapa={etapa}
                raia={raia}
                snapshot={snapshot}
                densidade={densidade}
                arrastando={arrastando}
                somenteLeitura={somenteLeitura}
                aoArrastarInicio={setArrastando}
                aoArrastarFim={() => setArrastando(null)}
                aoMover={aoMover}
                aoAbrir={aoAbrir}
                aoNovaTarefa={aoNovaTarefa}
                aoDropInvalido={aoDropInvalido}
              />
            ))}
          </div>
        </section>
      ))}

      {snapshot.tarefas.length === 0 && <EstadoVazio mensagem="Nenhuma tarefa neste board ainda." />}
    </>
  );
}

function Coluna({
  etapa,
  raia,
  snapshot,
  densidade,
  arrastando,
  somenteLeitura,
  aoArrastarInicio,
  aoArrastarFim,
  aoMover,
  aoAbrir,
  aoNovaTarefa,
  aoDropInvalido,
}: {
  etapa: Etapa;
  raia: Raia;
  snapshot: BoardSnapshot;
  densidade: Densidade;
  arrastando: TarefaResumo | null;
  somenteLeitura: boolean;
  aoArrastarInicio: (tarefa: TarefaResumo) => void;
  aoArrastarFim: () => void;
  aoMover: (tarefa: TarefaResumo, etapaDestinoId: string, raiaDestinoId?: string) => void;
  aoAbrir: (tarefaId: string) => void;
  aoNovaTarefa: (etapaId: string) => void;
  aoDropInvalido: () => void;
}) {
  const cards = snapshot.tarefas.filter((t) => t.etapaId === etapa.id && t.raiaId === raia.id);
  const alvoValido = arrastando?.destinosPermitidos.includes(etapa.id) ?? false;
  const classe = arrastando
    ? `column ${alvoValido ? 'column--drop-valid' : 'column--drop-invalid'}`
    : 'column';

  return (
    <div
      className={classe}
      aria-label={`Coluna ${etapa.nome} da raia ${raia.nome}`}
      onDragOver={(evento) => {
        if (alvoValido) {
          evento.preventDefault();
        }
      }}
      onDrop={(evento) => {
        evento.preventDefault();
        const tarefa = arrastando;
        aoArrastarFim();
        if (!tarefa) {
          return;
        }
        if (!tarefa.destinosPermitidos.includes(etapa.id)) {
          aoDropInvalido();
          return;
        }
        // A raia de destino vem da swimlane que recebeu o drop; o contrato ja aceita o campo.
        aoMover(tarefa, etapa.id, raia.id);
      }}
    >
      <div className="column__header">
        <span>{etapa.nome}</span>
        {arrastando ? (
          <span className={`badge-transicao badge-transicao--${alvoValido ? 'ok' : 'bloqueada'}`}>
            {alvoValido ? 'Permitido' : 'Sem transição'}
          </span>
        ) : (
          <Badge variante="neutro">{cards.length}</Badge>
        )}
      </div>

      {cards.length === 0 ? (
        <EstadoVazio compacto mensagem="Sem tarefas nesta etapa" />
      ) : (
        cards.map((tarefa) => (
          <TaskCard
            key={tarefa.id}
            tarefa={tarefa}
            etapas={snapshot.etapas}
            densidade={densidade}
            somenteLeitura={somenteLeitura}
            aoArrastarInicio={aoArrastarInicio}
            aoArrastarFim={aoArrastarFim}
            aoMover={(destino) => aoMover(tarefa, destino, raia.id)}
            aoAbrir={aoAbrir}
          />
        ))
      )}

      {!snapshot.somenteLeitura && (
        <Botao
          variante="text"
          aria-label={`Nova tarefa em ${etapa.nome}`}
          onClick={() => aoNovaTarefa(etapa.id)}
        >
          + Novo card
        </Botao>
      )}
    </div>
  );
}

function TaskCard({
  tarefa,
  etapas,
  densidade,
  somenteLeitura,
  aoArrastarInicio,
  aoArrastarFim,
  aoMover,
  aoAbrir,
}: {
  tarefa: TarefaResumo;
  etapas: Etapa[];
  densidade: Densidade;
  somenteLeitura: boolean;
  aoArrastarInicio: (tarefa: TarefaResumo) => void;
  aoArrastarFim: () => void;
  aoMover: (etapaDestinoId: string) => void;
  aoAbrir: (tarefaId: string) => void;
}) {
  const expandido = densidade === 'expandido';
  const classe = [
    'task-card',
    expandido ? 'task-card--expanded' : '',
    tarefa.impedida ? 'task-card__impedido' : '',
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <article
      className={classe}
      draggable={!somenteLeitura}
      onDragStart={() => aoArrastarInicio(tarefa)}
      onDragEnd={aoArrastarFim}
    >
      <Badge variante="tipo">{ROTULO_TIPO[tarefa.tipo] ?? tarefa.tipo}</Badge>
      <button
        onClick={() => aoAbrir(tarefa.id)}
        style={{ border: 0, background: 'none', padding: 0, textAlign: 'left', width: '100%' }}
      >
        <p>{tarefa.titulo}</p>
      </button>

      {expandido ? (
        <div className="meta-row">
          <span className="linha">
            {tarefa.responsavelNome && <Avatar nome={tarefa.responsavelNome} tamanho={20} />}
            {tarefa.impedida && (
              <Badge variante="warning" title="Tarefa impedida">
                Impedido
              </Badge>
            )}
          </span>
          <Badge variante="neutro">{ROTULO_PRIORIDADE[tarefa.prioridade] ?? tarefa.prioridade}</Badge>
        </div>
      ) : (
        <span className="linha">
          {tarefa.responsavelNome && <Avatar nome={tarefa.responsavelNome} tamanho={20} />}
          {tarefa.impedida && (
            <Badge variante="warning" title="Tarefa impedida">
              Impedido
            </Badge>
          )}
        </span>
      )}

      {!somenteLeitura && (
        <CardMenu tarefa={tarefa} etapas={etapas} desabilitado={somenteLeitura} aoMover={aoMover} />
      )}
    </article>
  );
}
