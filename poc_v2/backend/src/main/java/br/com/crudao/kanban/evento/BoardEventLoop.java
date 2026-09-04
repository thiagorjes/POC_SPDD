package br.com.crudao.kanban.evento;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Conexao JDBC dedicada em LISTEN no canal de eventos, com reconexao em backoff exponencial. O
 * estado da conexao alimenta o health indicator de readiness.
 */
@Slf4j
@Component
public class BoardEventLoop {

  private static final long BACKOFF_INICIAL_MS = 1_000L;
  private static final long BACKOFF_MAXIMO_MS = 30_000L;
  private static final long INTERVALO_POLL_MS = 500L;

  private final DataSource dataSource;
  private final BoardEventListener boardEventListener;
  private final ObjectMapper objectMapper;
  private final Counter reconexoes;
  private final AtomicBoolean ativo = new AtomicBoolean(false);
  private final AtomicBoolean conectado = new AtomicBoolean(false);
  private Thread thread;

  public BoardEventLoop(
      DataSource dataSource,
      BoardEventListener boardEventListener,
      ObjectMapper objectMapper,
      MeterRegistry meterRegistry) {
    this.dataSource = dataSource;
    this.boardEventListener = boardEventListener;
    this.objectMapper = objectMapper;
    this.reconexoes =
        Counter.builder("kanban.eventos.reconexoes")
            .description("Reconexoes da conexao LISTEN do canal de eventos de board")
            .register(meterRegistry);
  }

  /** Verdadeiro quando o LISTEN esta ativo; consumido pelo indicador de readiness. */
  public boolean conectado() {
    return conectado.get();
  }

  @EventListener(ApplicationReadyEvent.class)
  public void iniciar() {
    if (!ativo.compareAndSet(false, true)) {
      return;
    }
    thread = new Thread(this::executar, "board-event-loop");
    thread.setDaemon(true);
    thread.start();
  }

  @PreDestroy
  public void parar() {
    ativo.set(false);
    if (thread != null) {
      thread.interrupt();
    }
  }

  private void executar() {
    long backoff = BACKOFF_INICIAL_MS;
    while (ativo.get()) {
      try (Connection connection = dataSource.getConnection()) {
        try (Statement statement = connection.createStatement()) {
          statement.execute("LISTEN " + ListenNotifyEventoBoardPublisher.CANAL);
        }
        conectado.set(true);
        backoff = BACKOFF_INICIAL_MS;
        log.info("Escutando o canal {}", ListenNotifyEventoBoardPublisher.CANAL);
        consumir(connection);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      } catch (Exception e) {
        conectado.set(false);
        reconexoes.increment();
        if (backoff >= BACKOFF_MAXIMO_MS) {
          log.error("Conexao de eventos indisponivel; nova tentativa em {} ms", backoff, e);
        } else {
          log.warn("Falha na conexao de eventos; nova tentativa em {} ms", backoff);
        }
        backoff = dormir(backoff);
      }
    }
  }

  private void consumir(Connection connection) throws Exception {
    PGConnection pgConnection = connection.unwrap(PGConnection.class);
    while (ativo.get() && !connection.isClosed()) {
      PGNotification[] notificacoes = pgConnection.getNotifications((int) INTERVALO_POLL_MS);
      if (notificacoes == null) {
        continue;
      }
      for (PGNotification notificacao : notificacoes) {
        entregar(notificacao.getParameter());
      }
    }
  }

  private void entregar(String payload) {
    try {
      boardEventListener.onEvento(objectMapper.readValue(payload, EnvelopeEvento.class));
    } catch (Exception e) {
      log.error("Evento recebido em formato invalido foi descartado", e);
    }
  }

  private long dormir(long backoff) {
    try {
      Thread.sleep(backoff);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return Math.min(backoff * 2, BACKOFF_MAXIMO_MS);
  }
}
