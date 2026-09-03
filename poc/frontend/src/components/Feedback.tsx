'use client';

import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import { ApiError, exigeModal } from '@/lib/api';

interface Aviso {
  id: number;
  texto: string;
  erro: boolean;
}

interface FeedbackApi {
  informar: (texto: string) => void;
  reportar: (erro: unknown) => void;
}

const Contexto = createContext<FeedbackApi | null>(null);

/**
 * Erro informativo vira toast; erro que exige atencao vira modal bloqueante (DDR-003). A mensagem
 * exibida e sempre a que o backend devolveu — o front nao inventa texto de erro.
 */
export function FeedbackProvider({ children }: { children: ReactNode }) {
  const [avisos, setAvisos] = useState<Aviso[]>([]);
  const [bloqueio, setBloqueio] = useState<string | null>(null);

  const informar = useCallback((texto: string) => {
    const id = Date.now() + Math.random();
    setAvisos((atuais) => [...atuais, { id, texto, erro: false }]);
    setTimeout(() => setAvisos((atuais) => atuais.filter((a) => a.id !== id)), 5000);
  }, []);

  const reportar = useCallback((erro: unknown) => {
    const texto =
      erro instanceof ApiError || erro instanceof Error
        ? erro.message
        : 'Ocorreu um erro inesperado.';
    if (exigeModal(erro)) {
      setBloqueio(texto);
      return;
    }
    const id = Date.now() + Math.random();
    setAvisos((atuais) => [...atuais, { id, texto, erro: true }]);
    setTimeout(() => setAvisos((atuais) => atuais.filter((a) => a.id !== id)), 7000);
  }, []);

  const valor = useMemo(() => ({ informar, reportar }), [informar, reportar]);

  return (
    <Contexto.Provider value={valor}>
      {children}
      <div className="toasts" role="status" aria-live="polite">
        {avisos.map((aviso) => (
          <div key={aviso.id} className={aviso.erro ? 'toast erro' : 'toast'}>
            {aviso.texto}
          </div>
        ))}
      </div>
      {bloqueio && (
        <div className="backdrop" role="alertdialog" aria-modal="true" aria-label="Acao bloqueada">
          <div className="modal">
            <h2>Acao nao concluida</h2>
            <p>{bloqueio}</p>
            <div>
              <button className="primario" onClick={() => setBloqueio(null)}>
                Entendi
              </button>
            </div>
          </div>
        </div>
      )}
    </Contexto.Provider>
  );
}

export function useFeedback(): FeedbackApi {
  const contexto = useContext(Contexto);
  if (!contexto) {
    throw new Error('useFeedback exige FeedbackProvider.');
  }
  return contexto;
}
