'use client';

import { useState } from 'react';
import { useFeedback } from './FeedbackProvider';
import { api } from '@/lib/api';
import { rotuloPrioridade, rotuloTipo } from '@/lib/format';
import type {
  MembroProjetoResponse,
  PrioridadeTarefa,
  RaiaResponse,
  TipoTarefa,
} from '@/lib/types';

const TIPOS: TipoTarefa[] = ['FEATURE', 'BUG', 'TAREFA', 'MELHORIA'];
const PRIORIDADES: PrioridadeTarefa[] = ['BAIXA', 'MEDIA', 'ALTA', 'CRITICA'];

interface Props {
  projetoId: string;
  raias: RaiaResponse[];
  membros: MembroProjetoResponse[];
  raiaPreSelecionada: string | null;
  aoFechar: () => void;
  aoCriar: () => void;
}

/**
 * TL-05 — Nova tarefa (RF-018). Raia e responsavel sao opcionais: sem raia o backend usa a raia
 * padrao do projeto e a etapa de menor ordem (RN-CB-004/005).
 */
export function NovaTarefaModal({
  projetoId,
  raias,
  membros,
  raiaPreSelecionada,
  aoFechar,
  aoCriar,
}: Props) {
  const [titulo, setTitulo] = useState('');
  const [descricao, setDescricao] = useState('');
  const [tipo, setTipo] = useState<TipoTarefa>('FEATURE');
  const [prioridade, setPrioridade] = useState<PrioridadeTarefa>('MEDIA');
  const [raiaId, setRaiaId] = useState(raiaPreSelecionada ?? '');
  const [responsavelId, setResponsavelId] = useState('');
  const [tituloInvalido, setTituloInvalido] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const feedback = useFeedback();

  async function submeter(evento: React.FormEvent) {
    evento.preventDefault();
    if (titulo.trim().length === 0) {
      setTituloInvalido(true);
      return;
    }
    setEnviando(true);
    try {
      await api.criarTarefa(projetoId, {
        titulo: titulo.trim(),
        descricao: descricao.trim() === '' ? null : descricao.trim(),
        tipo,
        prioridade,
        raiaId: raiaId === '' ? null : raiaId,
        responsavelId: responsavelId === '' ? null : responsavelId,
      });
      feedback.sucesso('Card criado com sucesso.');
      aoCriar();
    } catch (erro) {
      feedback.reportar(erro);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className="modal-overlay">
      <div className="modal" role="dialog" aria-modal="true" aria-labelledby="nova-tarefa-title">
        <div className="page-header">
          <h1 id="nova-tarefa-title">Novo card</h1>
          <button className="btn btn-text" type="button" aria-label="Fechar" onClick={aoFechar}>
            ✕
          </button>
        </div>

        <form aria-label="Formulário de nova tarefa" onSubmit={submeter}>
          <div className="form-field">
            <label htmlFor="titulo">Título *</label>
            <input
              id="titulo"
              name="titulo"
              type="text"
              required
              aria-required="true"
              aria-invalid={tituloInvalido}
              aria-describedby={tituloInvalido ? 'titulo-erro-msg' : undefined}
              placeholder="Ex.: Corrigir timeout no gateway"
              maxLength={200}
              value={titulo}
              onChange={(evento) => {
                setTitulo(evento.target.value);
                setTituloInvalido(false);
              }}
            />
            {tituloInvalido && (
              <span id="titulo-erro-msg" className="form-error" role="alert">
                Informe o título da tarefa.
              </span>
            )}
          </div>

          <div className="form-field">
            <label htmlFor="descricao">Descrição</label>
            <textarea
              id="descricao"
              name="descricao"
              rows={3}
              maxLength={4000}
              value={descricao}
              onChange={(evento) => setDescricao(evento.target.value)}
            />
          </div>

          <div className="form-field">
            <label htmlFor="tipo">Tipo</label>
            <select
              id="tipo"
              name="tipo"
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
            <label htmlFor="prioridade">Prioridade</label>
            <select
              id="prioridade"
              name="prioridade"
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
            <label htmlFor="raia">Raia (opcional)</label>
            <select
              id="raia"
              name="raia"
              value={raiaId}
              onChange={(evento) => setRaiaId(evento.target.value)}
            >
              <option value="">Raia padrão do projeto</option>
              {raias.map((raia) => (
                <option key={raia.id} value={raia.id}>
                  {raia.nome}
                </option>
              ))}
            </select>
          </div>

          <div className="form-field">
            <label htmlFor="responsavel">Responsável (opcional)</label>
            <select
              id="responsavel"
              name="responsavel"
              value={responsavelId}
              onChange={(evento) => setResponsavelId(evento.target.value)}
            >
              <option value="">Sem responsável</option>
              {membros.map((membro) => (
                <option key={membro.usuarioId} value={membro.usuarioId}>
                  {membro.nome}
                </option>
              ))}
            </select>
          </div>

          <button
            className="btn btn-primary btn--full"
            type="submit"
            disabled={enviando}
            aria-busy={enviando}
          >
            {enviando ? 'Criando…' : 'Criar card'}
          </button>
        </form>
      </div>
    </div>
  );
}
