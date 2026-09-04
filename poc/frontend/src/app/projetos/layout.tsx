'use client';

import { useRouter } from 'next/navigation';
import { useEffect, useState, type ReactNode } from 'react';
import { Chrome } from '@/components/Chrome';
import { Skeleton } from '@/components/Estados';
import { inicializarAuth } from '@/lib/auth';

export default function ProjetosLayout({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [pronto, setPronto] = useState(false);

  useEffect(() => {
    inicializarAuth()
      .then((autenticado) => (autenticado ? setPronto(true) : router.replace('/login')))
      .catch(() => router.replace('/login'));
  }, [router]);

  if (!pronto) {
    return (
      <main className="main">
        <Skeleton linhas={4} />
      </main>
    );
  }
  return <Chrome>{children}</Chrome>;
}
