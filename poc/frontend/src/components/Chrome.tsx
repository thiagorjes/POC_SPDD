'use client';

import Link from 'next/link';
import { useParams, usePathname } from 'next/navigation';
import { useEffect, useState, type ReactNode } from 'react';
import { api } from '@/lib/api';
import { sair } from '@/lib/auth';
import type { Notificacao, UsuarioAtual } from '@/lib/types';

/** Sidebar + Topbar compartilhados por TL-02..TL-10. TL-01 (login) nao usa este chrome. */
export function Chrome({ children }: { children: ReactNode }) {
  const params = useParams<{ id?: string }>();
  const caminho = usePathname();
  const projetoId = params?.id;

  return (
    <div className="layout">
      <nav className="sidebar" aria-label="Navegacao principal">
        <strong>Kanban</strong>
        <ul style={{ listStyle: 'none', padding: 0, display: 'grid', gap: 8, marginTop: 16 }}>
          <li>
            <Link href="/projetos" aria-current={caminho === '/projetos' ? 'page' : undefined}>
              Projetos
            </Link>
          </li>
          {projetoId && (
            <>
              <li>
                <Link href={`/projetos/${projetoId}/board`}>Board</Link>
              </li>
              <li>
                <Link href={`/projetos/${projetoId}/dashboard`}>Dashboard</Link>
              </li>
              <li>
                <Link href={`/projetos/${projetoId}/admin`}>Admin do projeto</Link>
              </li>
              <li>
                <Link href={`/projetos/${projetoId}/admin/papeis`}>Papeis e toggles</Link>
              </li>
              <li>
                <Link href={`/projetos/${projetoId}/admin/usuarios`}>Usuarios</Link>
              </li>
            </>
          )}
        </ul>
      </nav>
      <div>
        <Topbar />
        <main className="conteudo">{children}</main>
      </div>
    </div>
  );
}

function Topbar() {
  const [usuario, setUsuario] = useState<UsuarioAtual | null>(null);
  const [notificacoes, setNotificacoes] = useState<Notificacao[]>([]);
  const [aberto, setAberto] = useState(false);

  useEffect(() => {
    void api.eu().then(setUsuario).catch(() => setUsuario(null));
  }, []);

  useEffect(() => {
    const carregar = () => void api.notificacoes(true).then(setNotificacoes).catch(() => undefined);
    carregar();
    const timer = setInterval(carregar, 30000);
    return () => clearInterval(timer);
  }, []);

  return (
    <header className="topbar">
      <span />
      <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
        <div>
          <button
            aria-expanded={aberto}
            aria-label={`Notificacoes: ${notificacoes.length} nao lidas`}
            onClick={() => setAberto((a) => !a)}
          >
            Notificacoes ({notificacoes.length})
          </button>
          {aberto && (
            <ul
              className="cartao"
              aria-live="polite"
              style={{ position: 'absolute', right: 24, listStyle: 'none', maxWidth: 380 }}
            >
              {notificacoes.length === 0 && <li>Nenhuma notificacao nao lida.</li>}
              {notificacoes.map((n) => (
                <li key={n.id} style={{ marginBottom: 8 }}>
                  {n.mensagem}{' '}
                  <button
                    onClick={async () => {
                      await api.marcarNotificacaoLida(n.id);
                      setNotificacoes((atuais) => atuais.filter((x) => x.id !== n.id));
                    }}
                  >
                    Marcar lida
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
        <span>{usuario?.nome ?? '—'}</span>
        <button onClick={sair}>Sair</button>
      </div>
    </header>
  );
}
