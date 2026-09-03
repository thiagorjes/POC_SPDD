package br.com.crudao.kanban.evento.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/** Readiness reflete o estado da conexao do listener de eventos. */
// O nome do bean vira o id do health indicator em /actuator/health; nao pode colidir com o
// proprio BoardEventLoop.
@Component("boardEventLoopCanal")
@RequiredArgsConstructor
public class BoardEventLoopHealthIndicator implements HealthIndicator {

  private final BoardEventLoop boardEventLoop;

  @Override
  public Health health() {
    return boardEventLoop.isConectado()
        ? Health.up().withDetail("canal", "conectado").build()
        : Health.down().withDetail("canal", "desconectado").build();
  }
}
