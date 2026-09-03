package br.com.crudao.kanban.evento.adapter;

import br.com.crudao.kanban.evento.EventoBoard;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Conexao JDBC dedicada em {@code LISTEN board_events}, em thread propria. Reconecta com backoff
 * exponencial e alimenta o health indicator de readiness.
 */
@Slf4j
@Component
public class BoardEventLoop {

  private static final long INTERVALO_POLL_MS = 500L;

  private final DataSource dataSource;
  private final StompBoardEventListener listener;
  private final ObjectMapper objectMapper;
  private final String canal;
  private final long backoffInicialMs;
  private final long backoffMaximoMs;

  private final Counter reconexoes;
  private final Timer latencia;

  private final AtomicBoolean executando = new AtomicBoolean(false);
  private volatile boolean conectado;
  private Thread thread;

  public BoardEventLoop(
      DataSource dataSource,
      StompBoardEventListener listener,
      ObjectMapper objectMapper,
      MeterRegistry meterRegistry,
      @Value("${kanban.eventos.canal}") String canal,
      @Value("${kanban.eventos.backoff-inicial-ms}") long backoffInicialMs,
      @Value("${kanban.eventos.backoff-maximo-ms}") long backoffMaximoMs) {
    this.dataSource = dataSource;
    this.listener = listener;
    this.objectMapper = objectMapper;
    this.canal = canal;
    this.backoffInicialMs = backoffInicialMs;
    this.backoffMaximoMs = backoffMaximoMs;
    this.reconexoes = Counter.builder("kanban.eventos.reconexoes").register(meterRegistry);
    this.latencia =
        Timer.builder("kanban.eventos.latencia")
            .description("NOTIFY do PostgreSQL ate o broadcast STOMP")
            .register(meterRegistry);
  }

  public boolean isConectado() {
    return conectado;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void iniciar() {
    if (!executando.compareAndSet(false, true)) {
      return;
    }
    thread = new Thread(this::loop, "board-event-loop");
    thread.setDaemon(true);
    thread.start();
    log.info("Listener de eventos iniciado no canal {}", canal);
  }

  @PreDestroy
  public void parar() {
    executando.set(false);
    if (thread != null) {
      thread.interrupt();
    }
  }

  private void loop() {
    long backoff = backoffInicialMs;
    while (executando.get()) {
      try (Connection connection = dataSource.getConnection()) {
        try (Statement statement = connection.createStatement()) {
          statement.execute("LISTEN " + canal);
        }
        conectado = true;
        backoff = backoffInicialMs;
        log.info("Escutando o canal {}", canal);
        consumir(connection);
      } catch (SQLException e) {
        conectado = false;
        reconexoes.increment();
        if (!executando.get()) {
          return;
        }
        log.warn("Conexao do listener perdida; reconectando em {} ms", backoff);
        if (backoff >= backoffMaximoMs) {
          log.error("Listener de eventos esgotou o backoff maximo — board em tempo real degradado");
        }
        dormir(backoff);
        backoff = Math.min(backoff * 2, backoffMaximoMs);
      }
    }
    conectado = false;
  }

  private void consumir(Connection connection) throws SQLException {
    PGConnection pgConnection = connection.unwrap(PGConnection.class);
    while (executando.get()) {
      PGNotification[] notificacoes = pgConnection.getNotifications((int) INTERVALO_POLL_MS);
      if (notificacoes == null) {
        continue;
      }
      for (PGNotification notificacao : notificacoes) {
        processar(notificacao.getParameter());
      }
    }
  }

  private void processar(String payload) {
    Instant inicio = Instant.now();
    try {
      EventoBoard evento = objectMapper.readValue(payload, EventoBoard.class);
      listener.onEvento(evento);
      Instant origem = evento.ocorridoEm() != null ? evento.ocorridoEm() : inicio;
      latencia.record(Duration.between(origem, Instant.now()));
    } catch (Exception e) {
      log.error("Falha ao processar evento recebido do canal {}", canal, e);
    }
  }

  private void dormir(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      executando.set(false);
    }
  }
}
