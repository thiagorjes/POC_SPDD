'use client';

import Link from 'next/link';
import { useParams, usePathname } from 'next/navigation';
import { useEffect, useState, type ReactNode } from 'react';
import { Avatar } from './ui/Avatar';
import { Botao } from './ui/Botao';
import { api } from '@/lib/api';
import { sair } from '@/lib/auth';
import type { Notificacao, UsuarioAtual } from '@/lib/types';

/** Sidebar + Topbar compartilhados por TL-02..TL-10. TL-01 (login) nao usa este chrome. */
export function Chrome({ children }: { children: ReactNode }) {
  const params = useParams<{ id?: string }>();
  const projetoId = params?.id;

  return (
    <div className="app-shell">
      <Sidebar projetoId={projetoId} />
      <Topbar />
      <main className="main">{children}</main>
    </div>
  );
}

function Sidebar({ projetoId }: { projetoId?: string }) {
  const caminho = usePathname();
  const [nomeProjeto, setNomeProjeto] = useState<string | null>(null);

  useEffect(() => {
    if (!projetoId) {
      setNomeProjeto(null);
      return;
    }
    void api
      .projetos()
      .then((lista) => setNomeProjeto(lista.find((p) => p.id === projetoId)?.nome ?? null))
      .catch(() => setNomeProjeto(null));
  }, [projetoId]);

  const atual = (href: string) => (caminho === href ? 'page' : undefined);

  return (
    <aside className="sidebar" aria-label="Navegação principal">
      <div className="sidebar__brand">Kanban</div>
      <nav aria-label="Menu">
        <Link href="/projetos" aria-current={atual('/projetos')}>
          Projetos
        </Link>
        {projetoId && (
          <>
            <Link href={`/projetos/${projetoId}/board`} aria-current={atual(`/projetos/${projetoId}/board`)}>
              Board
            </Link>
            <Link
              href={`/projetos/${projetoId}/dashboard`}
              aria-current={atual(`/projetos/${projetoId}/dashboard`)}
            >
              Dashboard
            </Link>
            <Link href={`/projetos/${projetoId}/admin`} aria-current={atual(`/projetos/${projetoId}/admin`)}>
              Administração
            </Link>
          </>
        )}
      </nav>
      {/* Omitido quando o nome nao pode ser resolvido: nao inventamos rotulo. */}
      {nomeProjeto && <div className="sidebar__project-active">Projeto ativo: {nomeProjeto}</div>}
    </aside>
  );
}

function formatarContagem(n: number): string {
  return n > 9 ? '9+' : String(n);
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
      {/* position: relative ancora o dropdown, que antes escapava para o viewport. */}
      <div style={{ position: 'relative' }}>
        <button
          className="topbar__notif"
          aria-expanded={aberto}
          aria-label={`Notificações: ${notificacoes.length} não lidas`}
          onClick={() => setAberto((a) => !a)}
        >
          <span aria-hidden="true">🔔</span>
          {notificacoes.length > 0 && (
            <span className="topbar__notif-badge">{formatarContagem(notificacoes.length)}</span>
          )}
        </button>
        {aberto && (
          <ul className="card dropdown" aria-live="polite">
            {notificacoes.length === 0 && <li>Nenhuma notificação não lida.</li>}
            {notificacoes.map((n) => (
              <li key={n.id}>
                <span>{n.mensagem}</span>
                <Botao
                  variante="text"
                  onClick={async () => {
                    await api.marcarNotificacaoLida(n.id);
                    setNotificacoes((atuais) => atuais.filter((x) => x.id !== n.id));
                  }}
                >
                  Marcar lida
                </Botao>
              </li>
            ))}
          </ul>
        )}
      </div>
      <div className="topbar__user">
        <Avatar nome={usuario?.nome} />
        <span>{usuario?.nome ?? '—'}</span>
      </div>
      <Botao variante="text" onClick={sair}>
        Sair
      </Botao>
    </header>
  );
}
