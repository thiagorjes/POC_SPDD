package br.com.crudao.kanban.evento.adapter;

import br.com.crudao.kanban.evento.EventoBoard;
import br.com.crudao.kanban.evento.EventoBoardPublisher;
import br.com.crudao.kanban.evento.SequenciaProjetoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Adapter de broadcast multi-pod via PostgreSQL {@code LISTEN/NOTIFY} (ADR-002/ADR-004).
 *
 * <p>O incremento do {@code seq} acontece <b>dentro</b> da transacao de escrita; o {@code NOTIFY}
 * acontece exclusivamente em {@code afterCommit}, de modo que um rollback tardio nunca produz um
 * evento sobre uma mudanca que nao existiu.
 */
@Slf4j
@Component
public class ListenNotifyEventoBoardPublisher implements EventoBoardPublisher {

  /** Limite de payload do NOTIFY adotado pelo projeto (o limite do PostgreSQL e 8000 bytes). */
  private static final int LIMITE_PAYLOAD_BYTES = 8_000;

  private final SequenciaProjetoRepository sequenciaProjetoRepository;
  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  private final String canal;

  public ListenNotifyEventoBoardPublisher(
      SequenciaProjetoRepository sequenciaProjetoRepository,
      JdbcTemplate jdbcTemplate,
      ObjectMapper objectMapper,
      @Value("${kanban.eventos.canal}") String canal) {
    this.sequenciaProjetoRepository = sequenciaProjetoRepository;
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
    this.canal = canal;
  }

  @Override
  public void publicar(EventoBoard evento, Set<UUID> destinatarios) {
    Long seq = sequenciaProjetoRepository.incrementarERetornar(evento.projetoId());
    if (seq == null) {
      log.warn("Projeto {} sem sequencia de eventos; evento {} nao publicado",
          evento.projetoId(), evento.tipo());
      return;
    }

    EventoBoard completo =
        evento.comSeq(seq, Instant.now()).comDestinatarios(destinatarios == null ? Set.of() : destinatarios);

    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              notificar(completo);
            }
          });
    } else {
      notificar(completo);
    }
  }

  private void notificar(EventoBoard evento) {
    try {
      String payload = objectMapper.writeValueAsString(evento);
      int bytes = payload.getBytes(StandardCharsets.UTF_8).length;
      if (bytes > LIMITE_PAYLOAD_BYTES) {
        log.error(
            "Payload de evento excede o limite ({} bytes) para o projeto {} — evento descartado",
            bytes,
            evento.projetoId());
        return;
      }
      // pg_notify e uma funcao: retorna resultset. Com `update` o driver falha assim que passa a
      // usar prepared statement no servidor (a partir da 5a execucao), derrubando o tempo real.
      jdbcTemplate.query(
          "SELECT pg_notify(?, ?)",
          ps -> {
            ps.setString(1, canal);
            ps.setString(2, payload);
          },
          rs -> null);
      log.debug("Evento {} seq={} publicado no canal {}", evento.tipo(), evento.seq(), canal);
    } catch (JsonProcessingException e) {
      log.error("Falha ao serializar evento de board do projeto {}", evento.projetoId(), e);
    } catch (RuntimeException e) {
      log.error("Falha ao publicar NOTIFY para o projeto {}", evento.projetoId(), e);
    }
  }
}
