'use client';

import { useState } from 'react';
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
    <div className="backdrop" role="alertdialog" aria-modal="true" aria-label="Confirmar exclusao">
      <div className="modal">
        <h2>Excluir {titulo}?</h2>
        <p>{impacto}</p>
        {erro && (
          <p className="erro-inline" role="alert">
            {erro}
          </p>
        )}
        <div style={{ display: 'flex', gap: 8 }}>
          <button
            className="perigo"
            disabled={executando}
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
            Excluir
          </button>
          <button onClick={aoFechar}>Cancelar</button>
        </div>
      </div>
    </div>
  );
}
