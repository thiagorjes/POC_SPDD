import { Client, type IMessage } from '@stomp/stompjs';
import { token } from './auth';
import { urlWebSocket } from './config';
import type { EventoBoard } from './types';

export interface AssinaturaBoard {
  encerrar: () => void;
}

interface Ouvintes {
  aoEvento: (evento: EventoBoard) => void;
  aoConectar: () => void;
  aoDesconectar: () => void;
}

/**
 * Conecta ao broker STOMP do backend e assina o topico do board (e, opcionalmente, o de
 * notificacoes do usuario). Cada pod publica no proprio broker in-memory; a convergencia entre
 * pods vem do LISTEN/NOTIFY (ADR-004), transparente para o cliente.
 */
export function assinarBoard(
  projetoId: string,
  usuarioId: string | null,
  ouvintes: Ouvintes,
): AssinaturaBoard {
  const cliente = new Client({
    brokerURL: urlWebSocket(),
    reconnectDelay: 5000,
    beforeConnect: async () => {
      const jwt = await token();
      cliente.connectHeaders = jwt ? { Authorization: `Bearer ${jwt}` } : {};
    },
    onConnect: () => {
      cliente.subscribe(`/topic/board/${projetoId}`, (mensagem: IMessage) => {
        ouvintes.aoEvento(JSON.parse(mensagem.body) as EventoBoard);
      });
      if (usuarioId) {
        cliente.subscribe(`/topic/notificacoes/${usuarioId}`, (mensagem: IMessage) => {
          ouvintes.aoEvento(JSON.parse(mensagem.body) as EventoBoard);
        });
      }
      ouvintes.aoConectar();
    },
    onWebSocketClose: () => ouvintes.aoDesconectar(),
  });

  cliente.activate();
  return {
    encerrar: () => {
      void cliente.deactivate();
    },
  };
}
