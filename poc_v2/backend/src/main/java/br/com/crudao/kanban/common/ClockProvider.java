package br.com.crudao.kanban.common;

import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Unica origem de carimbo temporal da aplicacao. Todo instante vem do {@code now()} transacional do
 * PostgreSQL: multiplos pods possuem relogios distintos e corromperiam as medias de lead-time de
 * forma silenciosa (R-6).
 */
@Component
@RequiredArgsConstructor
public class ClockProvider {

  private final EntityManager entityManager;

  /** Instante transacional do banco. Constante dentro da mesma transacao. */
  @Transactional(readOnly = true)
  public Instant agora() {
    Object resultado =
        entityManager.createNativeQuery("SELECT now()", Timestamp.class).getSingleResult();
    return ((Timestamp) resultado).toInstant();
  }
}
