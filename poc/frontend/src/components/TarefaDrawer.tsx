'use client';

import { useEffect, useState } from 'react';
import { Skeleton } from './Estados';
import { useFeedback } from './Feedback';
import { Badge } from './ui/Badge';
import { Botao } from './ui/Botao';
import { api } from '@/lib/api';
import type { TarefaDetalhe } from '@/lib/types';

const TIPOS: { valor: string; rotulo: string }[] = [
  { valor: 'FEATURE', rotulo: 'Feature' },
  { valor: 'BUG', rotulo: 'Bug' },
  { valor: 'TAREFA', rotulo: 'Tarefa' },
  { valor: 'MELHORIA', rotulo: 'Melhoria' },
];

const PRIORIDADES: { valor: string; rotulo: string }[] = [
  { valor: 'BAIXA', rotulo: 'Baixa' },
  { valor: 'MEDIA', rotulo: 'Média' },
  { valor: 'ALTA', rotulo: 'Alta' },
  { valor: 'CRITICA', rotulo: 'Crítica' },
];

function rotuloDe(lista: { valor: string; rotulo: string }[], valor: string): string {
  return lista.find((i) => i.valor === valor)?.rotulo ?? valor;
}

function duracao(segundos: number): string {
  const h = Math.floor(segundos / 3600);
  const m = Math.floor((segundos % 3600) / 60);
  return h > 0 ? `${h}h ${m}min` : `${m}min`;
}

/**
 * TL-04. Descricao e tipo ficam travados quando a tarefa ja foi iniciada (RF-003); o titulo
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
  const [salvo, setSalvo] = useState(false);
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
      <div className="drawer-backdrop">
        <aside className="drawer" role="dialog" aria-modal="true" aria-label="Detalhe da tarefa">
          <Skeleton linhas={5} />
        </aside>
      </div>
    );
  }

  const travado = somenteLeitura || salvando;
  const camposEstruturaisTravados = travado || tarefa.iniciada;

  const salvar = async () => {
    setSalvando(true);
    setSalvo(false);
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
      setSalvo(true);
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
    <div className="drawer-backdrop">
      <aside className="drawer" role="dialog" aria-modal="true" aria-labelledby="drawer-titulo">
        <div className="page-header">
          <h1 id="drawer-titulo" style={{ fontSize: 'var(--fonte-scale-lg)' }}>
            {tarefa.titulo}
          </h1>
          <div>
            <Botao variante="text" onClick={aoFechar} aria-label="Fechar detalhe">
              ✕ Fechar
            </Botao>
          </div>
        </div>

        <div className="linha" style={{ marginBottom: 'var(--espaco-scale-md)' }}>
          <Badge variante="tipo">{rotuloDe(TIPOS, tarefa.tipo)}</Badge>
          {tarefa.impedida && <Badge variante="warning">Impedido</Badge>}
        </div>

        {salvo && (
          <div
            className="toast toast-success"
            role="status"
            aria-live="polite"
            style={{ marginBottom: 'var(--espaco-scale-md)' }}
          >
            Alterações salvas com sucesso.
          </div>
        )}

        <div className="form-field">
          <label htmlFor="td-titulo">Título</label>
          <input
            id="td-titulo"
            value={tarefa.titulo}
            disabled={travado}
            onChange={(e) => setTarefa({ ...tarefa, titulo: e.target.value })}
          />
        </div>

        {/*
          Apos o inicio a descricao e o tipo viram leitura (RF-003). O prototipo representa isso
          com um campo travado, nao com um input desabilitado.
        */}
        {camposEstruturaisTravados && tarefa.iniciada ? (
          <>
            <div className="form-field">
              <span>Descrição</span>
              <div className="field-locked" aria-readonly="true">
                {tarefa.descricao || '—'}
              </div>
            </div>
            <div className="form-field">
              <span>Tipo</span>
              <div className="field-locked" aria-readonly="true">
                {rotuloDe(TIPOS, tarefa.tipo)}
              </div>
            </div>
            <p className="text-secondary">
              Tarefa iniciada: descrição e tipo só podem ser alterados por papel administrativo ou com
              o toggle do projeto habilitado.
            </p>
          </>
        ) : (
          <>
            <div className="form-field">
              <label htmlFor="td-descricao">Descrição</label>
              <textarea
                id="td-descricao"
                rows={4}
                value={tarefa.descricao ?? ''}
                disabled={camposEstruturaisTravados}
                onChange={(e) => setTarefa({ ...tarefa, descricao: e.target.value })}
              />
            </div>
            <div className="form-field">
              <label htmlFor="td-tipo">Tipo</label>
              <select
                id="td-tipo"
                value={tarefa.tipo}
                disabled={camposEstruturaisTravados}
                onChange={(e) => setTarefa({ ...tarefa, tipo: e.target.value as TarefaDetalhe['tipo'] })}
              >
                {TIPOS.map((t) => (
                  <option key={t.valor} value={t.valor}>
                    {t.rotulo}
                  </option>
                ))}
              </select>
            </div>
          </>
        )}

        <div className="form-field">
          <label htmlFor="td-prioridade">Prioridade</label>
          <select
            id="td-prioridade"
            value={tarefa.prioridade}
            disabled={travado}
            onChange={(e) =>
              setTarefa({ ...tarefa, prioridade: e.target.value as TarefaDetalhe['prioridade'] })
            }
          >
            {PRIORIDADES.map((p) => (
              <option key={p.valor} value={p.valor}>
                {p.rotulo}
              </option>
            ))}
          </select>
        </div>

        <section className="secao">
          <h2>Impedimento</h2>
          {tarefa.impedida ? (
            <p className="text-secondary">Motivo atual: {tarefa.motivoImpedimento ?? '—'}</p>
          ) : (
            <div className="form-field">
              <label htmlFor="td-motivo">Motivo</label>
              <input
                id="td-motivo"
                value={motivo}
                maxLength={500}
                disabled={travado}
                aria-describedby="td-motivo-desc"
                onChange={(e) => setMotivo(e.target.value)}
              />
              <span className="text-secondary" id="td-motivo-desc">
                O motivo fica registrado no histórico e alimenta o tempo de impedimento do dashboard.
              </span>
            </div>
          )}
          <Botao variante="outline" disabled={travado} onClick={alternarImpedimento}>
            {tarefa.impedida ? 'Remover impedimento' : 'Marcar impedimento'}
          </Botao>
        </section>

        <section className="secao">
          <h2>Lead-time por etapa</h2>
          <table>
            <thead>
              <tr>
                <th>Etapa</th>
                <th>Permanência</th>
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
          <p className="text-secondary">
            Total impedida: {duracao(tarefa.impedimentoTotalSegundos)}
          </p>
        </section>

        <section className="secao">
          <h2>Histórico</h2>
          {tarefa.historico.map((registro) => (
            <div className="history-item" key={registro.id}>
              <strong>{registro.autorNome ?? 'sistema'}</strong> alterou {registro.campo}:{' '}
              {registro.valorAnterior ?? '—'} → {registro.valorNovo ?? '—'}
              <div className="text-secondary">
                <time dateTime={registro.ocorridoEm}>
                  {new Date(registro.ocorridoEm).toLocaleString('pt-BR')}
                </time>
              </div>
            </div>
          ))}
          <Botao
            variante="text"
            onClick={async () => {
              const proxima = pagina + 1;
              const mais = await api.historico(tarefa.id, proxima);
              if (mais.length === 0) {
                informar('Não há mais registros no histórico.');
                return;
              }
              setPagina(proxima);
              setTarefa({ ...tarefa, historico: [...tarefa.historico, ...mais] });
            }}
          >
            Carregar mais
          </Botao>
        </section>

        <footer className="linha">
          <Botao variante="primary" disabled={travado} onClick={salvar}>
            Salvar
          </Botao>
          <Botao
            variante="outline"
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
          </Botao>
          <Botao variante="danger" disabled={travado} onClick={() => aoExcluir(tarefa)}>
            Excluir
          </Botao>
        </footer>
      </aside>
    </div>
  );
}
