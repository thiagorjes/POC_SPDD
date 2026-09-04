package br.com.crudao.kanban.evento;

import java.util.Set;
import java.util.UUID;

/**
 * Porta de dominio para publicacao de eventos de board. A Service Layer depende apenas desta
 * interface, nunca do adapter de infraestrutura (verificado por teste de arquitetura).
 */
public interface EventoBoardPublisher {

  /**
   * Publica o evento apos o commit da transacao corrente. {@code destinatarios} identifica os
   * usuarios que devem receber notificacao pessoal; vazio significa broadcast apenas no board.
   */
  void publicar(EventoBoard evento, Set<UUID> destinatarios);
}
