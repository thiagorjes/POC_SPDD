'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import type { ReactNode } from 'react';
import { Topbar } from './Topbar';

interface Props {
  /** Projeto ativo destacado no rodape da sidebar; ausente na lista de projetos (TL-02). */
  projetoId?: string;
  projetoNome?: string;
  children: ReactNode;
}

/** Layout global das telas internas TL-02..TL-10: sidebar colapsavel + topbar. */
export function AppShell({ projetoId, projetoNome, children }: Props) {
  const caminho = usePathname();
  const emProjetos = caminho === '/projetos';
  const emDashboard = caminho?.endsWith('/dashboard') === true;
  const emAdmin = caminho?.includes('/admin') === true;

  return (
    <div className="app-shell">
      <aside className="sidebar" aria-label="Navegação principal">
        <div className="sidebar__brand">Kanban</div>
        <nav aria-label="Menu">
          <Link href="/projetos" aria-current={emProjetos ? 'page' : undefined}>
            Projetos
          </Link>
          {projetoId && (
            <>
              <Link
                href={`/projetos/${projetoId}/board`}
                aria-current={caminho?.endsWith('/board') ? 'page' : undefined}
              >
                Board
              </Link>
              <Link
                href={`/projetos/${projetoId}/dashboard`}
                aria-current={emDashboard ? 'page' : undefined}
              >
                Dashboard
              </Link>
              <Link
                href={`/projetos/${projetoId}/admin`}
                aria-current={emAdmin ? 'page' : undefined}
              >
                Admin
              </Link>
            </>
          )}
        </nav>
        {projetoNome && <div className="sidebar__project-active">Projeto ativo: {projetoNome}</div>}
      </aside>

      <Topbar />

      <main className="main">{children}</main>
    </div>
  );
}
