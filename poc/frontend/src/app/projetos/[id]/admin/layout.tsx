'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { use, type ReactNode } from 'react';

const ABAS = [
  { href: '', rotulo: 'Workflow' },
  { href: '/papeis', rotulo: 'Papeis e toggles' },
  { href: '/usuarios', rotulo: 'Usuarios' },
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
      <h1>Administracao do projeto</h1>
      <nav className="abas" aria-label="Secoes da administracao">
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
