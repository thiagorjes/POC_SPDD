'use client';

import { useCallback, useEffect, useState } from 'react';
import { useSessao } from './AuthProvider';
import { api } from '@/lib/api';
import { dataHora, iniciais } from '@/lib/format';
import type { NotificacaoResponse } from '@/lib/types';

/** Topbar global: sino de notificacoes (RF-005) com `aria-live` e menu de usuario/logout. */
export function Topbar() {
  const { usuario, encerrar } = useSessao();
  const [notificacoes, setNotificacoes] = useState<NotificacaoResponse[]>([]);
  const [aberto, setAberto] = useState(false);

  const carregar = useCallback(async () => {
    try {
      const pagina = await api.notificacoes(false);
      setNotificacoes(pagina.content);
    } catch {
      setNotificacoes([]);
    }
  }, []);

  useEffect(() => {
    if (usuario) {
      void carregar();
    }
  }, [usuario, carregar]);

  const naoLidas = notificacoes.filter((n) => n.lidaEm === null).length;

  async function marcarLida(id: string) {
    await api.marcarNotificacaoLida(id);
    await carregar();
  }

  return (
    <header className="topbar">
      <button
        className="topbar__notif"
        type="button"
        aria-label={`Notificações (${naoLidas} não lidas)`}
        aria-expanded={aberto}
        onClick={() => setAberto((valor) => !valor)}
      >
        🔔
        {naoLidas > 0 && <span className="topbar__notif-badge">{naoLidas}</span>}
      </button>

      {aberto && (
        <div className="topbar__panel" role="region" aria-label="Notificações" aria-live="polite">
          {notificacoes.length === 0 ? (
            <div className="empty-state empty-state--inline">Nenhuma notificação.</div>
          ) : (
            notificacoes.map((notificacao) => (
              <div key={notificacao.id} className="topbar__panel-item">
                <span>
                  {notificacao.mensagem}{' '}
                  <span className="text-secondary">{dataHora(notificacao.criadaEm)}</span>
                </span>
                {notificacao.lidaEm === null && (
                  <button
                    className="btn btn-text"
                    type="button"
                    onClick={() => void marcarLida(notificacao.id)}
                  >
                    Marcar lida
                  </button>
                )}
              </div>
            ))
          )}
        </div>
      )}

      <div className="topbar__user">
        <span className="avatar">{iniciais(usuario?.nome)}</span> {usuario?.nome ?? '—'}
        <button className="btn btn-text" type="button" onClick={encerrar}>
          Sair
        </button>
      </div>
    </header>
  );
}
