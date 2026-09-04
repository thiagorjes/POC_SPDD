'use client';

import { usePathname, useRouter } from 'next/navigation';
import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { api } from '@/lib/api';
import { autenticado, inicializarAuth, sair } from '@/lib/auth';
import type { UsuarioResponse } from '@/lib/types';

interface Sessao {
  usuario: UsuarioResponse | null;
  carregando: boolean;
  encerrar: () => void;
}

const SessaoContext = createContext<Sessao>({ usuario: null, carregando: true, encerrar: () => {} });

export function useSessao(): Sessao {
  return useContext(SessaoContext);
}

/**
 * Inicializa o Keycloak e provisiona a identidade da aplicacao chamando `/api/usuarios/me`
 * (provisionamento JIT no backend, ADR-003/007). Sem sessao valida, redireciona para TL-01.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<UsuarioResponse | null>(null);
  const [carregando, setCarregando] = useState(true);
  const router = useRouter();
  const caminho = usePathname();

  useEffect(() => {
    let ativo = true;
    void (async () => {
      await inicializarAuth();
      if (!ativo) {
        return;
      }
      if (!autenticado()) {
        setCarregando(false);
        if (caminho !== '/login') {
          router.replace('/login');
        }
        return;
      }
      try {
        const eu = await api.eu();
        if (ativo) {
          setUsuario(eu);
        }
      } finally {
        if (ativo) {
          setCarregando(false);
        }
      }
    })();
    return () => {
      ativo = false;
    };
  }, [router, caminho]);

  return (
    <SessaoContext.Provider
      value={{
        usuario,
        carregando,
        encerrar: () => {
          void sair();
        },
      }}
    >
      {children}
    </SessaoContext.Provider>
  );
}
