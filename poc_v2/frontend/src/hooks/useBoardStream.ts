'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { api } from '@/lib/api';
import { assinarBoard } from '@/lib/stomp';
import type { BoardSnapshotResponse } from '@/lib/types';

export interface EstadoBoard {
  snapshot: BoardSnapshotResponse | null;
  carregando: boolean;
  erro: string | null;
  conectado: boolean;
  recarregar: () => Promise<void>;
}

/**
 * Mantem o snapshot do board sincronizado (ADR-004).
 *
 * O resync completo (`GET /api/projetos/{id}/board`) e disparado por gap de sequencia
 * (`seq > ultimo + 1`), por reconexao do WebSocket e pelo retorno de foco da aba — este ultimo
 * cobre a perda do *ultimo* evento, que nao produz gap detectavel.
 */
export function useBoardStream(projetoId: string, usuarioId: string | null): EstadoBoard {
  const [snapshot, setSnapshot] = useState<BoardSnapshotResponse | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [conectado, setConectado] = useState(false);
  const ultimoSeq = useRef(0);

  const recarregar = useCallback(async () => {
    try {
      const atual = await api.board(projetoId);
      ultimoSeq.current = atual.seq;
      setSnapshot(atual);
      setErro(null);
    } catch {
      setErro('Não foi possível carregar o board.');
    } finally {
      setCarregando(false);
    }
  }, [projetoId]);

  useEffect(() => {
    setCarregando(true);
    void recarregar();
  }, [recarregar]);

  useEffect(() => {
    const assinatura = assinarBoard(projetoId, usuarioId, {
      aoEvento: (evento) => {
        if (evento.projetoId !== projetoId) {
          return;
        }
        if (evento.seq > ultimoSeq.current + 1) {
          void recarregar();
          return;
        }
        ultimoSeq.current = Math.max(ultimoSeq.current, evento.seq);
        void recarregar();
      },
      aoConectar: () => {
        setConectado(true);
        void recarregar();
      },
      aoDesconectar: () => setConectado(false),
    });
    return () => assinatura.encerrar();
  }, [projetoId, usuarioId, recarregar]);

  useEffect(() => {
    const aoFocar = () => {
      if (document.visibilityState === 'visible') {
        void recarregar();
      }
    };
    document.addEventListener('visibilitychange', aoFocar);
    return () => document.removeEventListener('visibilitychange', aoFocar);
  }, [recarregar]);

  return { snapshot, carregando, erro, conectado, recarregar };
}
