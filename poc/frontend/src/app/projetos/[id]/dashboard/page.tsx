'use client';

import { use } from 'react';
import { Dashboard } from '@/components/Dashboard';
import { AvisoSomenteLeitura, SemPermissao, Skeleton } from '@/components/Estados';
import { PageHeader } from '@/components/ui/PageHeader';
import { usePermissoes } from '@/hooks/usePermissoes';

/** TL-07. Projeto finalizado continua legivel: o dashboard e leitura pura (RN-015). */
export default function DashboardPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const { carregando, pode, projetoAtivo } = usePermissoes(id);

  if (carregando) {
    return <Skeleton linhas={4} />;
  }
  if (!pode('dashboard:visualizar')) {
    return <SemPermissao />;
  }

  return (
    <>
      <PageHeader titulo="Dashboard de lead-time" />
      {!projetoAtivo && <AvisoSomenteLeitura />}
      <Dashboard projetoId={id} />
    </>
  );
}
