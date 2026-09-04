package br.com.crudao.kanban.evento;

import br.com.crudao.kanban.common.ClockProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Adapter LISTEN/NOTIFY da porta {@link EventoBoardPublisher} (ADR-004).
 *
 * <p>O {@code seq} e incrementado dentro da transacao de escrita; a publicacao ocorre apenas em
 * {@code afterCommit}, para nunca notificar mudanca que sofra rollback tardio.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ListenNotifyEventoBoardPublisher implements EventoBoardPublisher {

  /** Limite do payload conforme Safeguards, secao 2. */
  private static final int TAMANHO_MAXIMO_PAYLOAD = 8 * 1024;

  static final String CANAL = "board_events";

  private final SequenciaProjetoRepository sequenciaProjetoRepository;
  private final DataSource dataSource;
  private final ObjectMapper objectMapper;
  private final ClockProvider clockProvider;

  @Override
  public void publicar(EventoBoard evento, Set<UUID> destinatarios) {
    long seq = proximaSequencia(evento.projetoId());
    EnvelopeEvento envelope =
        new EnvelopeEvento(
            evento.comSequencia(seq, clockProvider.agora()),
            destinatarios == null ? Set.of() : Set.copyOf(destinatarios));

    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              notificar(envelope);
            }
          });
    } else {
      notificar(envelope);
    }
  }

  /** UPDATE serializado dentro da transacao corrente; garante monotonicidade por projeto. */
  private long proximaSequencia(UUID projetoId) {
    SequenciaProjeto sequencia =
        sequenciaProjetoRepository
            .buscarParaAtualizar(projetoId)
            .orElseGet(() -> new SequenciaProjeto(projetoId));
    sequencia.setUltimoSeq(sequencia.getUltimoSeq() + 1);
    return sequenciaProjetoRepository.save(sequencia).getUltimoSeq();
  }

  private void notificar(EnvelopeEvento envelope) {
    String payload;
    try {
      payload = objectMapper.writeValueAsString(envelope);
    } catch (JsonProcessingException e) {
      log.error(
          "Falha ao serializar evento de board do projeto {}", envelope.evento().projetoId(), e);
      return;
    }
    if (payload.length() > TAMANHO_MAXIMO_PAYLOAD) {
      log.error(
          "Payload de evento excede o limite de 8 KB e nao sera publicado (projeto {}, tipo {})",
          envelope.evento().projetoId(),
          envelope.evento().tipo());
      return;
    }
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("SELECT pg_notify(?, ?)")) {
      statement.setString(1, CANAL);
      statement.setString(2, payload);
      statement.execute();
    } catch (Exception e) {
      // A perda do evento e recuperavel: o cliente detecta o gap de seq e faz resync (ADR-004).
      log.error(
          "Falha ao publicar evento no canal {} (projeto {}); o cliente ira ressincronizar",
          CANAL,
          envelope.evento().projetoId(),
          e);
    }
  }
}
