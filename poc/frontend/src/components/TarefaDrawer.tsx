'use client';

import { useEffect, useState } from 'react';
import { Skeleton } from './Estados';
import { useFeedback } from './Feedback';
import { api } from '@/lib/api';
import type { TarefaDetalhe } from '@/lib/types';

const TIPOS = ['FEATURE', 'BUG', 'TAREFA', 'MELHORIA'];
const PRIORIDADES = ['BAIXA', 'MEDIA', 'ALTA', 'CRITICA'];

function duracao(segundos: number): string {
  const h = Math.floor(segundos / 3600);
  const m = Math.floor((segundos % 3600) / 60);
  return h > 0 ? `${h}h ${m}min` : `${m}min`;
}

/**
 * TL-04. Descricao e tipo ficam desabilitados quando a tarefa ja foi iniciada (RF-003); o titulo
 * permanece editavel. A trava aqui e espelho da regra do backend, que continua validando.
 */
export function TarefaDrawer({
  tarefaId,
  somenteLeitura,
  aoFechar,
  aoAlterar,
  aoExcluir,
}: {
  tarefaId: string;
  somenteLeitura: boolean;
  aoFechar: () => void;
  aoAlterar: () => void;
  aoExcluir: (tarefa: TarefaDetalhe) => void;
}) {
  const { informar, reportar } = useFeedback();
  const [tarefa, setTarefa] = useState<TarefaDetalhe | null>(null);
  const [salvando, setSalvando] = useState(false);
  const [motivo, setMotivo] = useState('');
  const [pagina, setPagina] = useState(0);

  const carregar = async () => {
    try {
      setTarefa(await api.tarefa(tarefaId));
    } catch (e) {
      reportar(e);
    }
  };

  useEffect(() => {
    void carregar();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tarefaId]);

  if (!tarefa) {
    return (
      <aside className="drawer" aria-label="Detalhe da tarefa">
        <Skeleton linhas={5} />
      </aside>
    );
  }

  const travado = somenteLeitura || salvando;
  const camposEstruturaisTravados = travado || tarefa.iniciada;

  const salvar = async () => {
    setSalvando(true);
    try {
      await api.atualizarTarefa(tarefa.id, {
        titulo: tarefa.titulo,
        descricao: tarefa.descricao,
        tipo: tarefa.tipo,
        prioridade: tarefa.prioridade,
        raiaId: tarefa.raiaId,
        versaoEsperada: tarefa.versao,
      });
      informar('Tarefa atualizada.');
      await carregar();
      aoAlterar();
    } catch (e) {
      reportar(e);
    } finally {
      setSalvando(false);
    }
  };

  const alternarImpedimento = async () => {
    try {
      if (tarefa.impedida) {
        await api.desmarcarImpedimento(tarefa.id);
      } else {
        await api.marcarImpedimento(tarefa.id, motivo);
      }
      await carregar();
      aoAlterar();
    } catch (e) {
      reportar(e);
    }
  };

  return (
    <aside className="drawer" aria-label={`Tarefa ${tarefa.titulo}`}>
      <header style={{ display: 'flex', justifyContent: 'space-between' }}>
        <h2>Detalhe da tarefa</h2>
        <button onClick={aoFechar} aria-label="Fechar detalhe">
          Fechar
        </button>
      </header>

      <label>
        Titulo
        <input
          value={tarefa.titulo}
          disabled={travado}
          onChange={(e) => setTarefa({ ...tarefa, titulo: e.target.value })}
        />
      </label>

      <label>
        Descricao
        <textarea
          rows={4}
          value={tarefa.descricao ?? ''}
          disabled={camposEstruturaisTravados}
          onChange={(e) => setTarefa({ ...tarefa, descricao: e.target.value })}
        />
      </label>

      <label>
        Tipo
        <select
          value={tarefa.tipo}
          disabled={camposEstruturaisTravados}
          onChange={(e) => setTarefa({ ...tarefa, tipo: e.target.value as TarefaDetalhe['tipo'] })}
        >
          {TIPOS.map((t) => (
            <option key={t}>{t}</option>
          ))}
        </select>
      </label>

      <label>
        Prioridade
        <select
          value={tarefa.prioridade}
          disabled={travado}
          onChange={(e) =>
            setTarefa({ ...tarefa, prioridade: e.target.value as TarefaDetalhe['prioridade'] })
          }
        >
          {PRIORIDADES.map((p) => (
            <option key={p}>{p}</option>
          ))}
        </select>
      </label>

      {tarefa.iniciada && (
        <p className="badge">
          Tarefa iniciada: descricao e tipo so podem ser alterados por papel administrativo ou com o
          toggle do projeto habilitado.
        </p>
      )}

      <section>
        <h3>Impedimento</h3>
        {!tarefa.impedida && (
          <label>
            Motivo
            <input
              value={motivo}
              maxLength={500}
              disabled={travado}
              onChange={(e) => setMotivo(e.target.value)}
            />
          </label>
        )}
        {tarefa.impedida && <p>Motivo atual: {tarefa.motivoImpedimento ?? '—'}</p>}
        <button disabled={travado} onClick={alternarImpedimento}>
          {tarefa.impedida ? 'Remover impedimento' : 'Marcar impedimento'}
        </button>
      </section>

      <section>
        <h3>Lead-time por etapa</h3>
        <table className="tabela">
          <thead>
            <tr>
              <th>Etapa</th>
              <th>Permanencia</th>
              <th>Impedida</th>
            </tr>
          </thead>
          <tbody>
            {tarefa.leadTimePorEtapa.map((linha) => (
              <tr key={linha.etapaId}>
                <td>{linha.etapaNome}</td>
                <td>{duracao(linha.permanenciaSegundos)}</td>
                <td>{duracao(linha.impedimentoSegundos)}</td>
              </tr>
            ))}
          </tbody>
        </table>
        <p>Total impedida: {duracao(tarefa.impedimentoTotalSegundos)}</p>
      </section>

      <section>
        <h3>Historico</h3>
        <ul>
          {tarefa.historico.map((registro) => (
            <li key={registro.id}>
              <time dateTime={registro.ocorridoEm}>
                {new Date(registro.ocorridoEm).toLocaleString('pt-BR')}
              </time>{' '}
              — {registro.campo}: {registro.valorAnterior ?? '—'} → {registro.valorNovo ?? '—'} (
              {registro.autorNome ?? 'sistema'})
            </li>
          ))}
        </ul>
        <button
          onClick={async () => {
            const proxima = pagina + 1;
            const mais = await api.historico(tarefa.id, proxima);
            if (mais.length === 0) {
              informar('Nao ha mais registros no historico.');
              return;
            }
            setPagina(proxima);
            setTarefa({ ...tarefa, historico: [...tarefa.historico, ...mais] });
          }}
        >
          Carregar mais
        </button>
      </section>

      <footer style={{ display: 'flex', gap: 8 }}>
        <button className="primario" disabled={travado} onClick={salvar}>
          Salvar
        </button>
        <button
          disabled={travado}
          onClick={async () => {
            try {
              if (tarefa.observando) {
                await api.desobservar(tarefa.id);
              } else {
                await api.observar(tarefa.id);
              }
              await carregar();
            } catch (e) {
              reportar(e);
            }
          }}
        >
          {tarefa.observando ? 'Deixar de observar' : 'Observar'}
        </button>
        <button className="perigo" disabled={travado} onClick={() => aoExcluir(tarefa)}>
          Excluir
        </button>
      </footer>
    </aside>
  );
}
