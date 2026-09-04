package br.com.crudao.kanban.evento;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload enxuto propagado por LISTEN/NOTIFY (ADR-004): apenas identificadores, tipo e {@code seq}.
 * O cliente busca o detalhe via REST. Limite de 8 KB por payload.
 */
public record EventoBoard(
    UUID projetoId,
    long seq,
    TipoEventoBoard tipo,
    UUID tarefaId,
    UUID etapaId,
    UUID raiaId,
    Instant ocorridoEm) {

  public static EventoBoard deTarefa(
      UUID projetoId, TipoEventoBoard tipo, UUID tarefaId, UUID etapaId, UUID raiaId) {
    return new EventoBoard(projetoId, 0L, tipo, tarefaId, etapaId, raiaId, null);
  }

  public static EventoBoard deProjeto(UUID projetoId, TipoEventoBoard tipo) {
    return new EventoBoard(projetoId, 0L, tipo, null, null, null, null);
  }

  /** Copia do evento com o {@code seq} e o instante atribuidos no momento da publicacao. */
  public EventoBoard comSequencia(long novoSeq, Instant instante) {
    return new EventoBoard(projetoId, novoSeq, tipo, tarefaId, etapaId, raiaId, instante);
  }
}
