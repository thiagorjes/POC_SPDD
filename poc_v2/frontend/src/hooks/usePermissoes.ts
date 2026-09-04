'use client';

import { useCallback, useEffect, useState } from 'react';
import { api } from '@/lib/api';
import type { ChaveToggle, PermissoesEfetivasResponse } from '@/lib/types';

export interface EstadoPermissoes {
  dados: PermissoesEfetivasResponse | null;
  carregando: boolean;
  /** Permissao efetiva do usuario no projeto. Serve apenas para esconder/desabilitar UI. */
  possui: (permissao: string) => boolean;
  toggleHabilitado: (chave: ChaveToggle) => boolean;
  projetoAtivo: boolean;
  recarregar: () => Promise<void>;
}

/**
 * Consulta as permissoes efetivas do usuario no projeto (RNF-003).
 *
 * A UI condicional e conveniencia: a autorizacao real acontece na Service Layer do backend, que
 * revalida toda regra exibida aqui. Nunca tratar este hook como autoridade.
 */
export function usePermissoes(projetoId: string): EstadoPermissoes {
  const [dados, setDados] = useState<PermissoesEfetivasResponse | null>(null);
  const [carregando, setCarregando] = useState(true);

  const recarregar = useCallback(async () => {
    try {
      setDados(await api.permissoes(projetoId));
    } catch {
      setDados(null);
    } finally {
      setCarregando(false);
    }
  }, [projetoId]);

  useEffect(() => {
    setCarregando(true);
    void recarregar();
  }, [recarregar]);

  const possui = useCallback(
    (permissao: string) => dados?.adminGlobal === true || dados?.permissoes.includes(permissao) === true,
    [dados],
  );

  const toggleHabilitado = useCallback(
    (chave: ChaveToggle) => dados?.toggles?.[chave] === true,
    [dados],
  );

  return {
    dados,
    carregando,
    possui,
    toggleHabilitado,
    projetoAtivo: dados?.projetoAtivo !== false,
    recarregar,
  };
}
