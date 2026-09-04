'use client';

import { useState } from 'react';
import { Botao } from './ui/Botao';
import { ApiError } from '@/lib/api';

/**
 * TL-06. A mensagem de bloqueio por permissao ou toggle e exibida <b>dentro</b> do modal: a
 * tentativa vai ao backend e a recusa e mostrada aqui, sem esconder o botao com base na UI.
 */
export function ConfirmarExclusaoModal({
  titulo,
  impacto,
  aoConfirmar,
  aoFechar,
}: {
  titulo: string;
  impacto: string;
  aoConfirmar: () => Promise<void>;
  aoFechar: () => void;
}) {
  const [erro, setErro] = useState<string | null>(null);
  const [executando, setExecutando] = useState(false);

  return (
    <div className="modal-overlay">
      <div
        className="modal"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="exclusao-titulo"
        aria-describedby="exclusao-desc"
      >
        <h1 id="exclusao-titulo" style={{ fontSize: 'var(--fonte-scale-lg)' }}>
          Excluir card
        </h1>
        <p id="exclusao-desc">
          Tem certeza de que deseja excluir <strong>&quot;{titulo}&quot;</strong>? {impacto}
        </p>

        {erro && (
          <div className="toast toast-error" role="alert">
            {erro}
          </div>
        )}

        <div className="modal-actions">
          <Botao variante="outline" onClick={aoFechar}>
            Cancelar
          </Botao>
          <Botao
            variante="danger"
            carregando={executando}
            disabled={erro !== null}
            onClick={async () => {
              setExecutando(true);
              setErro(null);
              try {
                await aoConfirmar();
                aoFechar();
              } catch (e) {
                setErro(e instanceof ApiError || e instanceof Error ? e.message : 'Falha ao excluir.');
              } finally {
                setExecutando(false);
              }
            }}
          >
            {executando ? 'Excluindo…' : 'Excluir'}
          </Botao>
        </div>
      </div>
    </div>
  );
}
