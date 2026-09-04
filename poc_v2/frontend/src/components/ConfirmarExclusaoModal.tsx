'use client';

import { useState } from 'react';
import { useFeedback } from './FeedbackProvider';
import { api, mensagemDeErro } from '@/lib/api';

interface Props {
  tarefaId: string;
  titulo: string;
  /** Falso quando o toggle `DEV_PODE_EXCLUIR_TAREFA` bloqueia a acao para o papel do usuario. */
  permitido: boolean;
  aoFechar: () => void;
  aoExcluir: () => void;
}

/**
 * TL-06 — Confirmacao de exclusao de card (RF-019). O bloqueio por toggle e exibido no proprio
 * modal; a autorizacao real e revalidada pela Service Layer no backend.
 */
export function ConfirmarExclusaoModal({
  tarefaId,
  titulo,
  permitido,
  aoFechar,
  aoExcluir,
}: Props) {
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState<string | null>(
    permitido
      ? null
      : 'Você não tem permissão para excluir este card. A opção "dev pode excluir tarefa" está desabilitada neste projeto.',
  );
  const feedback = useFeedback();

  async function excluir() {
    setEnviando(true);
    try {
      await api.excluirTarefa(tarefaId);
      feedback.sucesso('Card excluído com sucesso.');
      aoExcluir();
    } catch (erroExclusao) {
      setErro(mensagemDeErro(erroExclusao));
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className="modal-overlay">
      <div
        className="modal"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="excluir-title"
        aria-describedby="excluir-desc"
      >
        <h1 id="excluir-title">Excluir card</h1>
        <p id="excluir-desc">
          Tem certeza de que deseja excluir o card <strong>&quot;{titulo}&quot;</strong>? Esta ação
          não pode ser desfeita.
        </p>

        {erro && (
          <div className="toast toast-error" role="alert">
            {erro}
          </div>
        )}

        <div className="modal-actions">
          <button className="btn btn-outline" type="button" disabled={enviando} onClick={aoFechar}>
            {erro && !permitido ? 'Fechar' : 'Cancelar'}
          </button>
          <button
            className="btn btn-danger"
            type="button"
            disabled={enviando || !permitido}
            aria-busy={enviando}
            onClick={() => void excluir()}
          >
            {enviando ? 'Excluindo…' : 'Excluir'}
          </button>
        </div>
      </div>
    </div>
  );
}
