package br.com.crudao.kanban.evento;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Payload enxuto de evento de board (ids + tipo + seq, &le; 8 KB). O cliente busca o detalhe via
 * REST — o evento nunca transporta o estado completo da tarefa.
 *
 * @param destinatarios usuarios que devem receber notificacao pessoal; vazio para broadcast puro
 */
public record EventoBoard(
    UUID projetoId,
    long seq,
    TipoEventoBoard tipo,
    UUID tarefaId,
    UUID etapaId,
    UUID raiaId,
    Instant ocorridoEm,
    Set<UUID> destinatarios) {

  public static EventoBoard de(UUID projetoId, TipoEventoBoard tipo, UUID tarefaId) {
    return new EventoBoard(projetoId, 0L, tipo, tarefaId, null, null, null, Set.of());
  }

  public static EventoBoard de(
      UUID projetoId, TipoEventoBoard tipo, UUID tarefaId, UUID etapaId, UUID raiaId) {
    return new EventoBoard(projetoId, 0L, tipo, tarefaId, etapaId, raiaId, null, Set.of());
  }

  public EventoBoard comSeq(long novoSeq, Instant instante) {
    return new EventoBoard(
        projetoId, novoSeq, tipo, tarefaId, etapaId, raiaId, instante, destinatarios);
  }

  public EventoBoard comDestinatarios(Set<UUID> novosDestinatarios) {
    return new EventoBoard(
        projetoId, seq, tipo, tarefaId, etapaId, raiaId, ocorridoEm, Set.copyOf(novosDestinatarios));
  }
}
