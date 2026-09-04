'use client';

import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import { exigeModal, mensagemDeErro } from '@/lib/api';

interface Toast {
  id: number;
  tipo: 'sucesso' | 'erro';
  mensagem: string;
}

interface Feedback {
  /** Toast informativo (DDR-003). */
  sucesso: (mensagem: string) => void;
  /** Toast de erro sem bloqueio de fluxo. */
  falha: (mensagem: string) => void;
  /**
   * Reporta um erro do backend escolhendo toast ou modal pelo `errorCode` (DDR-003): codigos que
   * exigem atencao explicita abrem modal, os demais viram toast.
   */
  reportar: (erro: unknown) => void;
}

const FeedbackContext = createContext<Feedback | null>(null);

export function useFeedback(): Feedback {
  const contexto = useContext(FeedbackContext);
  if (!contexto) {
    throw new Error('useFeedback exige FeedbackProvider na arvore.');
  }
  return contexto;
}

export function FeedbackProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const [modal, setModal] = useState<string | null>(null);

  const empilhar = useCallback((tipo: Toast['tipo'], mensagem: string) => {
    const id = Date.now() + Math.random();
    setToasts((atuais) => [...atuais, { id, tipo, mensagem }]);
    window.setTimeout(() => {
      setToasts((atuais) => atuais.filter((toast) => toast.id !== id));
    }, 6000);
  }, []);

  const valor = useMemo<Feedback>(
    () => ({
      sucesso: (mensagem) => empilhar('sucesso', mensagem),
      falha: (mensagem) => empilhar('erro', mensagem),
      reportar: (erro) => {
        const mensagem = mensagemDeErro(erro);
        if (exigeModal(erro)) {
          setModal(mensagem);
        } else {
          empilhar('erro', mensagem);
        }
      },
    }),
    [empilhar],
  );

  return (
    <FeedbackContext.Provider value={valor}>
      {children}

      <div className="toast-stack">
        {toasts.map((toast) => (
          <div
            key={toast.id}
            className={toast.tipo === 'erro' ? 'toast toast-error' : 'toast toast-success'}
            role={toast.tipo === 'erro' ? 'alert' : 'status'}
            aria-live={toast.tipo === 'erro' ? 'assertive' : 'polite'}
          >
            {toast.mensagem}
          </div>
        ))}
      </div>

      {modal !== null && (
        <div className="modal-overlay">
          <div
            className="modal"
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="feedback-modal-title"
            aria-describedby="feedback-modal-desc"
          >
            <h1 id="feedback-modal-title">Ação bloqueada</h1>
            <p id="feedback-modal-desc">{modal}</p>
            <div className="modal-actions">
              <button className="btn btn-outline" type="button" onClick={() => setModal(null)}>
                Fechar
              </button>
            </div>
          </div>
        </div>
      )}
    </FeedbackContext.Provider>
  );
}
