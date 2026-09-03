'use client';

import { useCallback, useEffect, useState } from 'react';
import { api } from '@/lib/api';
import type { PermissoesEfetivas } from '@/lib/types';

/**
 * UI condicional. Esconder ou desabilitar aqui e conveniencia: o backend revalida toda acao
 * sensivel e continua sendo a unica autoridade (RNF-003).
 */
export function usePermissoes(projetoId: string) {
  const [dados, setDados] = useState<PermissoesEfetivas | null>(null);
  const [carregando, setCarregando] = useState(true);

  const recarregar = useCallback(async () => {
    setCarregando(true);
    try {
      setDados(await api.permissoes(projetoId));
    } finally {
      setCarregando(false);
    }
  }, [projetoId]);

  useEffect(() => {
    void recarregar();
  }, [recarregar]);

  const pode = useCallback(
    (permissao: string) => Boolean(dados?.permissoes.includes(permissao)),
    [dados],
  );

  return {
    permissoes: dados,
    carregando,
    pode,
    toggle: (chave: string) => Boolean(dados?.toggles?.[chave]),
    projetoAtivo: dados?.projetoAtivo ?? false,
    recarregar,
  };
}
