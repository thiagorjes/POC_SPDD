'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { use, type ReactNode } from 'react';
import { PageHeader } from '@/components/ui/PageHeader';

const ABAS = [
  { href: '', rotulo: 'Workflow' },
  { href: '/papeis', rotulo: 'Papéis e toggles' },
  { href: '/usuarios', rotulo: 'Usuários' },
];

export default function AdminLayout({
  children,
  params,
}: {
  children: ReactNode;
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const caminho = usePathname();
  const base = `/projetos/${id}/admin`;

  return (
    <>
      <PageHeader titulo="Admin de Projeto" />
      <nav className="tabs" aria-label="Seções da administração">
        {ABAS.map((aba) => {
          const href = `${base}${aba.href}`;
          return (
            <Link key={href} href={href} aria-current={caminho === href ? 'page' : undefined}>
              {aba.rotulo}
            </Link>
          );
        })}
      </nav>
      {children}
    </>
  );
}
