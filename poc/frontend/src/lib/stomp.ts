'use client';

import { Client, type IMessage } from '@stomp/stompjs';
import { obterToken } from './auth';

/** Fabrica do cliente STOMP. O broker e simples e por pod: nao ha relay (ADR-002/R-4). */
export function criarCliente(
  aoConectar: (cliente: Client) => void,
  aoDesconectar: () => void,
): Client {
  const cliente = new Client({
    brokerURL: (process.env.NEXT_PUBLIC_WS_URL ?? '').replace(/^http/, 'ws'),
    reconnectDelay: 2000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    beforeConnect: async () => {
      const token = await obterToken();
      cliente.connectHeaders = token ? { Authorization: `Bearer ${token}` } : {};
    },
    onConnect: () => aoConectar(cliente),
    onWebSocketClose: aoDesconectar,
  });
  return cliente;
}

export function assinar<T>(
  cliente: Client,
  topico: string,
  aoReceber: (payload: T) => void,
): () => void {
  const inscricao = cliente.subscribe(topico, (mensagem: IMessage) => {
    aoReceber(JSON.parse(mensagem.body) as T);
  });
  return () => inscricao.unsubscribe();
}
