'use client';

import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { useSessao } from '@/components/AuthProvider';
import { entrar } from '@/lib/auth';

/** TL-01 — Login via SSO Keycloak. Nao ha fallback de autenticacao local (ADR-006). */
export default function LoginPage() {
  const { usuario, carregando } = useSessao();
  const [redirecionando, setRedirecionando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const router = useRouter();

  useEffect(() => {
    if (usuario) {
      router.replace('/projetos');
    }
  }, [usuario, router]);

  async function autenticar() {
    setRedirecionando(true);
    setErro(null);
    try {
      await entrar();
    } catch {
      setRedirecionando(false);
      setErro(
        'Não foi possível autenticar. Verifique suas credenciais no Keycloak e tente novamente.',
      );
    }
  }

  return (
    <div className="login-shell">
      <main className="card login-card">
        <h1>Kanban de Tarefas</h1>
        <p className="text-secondary">
          Autentique-se com sua conta corporativa (Keycloak) para continuar.
        </p>

        {carregando || redirecionando ? (
          <section aria-label="Redirecionamento">
            <div className="skeleton" />
            <p className="text-secondary" role="status" aria-live="polite">
              Redirecionando para o provedor de identidade…
            </p>
          </section>
        ) : (
          <section aria-label="Login">
            <button className="btn btn-primary btn--full" type="button" onClick={() => void autenticar()}>
              Entrar com Keycloak
            </button>
          </section>
        )}

        {erro && (
          <div className="toast toast-error" role="alert">
            {erro}
          </div>
        )}

        {usuario && (
          <div className="toast toast-success" role="status" aria-live="polite">
            Login efetuado. Redirecionando para a lista de projetos…
          </div>
        )}
      </main>
    </div>
  );
}
