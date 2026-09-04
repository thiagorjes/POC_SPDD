package br.com.crudao.kanban.evento;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;

/**
 * Entrega o evento recebido do canal para as sessoes STOMP hospedadas por <em>este</em> pod. Sem
 * broker relay: ADR-002 proibe qualquer infraestrutura alem de PostgreSQL e Keycloak.
 */
@Slf4j
@Component
public class StompBoardEventListener implements BoardEventListener {

  private static final String TOPICO_BOARD = "/topic/board/";
  private static final String TOPICO_NOTIFICACOES = "/topic/notificacoes/";

  private final SimpMessagingTemplate simpMessagingTemplate;
  private final SimpUserRegistry simpUserRegistry;
  private final Timer latencia;

  public StompBoardEventListener(
      SimpMessagingTemplate simpMessagingTemplate,
      SimpUserRegistry simpUserRegistry,
      MeterRegistry meterRegistry) {
    this.simpMessagingTemplate = simpMessagingTemplate;
    this.simpUserRegistry = simpUserRegistry;
    this.latencia =
        Timer.builder("kanban.eventos.latencia")
            .description("Latencia entre a emissao do NOTIFY e o broadcast STOMP (RNF-001)")
            .register(meterRegistry);
  }

  @Override
  public void onEvento(EnvelopeEvento envelope) {
    EventoBoard evento = envelope.evento();
    simpMessagingTemplate.convertAndSend(TOPICO_BOARD + evento.projetoId(), evento);

    for (UUID destinatario : envelope.destinatarios()) {
      // Cada pod entrega apenas as sessoes locais; os demais ignoram quem nao hospedam.
      if (simpUserRegistry.getUser(destinatario.toString()) != null) {
        simpMessagingTemplate.convertAndSend(TOPICO_NOTIFICACOES + destinatario, evento);
      }
    }
    registrarLatencia(evento.ocorridoEm());
    log.debug("Evento {} seq={} entregue no pod", evento.tipo(), evento.seq());
  }

  private void registrarLatencia(Instant ocorridoEm) {
    if (ocorridoEm == null) {
      return;
    }
    latencia.record(Duration.between(ocorridoEm, Instant.now()));
  }
}
