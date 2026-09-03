'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import type { Client } from '@stomp/stompjs';
import { api } from '@/lib/api';
import { assinar, criarCliente } from '@/lib/stomp';
import type { BoardSnapshot, EventoBoard } from '@/lib/types';

/**
 * Mantem o board convergente sem confiar no stream: o {@code seq} monotonico por projeto detecta
 * gap, e qualquer gap, reconexao ou retorno de foco da aba dispara resync completo (ADR-004).
 *
 * <p>Eventos so carregam ids — o estado autoritativo vem sempre do snapshot REST.
 */
export function useBoardStream(projetoId: string) {
  const [snapshot, setSnapshot] = useState<BoardSnapshot | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [conectado, setConectado] = useState(false);
  const seqRef = useRef(0);
  const clienteRef = useRef<Client | null>(null);

  const resync = useCallback(async () => {
    try {
      const novo = await api.board(projetoId);
      seqRef.current = novo.seq;
      setSnapshot(novo);
      setErro(null);
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Falha ao carregar o board.');
    } finally {
      setCarregando(false);
    }
  }, [projetoId]);

  useEffect(() => {
    void resync();
  }, [resync]);

  useEffect(() => {
    const cliente = criarCliente(
      (conectadoCliente) => {
        setConectado(true);
        // Reconectou: o intervalo offline pode ter perdido eventos.
        void resync();
        assinar<EventoBoard>(conectadoCliente, `/topic/board/${projetoId}`, (evento) => {
          if (evento.seq <= seqRef.current) {
            return; // evento antigo ou duplicado
          }
          if (evento.seq > seqRef.current + 1) {
            void resync(); // gap: nao da para aplicar incrementalmente
            return;
          }
          seqRef.current = evento.seq;
          void resync();
        });
      },
      () => setConectado(false),
    );
    clienteRef.current = cliente;
    cliente.activate();
    return () => {
      void cliente.deactivate();
      clienteRef.current = null;
    };
  }, [projetoId, resync]);

  useEffect(() => {
    const aoFocar = () => {
      if (document.visibilityState === 'visible') {
        void resync();
      }
    };
    document.addEventListener('visibilitychange', aoFocar);
    window.addEventListener('focus', aoFocar);
    return () => {
      document.removeEventListener('visibilitychange', aoFocar);
      window.removeEventListener('focus', aoFocar);
    };
  }, [resync]);

  return { snapshot, carregando, erro, conectado, resync };
}
