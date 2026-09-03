package br.com.crudao.kanban.common;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Unica fonte de tempo do dominio. Delega ao {@code now()} transacional do PostgreSQL — relogios de
 * pods distintos corromperiam as medias de lead-time de forma silenciosa (R-6).
 *
 * <p>Proibido {@code Instant.now()} da JVM em qualquer caminho que alimente lead-time.
 */
@Component
@RequiredArgsConstructor
public class ClockProvider {

  private final EntityManager entityManager;

  /** Instante transacional do banco. Estavel dentro da mesma transacao. */
  public Instant agora() {
    OffsetDateTime agora =
        (OffsetDateTime)
            entityManager.createNativeQuery("SELECT now()", OffsetDateTime.class).getSingleResult();
    return agora.toInstant();
  }
}
