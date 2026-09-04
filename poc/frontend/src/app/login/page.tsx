'use client';

import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { Botao } from '@/components/ui/Botao';
import { entrar, inicializarAuth } from '@/lib/auth';

/** TL-01. Sem fallback local: se o Keycloak nao responder, o login falha com erro explicito (ADR-006). */
export default function LoginPage() {
  const router = useRouter();
  const [estado, setEstado] = useState<'verificando' | 'anonimo' | 'redirecionando' | 'falha'>(
    'verificando',
  );
  const [mensagem, setMensagem] = useState('');

  useEffect(() => {
    inicializarAuth()
      .then((autenticado) => {
        if (autenticado) {
          setEstado('redirecionando');
          router.replace('/projetos');
        } else {
          setEstado('anonimo');
        }
      })
      .catch((erro: Error) => {
        setMensagem(erro.message);
        setEstado('falha');
      });
  }, [router]);

  return (
    <main style={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}>
      <div className="card" style={{ width: 380, maxWidth: '92vw', textAlign: 'center' }}>
        <h1 style={{ fontSize: 'var(--fonte-scale-lg)' }}>Kanban de Tarefas</h1>
        <p className="text-secondary">
          Autentique-se com sua conta corporativa (Keycloak) para continuar.
        </p>

        {estado === 'verificando' && (
          <p className="text-secondary" role="status" aria-live="polite" aria-busy="true">
            Verificando sessão…
          </p>
        )}
        {estado === 'redirecionando' && (
          <p className="text-secondary" role="status" aria-live="polite">
            Redirecionando…
          </p>
        )}
        {estado === 'falha' && (
          <div className="toast toast-error" role="alert" style={{ marginBottom: 'var(--espaco-scale-md)' }}>
            {mensagem}
          </div>
        )}

        {(estado === 'anonimo' || estado === 'falha') && (
          <Botao variante="primary" className="full" onClick={entrar}>
            Entrar com Keycloak
          </Botao>
        )}
      </div>
    </main>
  );
}
