'use client';

import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
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
    <main
      style={{
        minHeight: '100vh',
        display: 'grid',
        placeItems: 'center',
      }}
    >
      <div className="cartao" style={{ width: 'min(420px, 92vw)', display: 'grid', gap: 16 }}>
        <h1>Kanban de Tarefas</h1>
        {estado === 'verificando' && <p aria-busy="true">Verificando sessao…</p>}
        {estado === 'redirecionando' && <p aria-live="polite">Redirecionando…</p>}
        {estado === 'falha' && (
          <p className="erro-inline" role="alert">
            {mensagem}
          </p>
        )}
        {(estado === 'anonimo' || estado === 'falha') && (
          <button className="primario" onClick={entrar}>
            Entrar com Keycloak
          </button>
        )}
      </div>
    </main>
  );
}
