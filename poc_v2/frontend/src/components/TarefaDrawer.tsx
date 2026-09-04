'use client';

import { useCallback, useEffect, useState } from 'react';
import { useFeedback } from './FeedbackProvider';
import { Skeleton } from './Skeleton';
import { api } from '@/lib/api';
import { dataHora, duracao, rotuloPrioridade, rotuloTipo } from '@/lib/format';
import type {
  AuditoriaResponse,
  EtapaResponse,
  MembroProjetoResponse,
  PrioridadeTarefa,
  RaiaResponse,
  TarefaDetalheResponse,
  TipoTarefa,
} from '@/lib/types';

const TIPOS: TipoTarefa[] = ['FEATURE', 'BUG', 'TAREFA', 'MELHORIA'];
const PRIORIDADES: PrioridadeTarefa[] = ['BAIXA', 'MEDIA', 'ALTA', 'CRITICA'];

interface Props {
  tarefaId: string;
  etapas: EtapaResponse[];
  raias: RaiaResponse[];
  membros: MembroProjetoResponse[];
  somenteLeitura: boolean;
  podeEditar: boolean;
  podeImpedir: boolean;
  podeAtribuir: boolean;
  aoFechar: () => void;
  aoAlterar: () => void;
}

/**
 * TL-04 — Drawer de detalhe da tarefa.
 *
 * Descricao e tipo ficam travados apos o inicio da tarefa (RF-003/A-03); o titulo permanece
 * editavel por ser auditado (RN-016). Lead-time por etapa vem de periodos persistidos (RF-006) e o
 * historico de auditoria e paginado (RF-017).
 */
export function TarefaDrawer({
  tarefaId,
  etapas,
  raias,
  membros,
  somenteLeitura,
  podeEditar,
  podeImpedir,
  podeAtribuir,
  aoFechar,
  aoAlterar,
}: Props) {
  const [tarefa, setTarefa] = useState<TarefaDetalheResponse | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const [titulo, setTitulo] = useState('');
  const [descricao, setDescricao] = useState('');
  const [tipo, setTipo] = useState<TipoTarefa>('FEATURE');
  const [prioridade, setPrioridade] = useState<PrioridadeTarefa>('MEDIA');
  const [raiaId, setRaiaId] = useState('');
  const [motivo, setMotivo] = useState('');
  const [historico, setHistorico] = useState<AuditoriaResponse[]>([]);
  const [paginaHistorico, setPaginaHistorico] = useState(0);
  const [temMaisHistorico, setTemMaisHistorico] = useState(false);
  const feedback = useFeedback();

  const carregar = useCallback(async () => {
    setCarregando(true);
    try {
      const detalhe = await api.tarefa(tarefaId);
      setTarefa(detalhe);
      setTitulo(detalhe.titulo);
      setDescricao(detalhe.descricao ?? '');
      setTipo(detalhe.tipo);
      setPrioridade(detalhe.prioridade);
      setRaiaId(detalhe.raiaId);
      setHistorico(detalhe.historico);
      setPaginaHistorico(0);
      setTemMaisHistorico(detalhe.historico.length > 0);
    } catch (erro) {
      feedback.reportar(erro);
      aoFechar();
    } finally {
      setCarregando(false);
    }
    // feedback e aoFechar sao estaveis por construcao; recarregar depende apenas da tarefa.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tarefaId]);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  async function carregarMaisHistorico() {
    const proxima = paginaHistorico + 1;
    const pagina = await api.historico(tarefaId, proxima);
    setHistorico((atual) => [...atual, ...pagina.content]);
    setPaginaHistorico(proxima);
    setTemMaisHistorico(proxima + 1 < pagina.totalPages);
  }

  async function salvar() {
    setSalvando(true);
    try {
      await api.atualizarTarefa(tarefaId, {
        titulo: titulo.trim(),
        descricao: descricao.trim() === '' ? null : descricao.trim(),
        tipo,
        prioridade,
        raiaId: raiaId === '' ? null : raiaId,
      });
      feedback.sucesso('Alterações salvas com sucesso.');
      await carregar();
      aoAlterar();
    } catch (erro) {
      feedback.reportar(erro);
    } finally {
      setSalvando(false);
    }
  }

  async function alternarImpedimento(impedir: boolean) {
    try {
      if (impedir) {
        await api.marcarImpedimento(tarefaId, {
          motivo: motivo.trim() === '' ? null : motivo.trim(),
        });
      } else {
        await api.desmarcarImpedimento(tarefaId);
      }
      await carregar();
      aoAlterar();
    } catch (erro) {
      feedback.reportar(erro);
    }
  }

  async function alterarResponsavel(valor: string) {
    try {
      await api.atribuirResponsavel(tarefaId, { responsavelId: valor === '' ? null : valor });
      await carregar();
      aoAlterar();
    } catch (erro) {
      feedback.reportar(erro);
    }
  }

  const travado = tarefa?.iniciada === true;
  const bloqueado = somenteLeitura || !podeEditar;

  return (
    <div className="drawer-backdrop">
      <aside className="drawer" role="dialog" aria-modal="true" aria-labelledby="drawer-title">
        <div className="page-header">
          <h1 id="drawer-title">{tarefa?.titulo ?? 'Carregando…'}</h1>
          <button className="btn btn-text" type="button" aria-label="Fechar" onClick={aoFechar}>
            ✕ Fechar
          </button>
        </div>

        {carregando && <Skeleton linhas={4} />}

        {!carregando && tarefa && (
          <>
            <span className="badge badge-tipo">{rotuloTipo(tarefa.tipo)}</span>

            <section aria-label="Detalhes da tarefa">
              <div className="form-field">
                <label htmlFor="drawer-titulo">Título</label>
                <input
                  id="drawer-titulo"
                  type="text"
                  maxLength={200}
                  disabled={bloqueado}
                  value={titulo}
                  onChange={(evento) => setTitulo(evento.target.value)}
                />
              </div>

              <div className="form-field">
                <label htmlFor="drawer-descricao">
                  Descrição{travado ? ' (campo travado pós-início)' : ''}
                </label>
                {travado ? (
                  <div id="drawer-descricao" className="field-locked" aria-readonly="true">
                    {tarefa.descricao ?? 'Sem descrição'}
                  </div>
                ) : (
                  <textarea
                    id="drawer-descricao"
                    rows={3}
                    maxLength={4000}
                    disabled={bloqueado}
                    value={descricao}
                    onChange={(evento) => setDescricao(evento.target.value)}
                  />
                )}
              </div>

              <div className="form-field">
                <label htmlFor="drawer-tipo">Tipo{travado ? ' (campo travado pós-início)' : ''}</label>
                <select
                  id="drawer-tipo"
                  disabled={bloqueado || travado}
                  value={tipo}
                  onChange={(evento) => setTipo(evento.target.value as TipoTarefa)}
                >
                  {TIPOS.map((valor) => (
                    <option key={valor} value={valor}>
                      {rotuloTipo(valor)}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-field">
                <label htmlFor="drawer-prioridade">Prioridade</label>
                <select
                  id="drawer-prioridade"
                  disabled={bloqueado}
                  value={prioridade}
                  onChange={(evento) => setPrioridade(evento.target.value as PrioridadeTarefa)}
                >
                  {PRIORIDADES.map((valor) => (
                    <option key={valor} value={valor}>
                      {rotuloPrioridade(valor)}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-field">
                <label htmlFor="drawer-raia">Raia</label>
                <select
                  id="drawer-raia"
                  disabled={bloqueado}
                  value={raiaId}
                  onChange={(evento) => setRaiaId(evento.target.value)}
                >
                  {raias.map((raia) => (
                    <option key={raia.id} value={raia.id}>
                      {raia.nome}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-field">
                <label htmlFor="drawer-responsavel">Responsável</label>
                <select
                  id="drawer-responsavel"
                  disabled={somenteLeitura || !podeAtribuir}
                  value={tarefa.responsavelId ?? ''}
                  onChange={(evento) => void alterarResponsavel(evento.target.value)}
                >
                  <option value="">Sem responsável</option>
                  {membros.map((membro) => (
                    <option key={membro.usuarioId} value={membro.usuarioId}>
                      {membro.nome}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-field">
                <label htmlFor="drawer-etapa">Etapa atual</label>
                <div id="drawer-etapa" className="field-locked" aria-readonly="true">
                  {etapas.find((etapa) => etapa.id === tarefa.etapaId)?.nome ?? '—'}
                </div>
              </div>

              <div className="form-field toggle">
                <label htmlFor="drawer-impedido">Marcado como impedido</label>
                <input
                  id="drawer-impedido"
                  type="checkbox"
                  disabled={somenteLeitura || !podeImpedir}
                  aria-describedby="drawer-impedido-desc"
                  checked={tarefa.impedida}
                  onChange={(evento) => void alternarImpedimento(evento.target.checked)}
                />
                <span id="drawer-impedido-desc" className="text-secondary">
                  Inicia contagem de lead-time de impedimento (RF-004)
                </span>
              </div>

              {!tarefa.impedida && (
                <div className="form-field">
                  <label htmlFor="drawer-motivo">Motivo do impedimento (opcional)</label>
                  <input
                    id="drawer-motivo"
                    type="text"
                    maxLength={500}
                    disabled={somenteLeitura || !podeImpedir}
                    value={motivo}
                    onChange={(evento) => setMotivo(evento.target.value)}
                  />
                </div>
              )}

              <div className="form-field">
                <span>Lead-time por etapa</span>
                <p className="text-secondary">
                  {tarefa.leadTimePorEtapa.length === 0
                    ? 'Sem histórico de etapas ainda.'
                    : tarefa.leadTimePorEtapa
                        .map((etapa) => `${etapa.etapaNome}: ${duracao(etapa.duracaoSegundos)}`)
                        .join(' · ')}
                  {' · '}
                  Impedimento acumulado: {duracao(tarefa.impedimentoTotalSegundos)}
                </p>
              </div>

              <button
                className="btn btn-primary"
                type="button"
                disabled={bloqueado || salvando}
                aria-busy={salvando}
                onClick={() => void salvar()}
              >
                {salvando ? 'Salvando…' : 'Salvar'}
              </button>
            </section>

            <section aria-label="Histórico de auditoria">
              <h2 className="section-title">Histórico</h2>
              {historico.length === 0 && (
                <div className="empty-state empty-state--inline">Sem histórico registrado.</div>
              )}
              {historico.map((item) => (
                <div key={item.id} className="history-item">
                  <strong>{item.campo}</strong>: {item.valorAnterior ?? '—'} →{' '}
                  {item.valorNovo ?? '—'}{' '}
                  <span className="text-secondary">{dataHora(item.ocorridoEm)}</span>
                </div>
              ))}
              {temMaisHistorico && (
                <button
                  className="btn btn-outline"
                  type="button"
                  onClick={() => void carregarMaisHistorico()}
                >
                  Carregar mais
                </button>
              )}
            </section>
          </>
        )}
      </aside>
    </div>
  );
}
