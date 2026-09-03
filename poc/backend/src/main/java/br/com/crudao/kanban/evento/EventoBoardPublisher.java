package br.com.crudao.kanban.evento;

import java.util.Set;
import java.util.UUID;

/**
 * Porta de dominio para publicacao de eventos de board. Isola a Service Layer da infraestrutura de
 * eventos — nenhuma Service pode referenciar o adapter diretamente (verificado por ArchUnit).
 */
public interface EventoBoardPublisher {

  /**
   * Incrementa o {@code seq} do projeto dentro da transacao corrente e agenda a publicacao para o
   * {@code afterCommit} — nunca notifica mudanca que possa sofrer rollback tardio.
   */
  void publicar(EventoBoard evento, Set<UUID> destinatarios);

  default void publicar(EventoBoard evento) {
    publicar(evento, Set.of());
  }
}
